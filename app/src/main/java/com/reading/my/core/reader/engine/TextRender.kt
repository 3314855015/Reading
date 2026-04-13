package com.reading.my.core.reader.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme

/**
 * 文本渲染器（TextRender）
 *
 * 职责：将一页文本内容按排版参数绘制到 Canvas 上。
 * 使用 Android Native Canvas 的 drawText 进行高性能文字渲染。
 */
object TextRender {

    /**
     * 在指定区域绘制页面文本
     *
     * @param drawScope   Compose DrawScope
     * @param pageText    页面纯文本（来自 Page.text）
     * @param config      排版参数
     * @param theme       阅读主题
     */
    fun renderPage(
        drawScope: DrawScope,
        pageText: String,
        config: PageLayoutConfig,
        theme: ReaderTheme,
    ) {
        if (pageText.isBlank()) return

        val paint = android.graphics.Paint().apply {
            color = colorToArgb(theme.textColor)
            textSize = config.fontSizePx
            isAntiAlias = true
        }

        val lines = pageText.split('\n')
        var y = config.verticalPaddingPx + config.lineHeightPx

        for (line in lines) {
            if (line.isBlank()) {
                y += config.lineHeightPx
                continue
            }
            val chunks = wrapLine(line, config.charsPerLine)
            for (chunk in chunks) {
                drawScope.drawContext.canvas.nativeCanvas.drawText(
                    chunk, config.horizontalPaddingPx, y, paint
                )
                y += config.lineHeightPx
            }
        }
    }

    internal fun wrapLine(text: String, maxCharsPerLine: Int): List<String> {
        if (text.length <= maxCharsPerLine) return listOf(text)
        val result = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            result.add(remaining.take(maxCharsPerLine))
            remaining = remaining.drop(maxCharsPerLine)
        }
        return result
    }

    internal fun colorToArgb(color: Color): Int =
        ((color.alpha * 255f).toInt() shl 24) or
        ((color.red * 255f).toInt() shl 16) or
        ((color.green * 255f).toInt() shl 8) or
        (color.blue * 255f).toInt()
}
