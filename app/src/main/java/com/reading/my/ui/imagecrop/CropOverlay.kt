package com.reading.my.ui.imagecrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.reading.my.ui.theme.PrimaryOrange

/**
 * 裁剪框遮罩组件（纯 UI，无交互）
 *
 * 功能：
 * 1. 绘制半透明遮罩，裁剪区域透明可见（矩形或圆形镂空）
 * 2. 绘制白色边线（矩形）或圆形边线（头像）
 * 3. 绘制四角/四方向橙色标记
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
        val left   = (containerW - cropW) / 2f
        val top    = (containerH - cropH) / 2f
        val right  = left + cropW
        val bottom = top  + cropH

        // ── 1. 半透明遮罩带 ──
        val maskColor = Color.Black.copy(alpha = 0.55f)

        if (isCircle) {
            // 圆形模式：全屏遮罩 + 圆形镂空
            val cx     = left + cropW / 2f
            val cy     = top  + cropH / 2f
            val radius = minOf(cropW, cropH) / 2f

            // 用 Path.combine 做差集运算：外矩形 - 内圆
            val outerRect = androidx.compose.ui.geometry.Rect(0f, 0f, containerW, containerH)
            val innerOval = androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius)
            val maskPath = Path().apply {
                addRect(outerRect)
                addOval(innerOval)
                fillType = PathFillType.EvenOdd  // 叠加区域自动镂空
            }

            drawPath(path = maskPath, color = maskColor)
        } else {
            // 矩形模式：四条遮罩带
            drawRect(maskColor, topLeft = Offset(0f, 0f), size = Size(containerW, top))
            drawRect(maskColor, topLeft = Offset(0f, bottom), size = Size(containerW, containerH - bottom))
            drawRect(maskColor, topLeft = Offset(0f, top), size = Size(left, cropH))
            drawRect(maskColor, topLeft = Offset(right, top), size = Size(containerW - right, cropH))
        }

        // ── 2. 裁剪框边线 ──
        if (isCircle) {
            val cx     = left + cropW / 2f
            val cy     = top  + cropH / 2f
            val radius = minOf(cropW, cropH) / 2f
            drawCircle(
                color  = Color.White,
                radius = radius,
                center = Offset(cx, cy),
                style  = Stroke(width = 2.dp.toPx())
            )
        } else {
            drawRect(
                color   = Color.White,
                topLeft = Offset(left, top),
                size    = Size(cropW, cropH),
                style   = Stroke(width = 2.dp.toPx())
            )
        }

        // ── 3. 四角/方向标记 ──
        val cornerLen         = 20.dp.toPx()
        val cornerStrokeWidth = 3.dp.toPx()

        if (isCircle) {
            // 圆形：四个方向的短线标记（上/下/左/右）
            val cx = left + cropW / 2f
            val cy = top  + cropH / 2f
            val r  = minOf(cropW, cropH) / 2f
            drawLine(cornerColor, Offset(cx, cy - r), Offset(cx, cy - r + cornerLen), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(cx, cy + r), Offset(cx, cy + r - cornerLen), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(cx - r, cy), Offset(cx - r + cornerLen, cy), strokeWidth = cornerStrokeWidth)
            drawLine(cornerColor, Offset(cx + r, cy), Offset(cx + r - cornerLen, cy), strokeWidth = cornerStrokeWidth)
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
