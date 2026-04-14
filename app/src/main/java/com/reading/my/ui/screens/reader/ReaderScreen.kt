package com.reading.my.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.reading.my.core.reader.engine.RenderCache
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme
import com.reading.my.core.reader.ui.FlipPagerReader
import com.reading.my.domain.model.Chapter

/**
 * 阅读器主界面（Phase 7: ViewModel 状态管理重构）
 *
 * UI 层职责：
 * 1. 观察 ReaderViewModel 的 StateFlow 渲染 UI
 * 2. 处理 Compose 侧配置计算（config、theme、configHash）
 * 3. 将用户操作（跨章翻页）委托给 ViewModel
 *
 * 业务逻辑（章节切换、分页加载、目标页解析）全部在 ReaderViewModel 中。
 *
 * @param chapters        全部章节列表（用于跨章跳转）
 * @param currentChapterIndex 当前章节索引（初始定位）
 * @param bookTitle       书名（显示在顶部栏）
 * @param totalChapters   总章数（用于进度计算，可选）
 * @param bookId          书籍 ID（L2 缓存 key）
 * @param onChapterChange 章节切换回调：(newChapterIndex) → Unit
 * @param onBack          返回上一级
 * @param onPageChange    翻页回调：(chapterIndex, pageIndex) → Unit
 */
@Composable
fun ReaderScreen(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    bookTitle: String,
    totalChapters: Int = 1,
    bookId: String = "",
    onChapterChange: ((newChapterIndex: Int) -> Unit)? = null,
    onBack: () -> Unit = {},
    onPageChange: ((chapterIndex: Int, pageIndex: Int) -> Unit)? = null,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // ---- 构建排版配置 ----
    val systemBarsHeightDp = 48f
    val densityVal = density.density
    val config = remember {
        PageLayoutConfig.default(
            screenWidthPx = (configuration.screenWidthDp * densityVal).toInt(),
            screenHeightPx = ((configuration.screenHeightDp - systemBarsHeightDp) * densityVal).toInt(),
            density = densityVal,
        )
    }

    // ---- 选择主题（默认日间） ----
    val theme = remember { ReaderTheme.DayLight }

    // ---- 计算配置哈希（L1/L2 共用）----
    val configHash = remember(config, theme) {
        RenderCache.computeConfigHash(config, theme)
    }

    // ---- 观察状态 ----
    val uiState by viewModel.uiState.collectAsState()
    val currentChapter = remember(chapters, uiState.activeChapterIndex) {
        chapters.getOrNull(uiState.activeChapterIndex)
    }

    // ---- 初始化 ViewModel（仅首次 composition 时执行一次）----
    LaunchedEffect(Unit) {
        viewModel.initReader(chapters, currentChapterIndex, bookId)
        viewModel.setConfig(config, theme)
    }

    // ---- 当章节或 config 变化时加载分页数据 ----
    LaunchedEffect(uiState.activeChapterIndex, configHash) {
        viewModel.loadCurrentChapter(configHash)
    }

    // ---- UI 渲染 ----
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = theme.backgroundColor)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.matchParentSize())
            }
            uiState.chapterPages != null && currentChapter != null -> {
                ReaderContent(
                    chapter = currentChapter!!,
                    chapterPages = uiState.chapterPages!!,
                    chapters = chapters,
                    activeChapterIndex = uiState.activeChapterIndex,
                    initialPage = uiState.targetInitialPage,
                    onActiveChapterChanged = { newIndex ->
                        // 外部回调通知（MainScreen 更新 readerState）
                        onChapterChange?.invoke(newIndex)
                    },
                    onNavigatePrev = { viewModel.navigateToPrevChapter() },
                    onNavigateNext = { viewModel.navigateToNextChapter() },
                    config = config,
                    theme = theme,
                    bookTitle = bookTitle,
                    totalChapters = totalChapters.coerceAtLeast(chapters.size),
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
    chapterPages: com.reading.my.core.reader.domain.ChapterPages,
    chapters: List<Chapter>,
    activeChapterIndex: Int,
    initialPage: Int = 0,
    onActiveChapterChanged: (newIndex: Int) -> Unit,
    onNavigatePrev: () -> Unit,
    onNavigateNext: () -> Unit,
    config: com.reading.my.core.reader.domain.PageLayoutConfig,
    theme: ReaderTheme,
    bookTitle: String,
    totalChapters: Int,
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
            initialPage = initialPage,
            onPageChange = { pageIndex ->
                onPageChange?.invoke(chapter.chapterIndex, pageIndex)
            },
            onReachStart = {
                // 在首页继续左滑 → 跳到上章**末页**
                if (activeChapterIndex > 0) {
                    onNavigatePrev()
                    onActiveChapterChanged(activeChapterIndex - 1)
                }
            },
            onReachEnd = {
                // 在末页继续右滑 → 跳到下章**首页**
                if (activeChapterIndex < chapters.size - 1) {
                    onNavigateNext()
                    onActiveChapterChanged(activeChapterIndex + 1)
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
