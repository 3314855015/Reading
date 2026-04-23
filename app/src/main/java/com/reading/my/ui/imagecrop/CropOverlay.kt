package com.reading.my.ui.imagecrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.reading.my.ui.theme.PrimaryOrange

/**
 * 裁剪框遮罩组件（纯 UI，无交互）
 *
 * 功能：
 * 1. 绘制四条半透明遮罩带（上/下/左/右），中间裁剪区留空
 * 2. 绘制白色边线（矩形）或圆形边线（头像）
 * 3. 绘制四角/四方向橙色标记
 *
 * @param config 裁剪配置（决定裁剪框比例，如 1:1 或 3:4）
 * @param isCircle 是否圆形裁剪框（true=圆形头像, false=矩形封面）
 * @param cornerColor 标记颜色（默认 PrimaryOrange）
 */
@Composable
fun CropOverlay(
    config: CropConfig,
    isCircle: Boolean = false,
    cornerColor: Color = PrimaryOrange,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Canvas(modifier = modifier) {
        val containerW = size.width
        val containerH = size.height

        // ── 计算裁剪框 ──
        val cropW = containerW * 0.7f
        val cropH = cropW * (config.aspectRatioH / config.aspectRatioW)
        val left = (containerW - cropW) / 2f
        val top = (containerH - cropH) / 2f
        val right = left + cropW
        val bottom = top + cropH

        // ── 1. 四条半透明遮罩带（挖空中间裁剪区域）──
        val maskColor = Color.Black.copy(alpha = 0.55f)
        // 上方
        drawRect(maskColor, topLeft = Offset(0f, 0f), size = Size(containerW, top))
        // 下方
        drawRect(maskColor, topLeft = Offset(0f, bottom), size = Size(containerW, containerH - bottom))
        // 左侧
        drawRect(maskColor, topLeft = Offset(left, 0f), size = Size(left, cropH))
        // 右侧
        drawRect(maskColor, topLeft = Offset(right, 0f), size = Size(containerW - right, cropH))

        // ── 2. 裁剪框边线 ──
        if (isCircle) {
            val cx = left + cropW / 2f
            val cy = top + cropH / 2f
            val radius = minOf(cropW, cropH) / 2f
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
        } else {
            drawRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(cropW, cropH),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // ── 3. 四角/方向标记 ──
        val cornerLen = 20.dp.toPx()
        val cornerStrokeWidth = 3.dp.toPx()

        if (isCircle) {
            // 圆形：四个方向的短线标记
            val centerX = left + cropW / 2f
            val centerY = top + cropH / 2f
            val r = minOf(cropW, cropH) / 2f
            drawLine(cornerColor, Offset(centerX, centerY - r), Offset(centerX, centerY - r + cornerLen), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(centerX, centerY + r), Offset(centerX, centerY + r - cornerLen), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(centerX - r, centerY), Offset(centerX - r + cornerLen, centerY), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(centerX + r, centerY), Offset(centerX + r - cornerLen, centerY), strokeWidth = cornerStrokeWidth)
        } else {
            // 矩形：四角 L 形标记
            drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = cornerStrokeWidth)
        }
    }
}
