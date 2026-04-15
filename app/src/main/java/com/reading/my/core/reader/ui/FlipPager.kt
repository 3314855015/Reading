package com.reading.my.core.reader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import android.util.Log
import com.reading.my.core.reader.domain.ChapterPages
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme
import com.reading.my.core.reader.engine.BookPageRenderer
import kotlinx.coroutines.flow.distinctUntilChanged

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
 * ## 章节边界跳转（核心机制）
 *
 * **问题**：HorizontalPager 在到达首/末页时会自动吸附（snap back）。
 * 我们需要区分两种场景：
 * - **正常到达边界**（从 p1 翻到 p0）：不应触发跨章跳转 ❌
 * - **在边界处继续往边缘方向滑**（在 p0 继续左滑）：应触发跨章跳转 ✅
 *
 * **实现**：监听 `isScrollInProgress` 的状态变化：
 * 1. 滚动开始时记录当前页码
 * 2. 滚动结束时检查：
 *    - 如果起始和结束是同一个边界页码（0 或末页）→ 用户尝试了越界 → 触发回调
 *    - 如果页码发生了变化（包括正常翻到边界） → 不触发
 *
 * @param chapterPages 当前章节的分页结果（来自 PageLayoutManager）
 * @param config        排版参数
 * @param theme         阅读主题
 * @param initialPage   初始显示的页码（默认 0）
 * @param onPageChange  翻页回调：(pageIndex) -> Unit
 * @param onReachStart  在第一页继续左滑 → 跳上一章末页
 * @param onReachEnd    在最后一页继续右滑 → 跳下一章首页
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlipPagerReader(
    chapterPages: ChapterPages,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    bookId: String = "",
    initialPage: Int = 0,
    onPageChange: ((pageIndex: Int) -> Unit)? = null,
    onReachStart: (() -> Unit)? = null,
    onReachEnd: (() -> Unit)? = null,
) {
    val pageCount = chapterPages.pageCount.coerceAtLeast(1)
    val resolvedInitialPage = initialPage.coerceIn(0, pageCount - 1)

    val pagerState = rememberPagerState(
        initialPage = 0, // ★ 始终从 0 开始，跨章跳页由下面的 scrollToPage 处理
        pageCount = { pageCount },
    )

    // 保持最新引用的回调（避免闭包捕获过期值）
    val currentOnReachStart by rememberUpdatedState(onReachStart)
    val currentOnReachEnd by rememberUpdatedState(onReachEnd)

    // ---- 0) 跨章跳转到目标页 [Bug2: scrollToPage 排查中] ----
    Log.d("FlipPager", "📌 FlipPagerReader入口: ch${chapterPages.chapterIndex}, pageCount=$pageCount, initialPage=$resolvedInitialPage, pager.currentPage=${pagerState.currentPage}")

    LaunchedEffect(chapterPages.chapterIndex, resolvedInitialPage) {
        Log.d("FlipPager", "🎯 LaunchedEffect触发: chapter=${chapterPages.chapterIndex}, targetPage=$resolvedInitialPage")
        Log.d("FlipPager", "🎯 触发时pagerState: currentPage=${pagerState.currentPage}, pageCount=${pagerState.pageCount}, canFwd=${pagerState.canScrollForward}, canBack=${pagerState.canScrollBackward}")

        // 等待 pagerState 准备就绪
        if (!pagerState.canScrollForward && !pagerState.canScrollBackward) {
            Log.w("FlipPager", "⚠️ pagerState未ready: canFwd=false, canBack=false, 等待...")
        }

        if (resolvedInitialPage != pagerState.currentPage) {
            try {
                Log.d("FlipPager", "🎯 执行scrollToPage: ${pagerState.currentPage} → $resolvedInitialPage")
                pagerState.scrollToPage(resolvedInitialPage)
                Log.d("FlipPager", "🎯 scrollToPage返回后: currentPage=${pagerState.currentPage}")
            } catch (e: Exception) {
                Log.e("FlipPager", "❌ scrollToPage异常: ${e.message}", e)
            }
        } else {
            Log.d("FlipPager", "🏁 无需跳转: currentPage(${pagerState.currentPage}) == target($resolvedInitialPage)")
        }
        Log.d("FlipPager", "🎯 最终状态: currentPage=${pagerState.currentPage}, pageCount=${pagerState.pageCount}")
    }

    // ---- 1) 正常翻页回调 ----
    if (onPageChange != null) {
        LaunchedEffect(pagerState) {
            androidx.compose.runtime.snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    onPageChange(page)
                }
        }
    }

    // ---- 2) 边界越跳转检测（Overscroll Detection）----
    // 核心逻辑：
    //   滚动开始时记录起始页码 → 滚动结束后若仍在同一边界页 → 触发跨章
    //   这排除了"正常翻到边界"的情况（那种情况页码会变化）
    if (onReachStart != null || onReachEnd != null) {
        LaunchedEffect(pagerState) {
            var scrollStartPage = -1

            androidx.compose.runtime.snapshotFlow { pagerState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    if (isScrolling) {
                        // 记录滚动开始时的页码
                        scrollStartPage = pagerState.currentPage
                    } else {
                        // 滚动结束 → 检测是否为越界操作
                        val settledPage = pagerState.currentPage
                        if (scrollStartPage == settledPage && scrollStartPage >= 0) {
                            // 页码没变 → 用户尝试了越界
                            if (settledPage == 0 && currentOnReachStart != null) {
                                currentOnReachStart?.invoke()
                            } else if (settledPage == pageCount - 1
                                && currentOnReachEnd != null
                                && pageCount > 1
                            ) {
                                currentOnReachEnd?.invoke()
                            }
                        }
                        scrollStartPage = -1
                    }
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
                    bookId = bookId,
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
