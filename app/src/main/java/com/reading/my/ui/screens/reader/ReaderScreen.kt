package com.reading.my.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * 阅读器主界面（Phase 6: 章节间跳转 + L2 缓存）
 *
 * 完整的阅读流程（三级缓存 + 跨章导航）：
 * 1. 打开章节 → 查询 L2 DatabaseCache（分页结果持久化）
 * 2. L2 未命中 → 执行 PageLayoutManager 分页 → 写入 L2
 * 3. 翻页渲染 → 查询 L1 RenderCache（内存 Bitmap）
 * 4. L1 未命中 → TextRender 渲染到 Bitmap → 写入 L1
 * 5. **章节边界**：首页往前滑 → 加载上一章末页；末页往后滑 → 加载下一章首页
 *
 * @param chapters        全部章节列表（用于跨章跳转，至少包含当前章）
 * @param currentChapterIndex 当前章节索引（用于定位）
 * @param bookTitle       书名（显示在顶部栏）
 * @param totalChapters   总章数（用于进度计算）
 * @param l2Cache         L2 二级缓存实例（可选，传入 null 则跳过数据库缓存）
 * @param onChapterChange 章节切换回调：(newChapterIndex) → Unit，外部据此更新 UI
 * @param onBack          返回上一级（详情页/书架）
 * @param onPageChange    翻页回调：(chapterIndex, pageIndex) → Unit
 */
@Composable
fun ReaderScreen(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    bookTitle: String,
    totalChapters: Int = 1,
    bookId: String = "",
    l2Cache: L2DatabaseCache? = null,
    onChapterChange: ((newChapterIndex: Int) -> Unit)? = null,
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

    // ---- 当前活跃章节状态 ----
    var activeChapterIndex by remember { mutableIntStateOf(currentChapterIndex) }
    var chapterPages by remember { mutableStateOf<ChapterPages?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    // 跨章跳转方向：true=显示末页(从下章返回), false=显示首页(正常/从上章进入)
    var jumpToLastPage by remember { mutableStateOf(false) }

    // 获取当前章节对象
    val currentChapter = remember(chapters, activeChapterIndex) {
        chapters.getOrNull(activeChapterIndex)
    }

    // ---- 分页逻辑（当章节或 configHash 变化时重新执行）----
    LaunchedEffect(currentChapter, configHash) {
        if (currentChapter == null) {
            return@LaunchedEffect
        }

        isLoading = true
        try {
            val result = if (l2Cache != null && bookId.isNotEmpty()) {
                l2Cache.getChapterPages(
                    bookId = bookId,
                    chapterIndex = currentChapter.chapterIndex,
                    configHash = configHash,
                    compute = {
                        val layoutManager = PageLayoutManager(config)
                        layoutManager.paginateChapter(currentChapter.chapterIndex, currentChapter.content)
                    },
                )
            } else {
                val layoutManager = PageLayoutManager(config)
                layoutManager.paginateChapter(currentChapter.chapterIndex, currentChapter.content)
            }
            chapterPages = result
        } catch (e: Exception) {
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
            chapterPages != null && currentChapter != null -> {
                ReaderContent(
                    chapter = currentChapter!!,
                    chapterPages = chapterPages!!,
                    chapters = chapters,
                    activeChapterIndex = activeChapterIndex,
                    initialPage = if (jumpToLastPage) (chapterPages!!.pageCount - 1).coerceAtLeast(0) else 0,
                    onActiveChapterChanged = { newIndex ->
                        activeChapterIndex = newIndex
                        onChapterChange?.invoke(newIndex)
                    },
                    onJumpDirectionSet = { toLast -> jumpToLastPage = toLast },
                    config = config,
                    theme = theme,
                    configHash = configHash,
                    bookTitle = bookTitle,
                    totalChapters = totalChapters,
                    bookId = bookId,
                    l2Cache = l2Cache,
                    onBack = onBack,
                    onPageChange = onPageChange,
                )
            }
        }
    }
}

/**
 * 阅读器内容区（含顶部栏 + 翻页区域 + 触控处理 + 跨章导航）
 */
@Composable
private fun ReaderContent(
    chapter: Chapter,
    chapterPages: ChapterPages,
    chapters: List<Chapter>,
    activeChapterIndex: Int,
    initialPage: Int = 0,
    onActiveChapterChanged: (newIndex: Int) -> Unit,
    onJumpDirectionSet: ((toLast: Boolean) -> Unit)? = null,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    configHash: Int,
    bookTitle: String,
    totalChapters: Int,
    bookId: String,
    l2Cache: L2DatabaseCache?,
    onBack: () -> Unit,
    onPageChange: ((chapterIndex: Int, pageIndex: Int) -> Unit)?,
) {
    var showBars by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        showBars = !showBars
                    },
                )
            },
    ) {
        // ===== 翻页阅读区（带边界检测） =====
        FlipPagerReader(
            chapterPages = chapterPages,
            config = config,
            theme = theme,
            bookId = bookId,
            initialPage = initialPage,
            onPageChange = { pageIndex ->
                onPageChange?.invoke(chapter.chapterIndex, pageIndex)
            },
            onReachStart = {
                // 在首页继续左滑 → 跳到上章**末页**
                val prevIndex = activeChapterIndex - 1
                if (prevIndex >= 0 && prevIndex < chapters.size) {
                    onJumpDirectionSet?.invoke(true)   // 显示目标章的末页
                    onActiveChapterChanged(prevIndex)
                }
            },
            onReachEnd = {
                // 在末页继续右滑 → 跳到下章**首页**
                val nextIndex = activeChapterIndex + 1
                if (nextIndex >= 0 && nextIndex < chapters.size) {
                    onJumpDirectionSet?.invoke(false)  // 显示目标章的首页
                    onActiveChapterChanged(nextIndex)
                }
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
