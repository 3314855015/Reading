package com.reading.my.core.reader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.reading.my.core.reader.domain.ChapterPages
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme
import com.reading.my.core.reader.engine.BookPageRenderer
import kotlinx.coroutines.flow.distinctUntilChanged

/** 翻页模式枚举 */
enum class FlipMode {
    HORIZONTAL,
    SCROLL,
}

/**
 * 翻页模式阅读器（HorizontalPager 实现）
 *
 * 使用 Compose Foundation 的 HorizontalPager 实现左右滑动翻页，
 * 提供类似 Kindle / 微信读书翻页的交互体验。
 *
 * 特性：
 * - 左右滑动切换页面，带弹簧动画
 * - 自动预加载相邻页面（±1）
 * - 翻页回调（用于进度保存、预加载触发）
 * - 支持跳转到指定页码
 *
 * @param chapterPages 当前章节的分页结果（来自 PageLayoutManager）
 * @param config        排版参数
 * @param theme         阅读主题
 * @param initialPage   初始显示的页码（默认 0）
 * @param onPageChange  翻页回调：(chapterIndex, pageIndex) → Unit
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlipPagerReader(
    chapterPages: ChapterPages,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    initialPage: Int = 0,
    onPageChange: ((pageIndex: Int) -> Unit)? = null,
) {
    val pageCount = chapterPages.pageCount.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount },
    )

    // 监听翻页事件并回调
    if (onPageChange != null) {
        LaunchedEffect(pagerState) {
            // snapshotFlow 将 PagerState 的 currentPage 变为 Flow<Int>
            androidx.compose.runtime.snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    onPageChange(page)
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> "$index-${chapterPages.chapterIndex}" },
        ) { pageIndex ->
            val page = chapterPages.getPage(pageIndex)
            if (page != null) {
                BookPageRenderer(
                    page = page,
                    config = config,
                    theme = theme,
                    showPageNumber = true,
                )
            }
        }
    }
}

// ==================== 公开 API ====================

/** 翻页模式阅读器的公开接口 */
object FlipModeReader {

    /**
     * 创建一个可记忆翻页状态的阅读器
     *
     * 与 [FlipPagerReader] 相同功能，提供面向对象的调用风格。
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun create(
        chapterPages: ChapterPages,
        config: PageLayoutConfig,
        theme: ReaderTheme,
        initialPage: Int = 0,
        onPageChange: ((Int) -> Unit)? = null,
    ) {
        FlipPagerReader(
            chapterPages = chapterPages,
            config = config,
            theme = theme,
            initialPage = initialPage,
            onPageChange = onPageChange,
        )
    }
}
