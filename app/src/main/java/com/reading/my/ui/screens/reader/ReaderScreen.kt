package com.reading.my.ui.screens.reader

import android.util.DisplayMetrics
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat.getSystemService
import com.reading.my.core.reader.engine.PageLayoutManager
import com.reading.my.core.reader.engine.TextRender
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme
import com.reading.my.core.reader.domain.ChapterPages
import com.reading.my.core.reader.ui.FlipPagerReader
import com.reading.my.core.reader.ui.FlipMode
import com.reading.my.core.reader.ui.ScrollModeReader
import com.reading.my.domain.model.Chapter

/**
 * 阅读器主界面（Phase 4 核心组件）
 *
 * 完整的阅读流程：
 * 接收章节内容 → 分页 → 渲染 → 翻页交互 → 进度回调
 *
 * @param chapter       当前要阅读的章节（包含完整正文 content）
 * @param bookTitle     书名（显示在顶部栏）
 * @param totalChapters 总章数（用于进度计算）
 * @param onBack        返回上一级（详情页/书架）
 * @param onPageChange  翻页回调：(chapterIndex, pageIndex) → Unit
 */
@Composable
fun ReaderScreen(
    chapter: Chapter,
    bookTitle: String,
    totalChapters: Int = 1,
    onBack: () -> Unit = {},
    onPageChange: ((chapterIndex: Int, pageIndex: Int) -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density

    // ---- 构建排版配置 ----
    // 注意：使用 configuration.screenHeightDp 会包含系统状态栏和导航栏的高度，
    // 但实际 Compose Canvas 绘制区域不含这些，会导致分页高度偏大、底部留白。
    // 因此需要减去系统栏的预估高度（约 48dp：状态栏24dp + 导航栏24dp）
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

    // ---- 执行分页（仅当 chapter 变化时重新计算）----
    var chapterPages by remember(chapter.chapterIndex, chapter.content) {
        mutableStateOf<ChapterPages?>(null)
    }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(chapter) {
        isLoading = true
        try {
            val layoutManager = PageLayoutManager(config)
            val result = layoutManager.paginateChapter(chapter.chapterIndex, chapter.content)
            Log.d("ReaderScreen", "分页完成: ${result.pageCount}页")
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
            initialPage = 0,
            onPageChange = { pageIndex ->
                onPageChange?.invoke(chapter.chapterIndex, pageIndex)
            },
        )

        // ===== 顶部信息栏（可隐藏） =====
        if (showBars) {
            ReaderTopBar(
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
                theme = theme,
                onClose = { showBars = false },
            )
        }
    }
}
