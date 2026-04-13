package com.reading.my.core.reader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reading.my.core.reader.domain.ChapterPages
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme
import com.reading.my.core.reader.engine.BookPageRenderer

/**
 * 滚动模式阅读器（LazyColumn 连续滚动实现）
 *
 * 将所有页面垂直排列在 LazyColumn 中，用户上下滑动浏览。
 * 类似微信读书"连续模式"、浏览器滚动的体验。
 *
 * 特性：
 * - 垂直连续滚动，无分页边界
 * - 懒加载：只渲染可见区域内的页面
 * - 页面之间有微小间距分隔（视觉提示）
 *
 * @param chapterPages 当前章节的分页结果
 * @param config        排版参数
 * @param theme         阅读主题
 * @param onScrollProgress 滚动进度回调：(visibleStartRatio) → Unit
 */
@Composable
fun ScrollModeReader(
    chapterPages: ChapterPages,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    onScrollProgress: ((Float) -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = theme.backgroundColor),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            count = chapterPages.pages.size,
            key = { index -> "scroll-${chapterPages.chapterIndex}-$index" },
        ) { pageIndex ->
            val page = chapterPages.getPage(pageIndex) ?: return@items
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((config.screenHeightPx / config.density).dp)  // 一屏高度
            ) {
                BookPageRenderer(
                    page = page,
                    config = config,
                    theme = theme,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}
