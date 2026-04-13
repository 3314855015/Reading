package com.reading.my.core.reader.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.nativeCanvas
import com.reading.my.core.reader.domain.Page
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme

/**
 * 渲染单页内容为 Composable
 */
@Composable
fun BookPageRenderer(
    page: Page,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    modifier: Modifier = Modifier,
    showPageNumber: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = theme.backgroundColor)
            .drawBehind {
                // 绘制正文
                TextRender.renderPage(this, page.text, config, theme)

                // 可选：绘制页码
                if (showPageNumber) {
                    val pageNumPaint = android.graphics.Paint().apply {
                        color = TextRender.colorToArgb(theme.secondaryColor)
                        textSize = 12f * config.density
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${page.pageIndex + 1}",
                        size.width - config.horizontalPaddingPx,
                        size.height - config.verticalPaddingPx / 2,
                        pageNumPaint,
                    )
                }
            },
    )
}
