package com.reading.my.core.reader.engine

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme

/**
 * 文本渲染器（TextRender）
 *
 * 职责：将一页文本内容按排版参数绘制到 Canvas 上。
 * 使用 Android Native Canvas 的 drawText 进行高性能文字渲染。
 *
 * 特性：
 * - 首行缩进（基于像素宽度精确偏移）
 * - 基于 Paint.measureText 的真实宽度换行（不依赖 charsPerLine 估算）
 * - 底部边界裁剪（防止文字溢出屏幕）
 */
object TextRender {

    private val TAG = "TextRender"

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

        val canvasWidth = drawScope.size.width
        val canvasHeight = drawScope.size.height
        // 文字绘制区域的右边界
        val maxDrawX = canvasWidth - config.horizontalPaddingPx
        // 绘制区域底部边界
        val maxDrawY = canvasHeight - config.verticalPaddingPx

        Log.d(TAG, "render: ${pageText.length}字, 画布=${canvasWidth.toInt()}x${canvasHeight.toInt()}, " +
                "可绘区域 x:[${config.horizontalPaddingPx.toInt()}, ${maxDrawX.toInt()}] y:[${config.verticalPaddingPx.toInt()}, ${maxDrawY.toInt()}]")

        val lines = pageText.split('\n')
        var y = config.verticalPaddingPx + config.lineHeightPx

        for ((lineIndex, line) in lines.withIndex()) {
            // 空行 → 段落间距
            if (line.isBlank()) {
                y += config.lineHeightPx
                continue
            }

            // 边界检查：当前 y 已超底边就停止
            if (y > maxDrawY) break

            // 判断是否为段落首行（需要缩进）
            val isFirstLineOfParagraph = (lineIndex == 0) ||
                (lineIndex > 0 && lines[lineIndex - 1].isBlank())

            // 首行缩进像素值（直接用配置计算，不依赖全角空格测量）
            val indentPx = if (isFirstLineOfParagraph && config.firstLineIndentChars > 0)
                config.firstLineIndentPx
            else
                0f

            // 本行起始 x = 左边距 + 缩进
            var x = config.horizontalPaddingPx + indentPx
            // 本行剩余可用宽度
            var remainingWidth = maxDrawX - x

            // 逐字符测量换行
            var i = 0
            while (i < line.length) {
                // 用 Paint.breakText 计算在 remainingWidth 内能放多少个字符
                val fitCount = paint.breakText(
                    line.substring(i), true, remainingWidth, null
                )
                if (fitCount <= 0) {
                    // 连一个字符都放不下（极端情况），强制放一个然后换行
                    val chunk = line[i].toString()
                    drawScope.drawContext.canvas.nativeCanvas.drawText(chunk, x, y, paint)
                } else {
                    val chunk = line.substring(i, i + fitCount)
                    drawScope.drawContext.canvas.nativeCanvas.drawText(chunk, x, y, paint)
                    i += fitCount
                    if (i >= line.length) break
                }

                // 换到下一行
                y += config.lineHeightPx
                // 超出底边界则停止
                if (y > maxDrawY) break

                // 新行重置 x 到左边距（后续行无缩进），重新计算可用宽度
                x = config.horizontalPaddingPx
                remainingWidth = maxDrawX - x
            }

            // 段落结束后推进 y（如果没被 break 跳出的话，这里确保段间距）
            if (y <= maxDrawY) {
                y += config.lineHeightPx
            }
        }

        Log.d(TAG, "render完成: 最终y=${y}px, 画布高=$canvasHeight")
    }

    internal fun colorToArgb(color: Color): Int =
        ((color.alpha * 255f).toInt() shl 24) or
        ((color.red * 255f).toInt() shl 16) or
        ((color.green * 255f).toInt() shl 8) or
        (color.blue * 255f).toInt()
}
