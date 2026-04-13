package com.reading.my.core.reader.engine

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.reading.my.core.reader.domain.Page
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
 *   - 续接段落的第一行自动跳过缩进
 *   - 同一页内后续的新段落正常有缩进
 * - 基于 Paint.breakText 的真实宽度换行
 * - 中文排版禁则：行首不允许出现标点符号，自动回退到上一行末尾
 * - 底部边界裁剪（防止文字溢出屏幕）
 */
object TextRender {

    private val TAG = "TextRender"

    /**
     * 行首禁止出现的字符集合（中文排版禁则）
     */
    private val FORBIDDEN_START_CHARS = setOf(
        '，', '。', '！', '？', '：', '；', '、',
        '》', '”', '’', '】', '）',
        '…', '·', '—', '～',
        ',', '.', '!', '?', ':', ';',
        '"', "'", '>', ')', ']', '}'
    )

    /**
     * 在指定区域绘制页面文本
     *
     * @param page   页面对象（包含文本和是否为续接段落的标记）
     * @param config 排版参数
     * @param theme  阅读主题
     */
    fun renderPage(
        drawScope: DrawScope,
        page: Page,
        config: PageLayoutConfig,
        theme: ReaderTheme,
    ) {
        val pageText = page.text
        if (pageText.isBlank()) return

        val paint = android.graphics.Paint().apply {
            color = colorToArgb(theme.textColor)
            textSize = config.fontSizePx
            isAntiAlias = true
        }

        // 字体：默认系统字体，可通过 paint.typeface 切换
        // 例：paint.typeface = Typeface.create("宋体", Typeface.NORMAL)

        val canvasWidth = drawScope.size.width
        val canvasHeight = drawScope.size.height
        val maxDrawX = canvasWidth - config.horizontalPaddingPx
        val maxDrawY = canvasHeight - config.verticalPaddingPx

        // 空行间距比例
        val blankLineSpacingRatio = 0.4f

        Log.d(TAG, "p${page.pageIndex}: ${pageText.length}字, 续接=${page.isContinuation}, " +
                "${canvasWidth.toInt()}x${canvasHeight.toInt()}")

        val lines = pageText.split('\n')
        var y = config.verticalPaddingPx + config.lineHeightPx

        for ((lineIndex, line) in lines.withIndex()) {
            // ====== 空行处理 ======
            if (line.isBlank()) {
                y += config.lineHeightPx * blankLineSpacingRatio
                continue
            }
            if (y > maxDrawY) break

            // ====== 首行缩进判断（修复：isContinuation 只影响第一行）======
            //
            // 规则：
            // - page.isContinuation=true 且 lineIndex==0 → 不缩进（跨页续接的首行）
            // - 其他情况按正常逻辑：lineIndex==0 或前一行是空白 → 缩进（新段落首行）
            //
            val isFirstLineOfPage = (lineIndex == 0)
            val prevLineIsBlank = (lineIndex > 0 && lines[lineIndex - 1].isBlank())

            val shouldIndent = when {
                // 跨页续接的首行 → 不缩进
                page.isContinuation && isFirstLineOfPage -> false
                // 新段落的首行（页首 或 前面有空行）→ 缩进
                isFirstLineOfPage || prevLineIsBlank -> true
                // 段落中间的续行 → 不缩进
                else -> false
            }

            val indentPx = if (shouldIndent && config.firstLineIndentChars > 0)
                config.firstLineIndentPx
            else
                0f

            var x = config.horizontalPaddingPx + indentPx
            var remainingWidth = maxDrawX - x

            // ====== 逐字符测量换行 + 禁则处理 ======
            var i = 0
            while (i < line.length) {
                var fitCount = paint.breakText(line.substring(i), true, remainingWidth, null)

                // 禁则：断点后是行首禁止标点 → 回退一字
                if (fitCount in 1 until (line.length - i)) {
                    val nextChar = line[i + fitCount]
                    if (nextChar in FORBIDDEN_START_CHARS) {
                        fitCount--
                    }
                }

                if (fitCount <= 0) {
                    drawScope.drawContext.canvas.nativeCanvas.drawText(
                        line[i].toString(), x, y, paint
                    )
                } else {
                    val chunk = line.substring(i, i + fitCount)
                    drawScope.drawContext.canvas.nativeCanvas.drawText(chunk, x, y, paint)
                    i += fitCount
                    if (i >= line.length) break
                }

                y += config.lineHeightPx
                if (y > maxDrawY) break

                // 后续行回到左边距，无缩进
                x = config.horizontalPaddingPx
                remainingWidth = maxDrawX - x
            }

            // 段落后推进 y（未超底边的话）
            if (y <= maxDrawY) {
                y += config.lineHeightPx
            }
        }

        Log.d(TAG, "完成 p${page.pageIndex}: 最终y=${y}/${canvasHeight}px, 剩余=${(canvasHeight - y).toInt()}px")
    }

    internal fun colorToArgb(color: Color): Int =
        ((color.alpha * 255f).toInt() shl 24) or
        ((color.red * 255f).toInt() shl 16) or
        ((color.green * 255f).toInt() shl 8) or
        (color.blue * 255f).toInt()
}
