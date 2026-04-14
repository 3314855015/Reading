package com.reading.my.core.reader.engine

import android.util.Log
import com.reading.my.core.reader.domain.ChapterPages
import com.reading.my.core.reader.domain.Page
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.data.local.database.dao.RenderCacheDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.net.URLDecoder

/**
 * L2 数据库缓存（Phase 5 核心组件）
 *
 * 将分页引擎（PageLayoutManager）的输出结果 [ChapterPages] 持久化到 Room 数据库，
 * 实现跨进程/跨生命周期的分页结果复用。
 *
 * ## 三级缓存架构中的位置
 *
 * ```
 * 请求页 → L1 RenderCache (内存 Bitmap) → 未命中
 *        → L2 DatabaseCache (本库，分页结果) → 未命中
 *        → 执行分页 PageLayoutManager.paginateChapter()
 *        → 结果回填 L2 + 渲染后回填 L1
 * ```
 *
 * ## 缓存内容 vs L1 的区别
 * - **L1 (RenderCache)**：缓存渲染后的 Bitmap 像素数据（~9MB/页），进程内有效
 * - **L2 (DatabaseCache)**：缓存的分页结构数据 List<Page>（~几KB/页），持久化有效
 *
 * ## Key 设计
 * - 格式：`"{bookId}_{chapterIndex}_{configHash}"`
 * - configHash 与 L1 的 RenderCache.computeConfigHash() 使用相同算法，
 *   确保排版参数变化时 L1/L2 同步失效
 *
 * ## 序列化格式
 * 使用紧凑的自定义文本格式（非 JSON），每页一行：
 * ```
 * pageIndex|startCharIndex|endCharIndex|isContinuation|text
 * ```
 * 页间用 `\n` 分隔，text 中的 `|` 和 `\n` 做 URL 编码转义。
 */
class L2DatabaseCache(
    private val dao: RenderCacheDao,
) {

    companion object {
        private const val TAG = "L2DatabaseCache"
        /** 字段分隔符 */
        private const val FIELD_SEP = "|"
        /** 记录分隔符 */
        private const val RECORD_SEP = "\n"
    }

    // ==================== 公开 API ====================

    /**
     * 获取章节的分页结果（带自动回源计算）
     *
     * 查询流程：
     * 1. 用 cacheKey 查 Room DB
     * 2. 命中且 configHash 匹配 → 反序列化返回
     * 3. 未命中或不匹配 → 调用 [compute] 函数执行分页，写入 DB 后返回
     *
     * @param bookId       书籍标识
     * @param chapterIndex 章节索引
     * @param configHash   排版配置哈希值（与 RenderCache.computeConfigHash 一致）
     * @param compute      回源函数：执行实际分页计算并返回 ChapterPages
     * @return 章节的分页结果
     */
    suspend fun getChapterPages(
        bookId: String,
        chapterIndex: Int,
        configHash: Int,
        compute: suspend () -> ChapterPages,
    ): ChapterPages = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(bookId, chapterIndex, configHash)

        // 1) 尝试从数据库读取
        val cached = dao.getByKey(cacheKey)
        if (cached != null && cached.configHash == configHash) {
            Log.d(TAG, "L2 命中: $cacheKey (${cached.pageCount} 页)")
            return@withContext deserialize(cached.pageDataJson, chapterIndex)
        }

        // 2) 缓存未命中 → 执行分页计算
        Log.d(TAG, "L2 未命中: $cacheKey → 执行分页")
        val result = compute()

        // 3) 写入数据库
        putChapterPages(bookId, chapterIndex, configHash, result)

        result
    }

    /**
     * 写入章节的分页结果到数据库
     */
    suspend fun putChapterPages(
        bookId: String,
        chapterIndex: Int,
        configHash: Int,
        pages: ChapterPages,
    ) = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(bookId, chapterIndex, configHash)
        val json = serialize(pages.pages)

        val entity = com.reading.my.data.local.database.entity.RenderCacheEntity(
            cacheKey = cacheKey,
            bookId = bookId,
            chapterIndex = chapterIndex,
            configHash = configHash,
            pageCount = pages.pageCount,
            pageDataJson = json,
            createdAt = System.currentTimeMillis(),
        )
        dao.insertOrReplace(entity)
        Log.d(TAG, "L2 写入: $cacheKey (${pages.pageCount} 页, ${json.length} bytes)")
    }

    /** 切换书籍时清理该书的全部缓存 */
    suspend fun evictBook(bookId: String) = withContext(Dispatchers.IO) {
        val count = dao.deleteByBookId(bookId)
        Log.d(TAG, "L2 清理书籍: $bookId ($count 条)")
    }

    /** 章节内容更新时清理该章缓存 */
    suspend fun evictChapter(
        bookId: String,
        chapterIndex: Int,
    ) = withContext(Dispatchers.IO) {
        val count = dao.deleteByBookAndChapter(bookId, chapterIndex)
        Log.d(TAG, "L2 清理章节: $bookId/$chapterIndex ($count 条)")
    }

    /** 排版参数大变时全量清空 */
    suspend fun clear() = withContext(Dispatchers.IO) {
        dao.deleteAll()
        Log.d(TAG, "L2 全量清空")
    }

    // ==================== 内部方法 ====================

    /** 构建缓存 key */
    internal fun buildCacheKey(
        bookId: String,
        chapterIndex: Int,
        configHash: Int,
    ): String = "${URLEncoder.encode(bookId, "UTF-8")}_${chapterIndex}_$configHash"

    // ---- 序列化：List<Page> → 紧凑字符串 ----

    /**
     * 将 Page 列表序列化为紧凑文本格式
     *
     * 每条记录：`pageIndex|startCharIndex|endCharIndex|isContinuation|encodedText`
     * 记录间以换行符分隔
     */
    internal fun serialize(pages: List<Page>): String {
        return pages.joinToString(RECORD_SEP) { page ->
            buildString {
                append(page.pageIndex)
                append(FIELD_SEP)
                append(page.startCharIndex)
                append(FIELD_SEP)
                append(page.endCharIndex)
                append(FIELD_SEP)
                append(if (page.isContinuation) 1 else 0)
                append(FIELD_SEP)
                append(encodeText(page.text))
            }
        }
    }

    /**
     * 从紧凑文本反序列化为 ChapterPages
     */
    internal fun deserialize(data: String, chapterIndex: Int): ChapterPages {
        if (data.isBlank()) {
            return ChapterPages(chapterIndex, emptyList())
        }

        val pages = data.split(RECORD_SEP).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split(FIELD_SEP, limit = 5)
            if (parts.size < 5) return@mapNotNull null
            try {
                Page(
                    chapterIndex = chapterIndex,
                    pageIndex = parts[0].toInt(),
                    startCharIndex = parts[1].toInt(),
                    endCharIndex = parts[2].toInt(),
                    isContinuation = parts[3].toInt() == 1,
                    text = decodeText(parts[4]),
                )
            } catch (e: NumberFormatException) {
                Log.w(TAG, "反序列化跳过异常行: $line", e)
                null
            }
        }
        return ChapterPages(chapterIndex = chapterIndex, pages = pages)
    }

    /** 对 text 中的特殊字符做 URL 编码，防止破坏分隔符 */
    private fun encodeText(text: String): String =
        URLEncoder.encode(text, "UTF-8")

    private fun decodeText(encoded: String): String =
        URLDecoder.decode(encoded, "UTF-8")
}
