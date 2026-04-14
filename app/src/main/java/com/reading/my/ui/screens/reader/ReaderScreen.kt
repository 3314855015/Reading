package com.reading.my.ui.screens.reader

import android.util.DisplayMetrics
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.reading.my.core.reader.engine.L2DatabaseCache
import com.reading.my.core.reader.engine.PageLayoutManager
import com.reading.my.core.reader.engine.RenderCache
import com.reading.my.core.reader.domain.ChapterPages
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme
import com.reading.my.core.reader.ui.FlipPagerReader
import com.reading.my.domain.model.Chapter

/**
 * 阅读器主界面（Phase 5: 集成 L2 数据库缓存）
 *
 * 完整的阅读流程（三级缓存）：
 * 1. 打开章节 → 查询 L2 DatabaseCache（分页结果持久化）
 * 2. L2 未命中 → 执行 PageLayoutManager 分页 → 写入 L2
 * 3. 翻页渲染 → 查询 L1 RenderCache（内存 Bitmap）
 * 4. L1 未命中 → TextRender 渲染到 Bitmap → 写入 L1
 *
 * @param chapter       当前要阅读的章节（包含完整正文 content）
 * @param bookTitle     书名（显示在顶部栏）
 * @param totalChapters 总章数（用于进度计算）
 * @param l2Cache       L2 二级缓存实例（可选，传入 null 则跳过数据库缓存）
 * @param onBack        返回上一级（详情页/书架）
 * @param onPageChange  翻页回调：(chapterIndex, pageIndex) → Unit
 */
@Composable
fun ReaderScreen(
    chapter: Chapter,
    bookTitle: String,
    totalChapters: Int = 1,
    bookId: String = "",
    l2Cache: L2DatabaseCache? = null,
    onBack: () -> Unit = {},
    onPageChange: ((chapterIndex: Int, pageIndex: Int) -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density

    // ---- 构建排版配置 ----
    val systemBarsHeightDp = 48f
    val config = remember {
        PageLayoutConfig.default(
            screenWidthPx = (configuration.screenWidthDp * density).toInt(),
            screenHeightPx = ((configuration.screenHeightDp - systemBarsHeightDp) * density).toInt(),
            density = density,
        )
    }

    // ---- 选择主题（默认日间） ----
    val theme = remember { ReaderTheme.DayLight }

    // ---- 计算配置哈希（L1/L2 共用）----
    val configHash = remember(config, theme) {
        RenderCache.computeConfigHash(config, theme)
    }

    // ---- 分页结果状态 ----
    var chapterPages by remember(chapter.chapterIndex) {
        mutableStateOf<ChapterPages?>(null)
    }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(chapter, configHash) {
        isLoading = true
        try {
            val result = if (l2Cache != null && bookId.isNotEmpty()) {
                // Phase 5: 优先从 L2 数据库缓存读取分页结果
                l2Cache.getChapterPages(
                    bookId = bookId,
                    chapterIndex = chapter.chapterIndex,
                    configHash = configHash,
                    compute = {
                        // 回源：执行实际分页计算
                        val layoutManager = PageLayoutManager(config)
                        layoutManager.paginateChapter(chapter.chapterIndex, chapter.content)
                    },
                )
            } else {
                // 无 L2 缓存：直接分页（降级兼容）
                val layoutManager = PageLayoutManager(config)
                layoutManager.paginateChapter(chapter.chapterIndex, chapter.content)
            }
            Log.d("ReaderScreen", "分页完成: ${result.pageCount}页 (source=${if (l2Cache != null && bookId.isNotEmpty()) "L2/direct" else "direct"})")
            chapterPages = result
        } catch (e: Exception) {
            Log.e("ReaderScreen", "分页失败", e)
            chapterPages = null
        } finally {
            isLoading = false
        }
    }

    // ---- UI 渲染 ----
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = theme.backgroundColor)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.matchParentSize())
            }
            chapterPages != null -> {
                ReaderContent(
                    chapterPages = chapterPages!!,
                    config = config,
                    theme = theme,
                    chapter = chapter,
                    bookTitle = bookTitle,
                    totalChapters = totalChapters,
                    bookId = bookId,
                    onBack = onBack,
                    onPageChange = onPageChange,
                )
            }
        }
    }
}

/**
 * 阅读器内容区（含顶部栏 + 翻页区域 + 触控处理）
 */
@Composable
private fun ReaderContent(
    chapterPages: ChapterPages,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    chapter: Chapter,
    bookTitle: String,
    totalChapters: Int,
    bookId: String,
    onBack: () -> Unit,
    onPageChange: ((chapterIndex: Int, pageIndex: Int) -> Unit)?,
) {
    var showBars by remember { mutableStateOf(false) }  // 是否显示顶/底栏

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // 轻触中心区域 → 切换菜单栏显隐
                        showBars = !showBars
                    },
                )
            },
    ) {
        // ===== 翻页阅读区 =====
        FlipPagerReader(
            chapterPages = chapterPages,
            config = config,
            theme = theme,
            bookId = bookId,
            initialPage = 0,
            onPageChange = { pageIndex ->
                onPageChange?.invoke(chapter.chapterIndex, pageIndex)
            },
        )

        // ===== 顶部信息栏（可隐藏） =====
        if (showBars) {
            ReaderTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                bookTitle = bookTitle,
                chapterTitle = chapter.title,
                currentChapter = chapter.chapterIndex + 1,
                totalChapters = totalChapters,
                currentPageCount = chapterPages.pageCount,
                onBack = onBack,
                onClose = { showBars = false },
            )
        }

        // ===== 底部信息栏（可隐藏） =====
        if (showBars) {
            ReaderBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                theme = theme,
                onClose = { showBars = false },
            )
        }
    }
}
