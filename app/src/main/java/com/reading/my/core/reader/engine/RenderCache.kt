package com.reading.my.core.reader.engine

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.reading.my.core.reader.domain.Page
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme

/**
 * L1 内存渲染缓存（Phase 3）
 *
 * 将渲染后的页面以 Bitmap 形式缓存在内存中，避免每次翻页/重组时重复执行
 * Paint.breakText 测量 + Canvas.drawText 绘制。
 *
 * 策略：
 * - 使用 Android LruCache 自动管理内存
 * - 默认缓存容量：约 6 页（当前页 ± 前后各 2 页），单页约 1080×2124 × 4B ≈ 9MB
 * - 当排版参数（字号/行高/主题色）变化时自动失效（通过 configHash 判定）
 * - Key 格式："bookId_chapterIndex_pageIndex_configHash"（含 bookId，天然跨书隔离）
 *
 * 典型用法：
 * ```kotlin
 * val bitmap = renderCache.getOrRender(bookId, chapterIndex, pageIndex, page, config, theme) {
 *     // 渲染回调：在离屏 Canvas 上绘制
 *     renderToBitmap(page, config, theme)
 * }
 * ```
 */
object RenderCache {

    /** 缓存 key：唯一标识一页的渲染结果（含 bookId 隔离） */
    private data class CacheKey(
        val bookId: String,
        val chapterIndex: Int,
        val pageIndex: Int,
        val configHash: Int,
    ) {
        fun stringify(): String = "${bookId}_${chapterIndex}_${pageIndex}_$configHash"
    }

    /**
     * LRU 缓存实例
     *
     * 容量计算：每页 Bitmap 约 width × height × 4 bytes (ARGB_8888)
     * 以 1080×2124 为例，单页 ≈ 9.2MB
     * 设定缓存 6 页 ≈ 55MB，占堆内存合理比例
     */
    private val lruCache = object : LruCache<String, Bitmap>(
        (6 * 1080 * 2124 * 4).toLong().toInt()  // ~55MB
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            // 返回 Bitmap 的字节大小（width × height × 4 for ARGB_8888）
            return value.byteCount
        }
    }

    /**
     * 获取或渲染一页的 Bitmap
     *
     * @param bookId       书籍标识（编码到缓存 key 中，天然隔离不同书籍）
     * @param chapterIndex 章节索引
     * @param pageIndex    页码
     * @param page         Page 数据对象
     * @param config       排版参数
     * @param theme        阅读主题
     * @param renderer     缓存未命中时的渲染函数：(DrawScope-like) → Bitmap
     * @return 渲染好的 Bitmap（可能来自缓存）
     */
    fun getOrRender(
        bookId: String,
        chapterIndex: Int,
        pageIndex: Int,
        page: Page,
        config: PageLayoutConfig,
        theme: ReaderTheme,
        renderer: () -> Bitmap,
    ): Bitmap {
        val key = CacheKey(bookId, chapterIndex, pageIndex, computeConfigHash(config, theme)).stringify()

        val cached = lruCache.get(key)
        if (cached != null) {
            Log.d("RenderCache", "💾 L1命中: key='$key', bookId='$bookId', ch=$chapterIndex, p=$pageIndex")
            return cached
        }

        // 缓存未命中 → 渲染并写入
        Log.d("RenderCache", "🎨 L1未命中: key='$key', bookId='$bookId', ch=$chapterIndex, p=$pageIndex, text='${page.text.take(30)}...'")
        val bitmap = renderer()
        lruCache.put(key, bitmap)
        return bitmap
    }

    /**
     * 预渲染并缓存指定页（供后台预加载使用）
     */
    fun prefetch(
        bookId: String,
        chapterIndex: Int,
        pageIndex: Int,
        page: Page,
        config: PageLayoutConfig,
        theme: ReaderTheme,
        renderer: () -> Bitmap,
    ) {
        val key = CacheKey(bookId, chapterIndex, pageIndex, computeConfigHash(config, theme)).stringify()
        if (lruCache.get(key) == null) {
            val bitmap = renderer()
            lruCache.put(key, bitmap)
        }
    }

    /** 检查某页是否已缓存 */
    fun isCached(bookId: String, chapterIndex: Int, pageIndex: Int, config: PageLayoutConfig, theme: ReaderTheme): Boolean {
        val key = CacheKey(bookId, chapterIndex, pageIndex, computeConfigHash(config, theme)).stringify()
        return lruCache.get(key) != null
    }

    /** 清空全部缓存（排版参数大变时调用） */
    fun clear() {
        lruCache.evictAll()
    }

    /** 仅移除指定书籍的指定章节缓存（章节内容变化时） */
    fun evictChapter(bookId: String, chapterIndex: Int) {
        // LruCache 不支持前缀删除，遍历快照逐个移除
        val prefix = "${bookId}_${chapterIndex}_"
        val snapshot = lruCache.snapshot()
        for ((key) in snapshot) {
            if (key.startsWith(prefix)) {
                lruCache.remove(key)
            }
        }
    }

    /** 移除指定书籍的全部缓存（删除书籍时调用） */
    fun evictBook(bookId: String) {
        val prefix = "${bookId}_"
        val snapshot = lruCache.snapshot()
        for ((key) in snapshot) {
            if (key.startsWith(prefix)) {
                lruCache.remove(key)
            }
        }
    }

    /** 获取缓存统计信息（用于调试） */
    fun stats(): CacheStats = CacheStats(
        size = lruCache.size(),
        maxSize = lruCache.maxSize(),
        hitCount = lruCache.hitCount(),
        missCount = lruCache.missCount(),
        evictionCount = lruCache.evictionCount(),
        putCount = lruCache.putCount(),
    )

    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val hitCount: Int,
        val missCount: Int,
        val evictionCount: Int,
        val putCount: Int,
    ) {
        val hitRate: Float get() = if (hitCount + missCount > 0) hitCount.toFloat() / (hitCount + missCount) else 0f
        val sizeMB: Float get() = size / (1024f * 1024f)
        override fun toString(): String =
            "RenderCache: ${sizeMB.toInt()}MB/${
                (maxSize / (1024f * 1024f)).toInt()
            }MB, hit=$hitRate%, hits=$hitCount misses=$missCount evicted=$evictionCount"
    }

    /**
     * 计算配置哈希值 —— 用于检测排版参数变化使缓存失效
     *
     * 包含：屏幕尺寸、字号、行高倍率、边距、缩进、间距参数、文字颜色、背景色
     * 不包含：章节文本内容（由 chapterIndex+pageIndex 隐含）
     *
     * 该函数为 internal 可见性，供 L2DatabaseCache / ReaderScreen 复用以确保
     * L1(内存Bitmap) 与 L2(数据库分页结果) 在排版参数变化时同步失效。
     */
    internal fun computeConfigHash(config: PageLayoutConfig, theme: ReaderTheme): Int {
        var hash = 17
        hash = 31 * hash + config.screenWidthPx
        hash = 31 * hash + config.screenHeightPx
        hash = 31 * hash + config.fontSizeSp.toBits()
        hash = 31 * hash + config.lineHeightMultiplier.toBits()
        hash = 31 * hash + config.horizontalPaddingDp.toBits()
        hash = 31 * hash + config.verticalPaddingDp.toBits()
        hash = 31 * hash + config.firstLineIndentChars
        hash = 31 * hash + config.blankLineSpacingRatio.toBits()
        hash = 31 * hash + config.paraEndSpacingRatio.toBits()
        hash = 31 * hash + TextRender.colorToArgb(theme.textColor)
        hash = 31 * hash + TextRender.colorToArgb(theme.backgroundColor)
        return hash
    }

    /** 将 Float 的位模式转为 Int 用于 hash（避免 hashCode 对 float 的装箱） */
    private fun Float.toBits(): Int = java.lang.Float.floatToIntBits(this)
}
