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

        // 从 config 统一读取间距参数
        val blankLineSpacingRatio = config.blankLineSpacingRatio
        val paraEndSpacingRatio = config.paraEndSpacingRatio

        Log.d(TAG, "=== 渲染 p${page.pageIndex} === 文本${pageText.length}字, 续接=${page.isContinuation}")
        Log.d(TAG, "  画布: ${canvasWidth.toInt()}x${canvasHeight.toInt()}px")
        Log.d(TAG, "  边距: 左右${config.horizontalPaddingPx.toInt()}px, 上下${config.verticalPaddingPx.toInt()}px")
        Log.d(TAG, "  可绘制区域: x=[${config.horizontalPaddingPx.toInt()}, ${maxDrawX.toInt()}], y=[${config.verticalPaddingPx.toInt()}, ${maxDrawY.toInt()}]")
        Log.d(TAG, "  行高=${config.lineHeightPx.toInt()}px, 字号=${config.fontSizePx.toInt()}px")
        Log.d(TAG, "  可用高度=${(maxDrawY - config.verticalPaddingPx).toInt()}px, 理论最大行数=${((maxDrawY - config.verticalPaddingPx) / config.lineHeightPx).toInt()}")

        val lines = pageText.split('\n')
        var y = config.verticalPaddingPx + config.lineHeightPx
        var lineCounter = 0  // 实际绘制行计数（视觉子行）
        var totalCharsRendered = 0  // 实际绘制的总字符数
        val breakTextStats = mutableListOf<Int>()  // 每行 breakText 实际容纳字数

        Log.d(TAG, "  起始y=${y.toInt()}px (上边距+1行高), 共${lines.size}个文本段")
        Log.d(TAG, "  [诊断] charsPerLine(分页)=${config.charsPerLine}, contentWidthPx=${config.contentWidthPx.toInt()}, fontSizePx=${config.fontSizePx.toInt()}")

        for ((lineIndex, line) in lines.withIndex()) {
            // ====== 空行处理 ======
            if (line.isBlank()) {
                y += config.lineHeightPx * blankLineSpacingRatio
                Log.d(TAG, "  L${lineCounter}: 空行 → y+${(config.lineHeightPx * blankLineSpacingRatio).toInt()}=${y.toInt()}px")
                continue
            }
            if (y > maxDrawY) {
                Log.d(TAG, "  ⚠️ 空行检查: y=${y.toInt()} > maxDrawY=${maxDrawY.toInt()}, 停止!")
                break
            }

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

            Log.d(TAG, "  L${lineCounter}: [${line.take(15)}...] indent=$indentPx, 起始y=${y.toInt()}px, 可用宽=${remainingWidth.toInt()}px")

            // ====== 逐字符测量换行 + 禁则处理 ======
            var i = 0
            var subLineIndex = 0
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
                    totalCharsRendered += 1
                    i += 1
                } else {
                    val chunk = line.substring(i, i + fitCount)
                    drawScope.drawContext.canvas.nativeCanvas.drawText(chunk, x, y, paint)
                    i += fitCount
                    totalCharsRendered += fitCount
                }

                breakTextStats.add(if (fitCount <= 0) 1 else fitCount)
                subLineIndex++
                Log.d(TAG, "    子行${subLineIndex}: breakText=${fitCount}字(可用宽=${remainingWidth.toInt()}px), 累计y=${y.toInt()}px")

                // 每个子行都必须递增 y（包括最后一个子行！）
                y += config.lineHeightPx
                lineCounter++

                if (y > maxDrawY) {
                    Log.d(TAG, "    子行绘制后: y=${y.toInt()} > maxDrawY=${maxDrawY.toInt()}, 停止!")
                    break
                }

                // 后续行回到左边距，无缩进
                x = config.horizontalPaddingPx
                remainingWidth = maxDrawX - x
            }

            // 段落后推进 y（使用 paraEndSpacingRatio 控制段间距）
            if (y <= maxDrawY) {
                y += config.lineHeightPx * paraEndSpacingRatio
                Log.d(TAG, "  段落间距: y+${(config.lineHeightPx * paraEndSpacingRatio).toInt()}=${y.toInt()}px")
            }
        }

        val bottomWaste = canvasHeight - y
        // 计算分页端预估行数（用 charsPerLine 反算）
        val estLinesByCharCount = if (config.charsPerLine > 0) {
            kotlin.math.ceil(pageText.length.toDouble() / config.charsPerLine).toInt()
        } else { 0 }
        Log.d(TAG, "=== 完成 p${page.pageIndex} === 最终y=${y.toInt()}/${canvasHeight.toInt()}px, " +
                "底部浪费=${bottomWaste.toInt()}px(${(bottomWaste / canvasHeight * 100).toInt()}%), 共绘制${lineCounter}行")
        Log.d(TAG, "  [诊断汇总] 文本=${pageText.length}字, 渲染总字数=${totalCharsRendered}, 绘制子行=${lineCounter}")
        Log.d(TAG, "  [诊断] 每行breakText字数: ${breakTextStats.joinToString(",")} (平均=${if (breakTextStats.isNotEmpty()) String.format("%.1f", breakTextStats.average()) else "N/A"})")
        Log.d(TAG, "  [诊断] 分页charsPerLine=${config.charsPerLine}, 按文本/${config.charsPerLine}≈${estLinesByCharCount}文本行 vs 实际${lineCounter}绘制子行")
    }

    internal fun colorToArgb(color: Color): Int =
        ((color.alpha * 255f).toInt() shl 24) or
        ((color.red * 255f).toInt() shl 16) or
        ((color.green * 255f).toInt() shl 8) or
        (color.blue * 255f).toInt()
}
