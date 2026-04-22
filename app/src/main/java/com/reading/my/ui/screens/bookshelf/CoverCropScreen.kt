package com.reading.my.ui.screens.bookshelf

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.theme.TextPrimary

/**
 * 封面裁剪页
 *
 * 支持：双指缩放图片、单指移动图片，固定裁剪框（3:4 书籍封面比例）
 * 点击"使用此封面"后将当前 URI 直接保存（完整裁剪需引入专用库，此处为轻量实现）
 *
 * TODO: [封面裁剪] 后续可引入 uCrop 或 android-image-cropper 实现像素级裁剪
 */
@Composable
fun CoverCropScreen(
    imageUri: String,
    onConfirm: (String) -> Unit,   // 返回裁剪后（当前为原图）URI
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── 顶部栏 ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "取消", tint = Color.White)
            }
            Text(
                text = "选择封面",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onConfirm(imageUri) }) {
                Text("使用此封面", color = PrimaryOrange, fontWeight = FontWeight.Medium)
            }
        }

        // ── 图片预览 + 裁剪框 ────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 4f)
                        offset += pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 图片（可移动/缩放）
            AsyncImage(
                model = Uri.parse(imageUri),
                contentDescription = "封面预览",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )

            // 裁剪框遮罩（3:4 比例）
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cropW = size.width * 0.7f
                val cropH = cropW * (4f / 3f)
                val left = (size.width - cropW) / 2f
                val top = (size.height - cropH) / 2f
                val cropRect = Rect(left, top, left + cropW, top + cropH)

                // 半透明遮罩
                drawRect(color = Color.Black.copy(alpha = 0.5f))
                // 清除裁剪区域（用白色模拟透明）
                drawRect(color = Color.Transparent, topLeft = cropRect.topLeft, size = cropRect.size)
                // 裁剪框边线
                drawRect(
                    color = Color.White,
                    topLeft = cropRect.topLeft,
                    size = cropRect.size,
                    style = Stroke(width = 2.dp.toPx())
                )
                // 四角标记
                val corner = 20.dp.toPx()
                val stroke = Stroke(width = 3.dp.toPx())
                // 左上
                drawLine(PrimaryOrange, cropRect.topLeft, cropRect.topLeft + Offset(corner, 0f), strokeWidth = 3.dp.toPx())
                drawLine(PrimaryOrange, cropRect.topLeft, cropRect.topLeft + Offset(0f, corner), strokeWidth = 3.dp.toPx())
                // 右上
                drawLine(PrimaryOrange, cropRect.topRight, cropRect.topRight + Offset(-corner, 0f), strokeWidth = 3.dp.toPx())
                drawLine(PrimaryOrange, cropRect.topRight, cropRect.topRight + Offset(0f, corner), strokeWidth = 3.dp.toPx())
                // 左下
                drawLine(PrimaryOrange, cropRect.bottomLeft, cropRect.bottomLeft + Offset(corner, 0f), strokeWidth = 3.dp.toPx())
                drawLine(PrimaryOrange, cropRect.bottomLeft, cropRect.bottomLeft + Offset(0f, -corner), strokeWidth = 3.dp.toPx())
                // 右下
                drawLine(PrimaryOrange, cropRect.bottomRight, cropRect.bottomRight + Offset(-corner, 0f), strokeWidth = 3.dp.toPx())
                drawLine(PrimaryOrange, cropRect.bottomRight, cropRect.bottomRight + Offset(0f, -corner), strokeWidth = 3.dp.toPx())
            }
        }

        // ── 提示文字 ─────────────────────────────────────────
        Text(
            text = "双指缩放 · 单指移动",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp)
        )
    }
}
