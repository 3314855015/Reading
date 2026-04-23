package com.reading.my.ui.imagecrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reading.my.ui.theme.PrimaryOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * 通用图片裁剪页（共享核心组件）
 *
 * 封装了所有裁剪页的公共逻辑：
 * - 图片尺寸异步加载 + 容器尺寸跟踪 + 自动初始化
 * - 双指缩放 / 单指平移手势处理（带边界限制）
 * - AsyncImage 渲染 + CropOverlay 遮罩层
 * - 标准布局：标题栏(返回+标题+确认) + 预览区 + 提示文字
 *
 * 各业务页面只需传入差异化参数即可：
 * @param imageUri     图片 URI 字符串
 * @param config       裁剪配置（比例、缩放范围等）
 * @param title        顶部标题文字（如 "裁剪头像" / "选择封面"）
 * @param confirmText  确认按钮文字（如 "使用" / "使用此封面"）
 * @param hintText     底部提示文字
 * @param isCircle     是否圆形裁剪框（头像=true, 封面=false）
 * @param maxDimension 输出 Base64 时的最大边长像素值
 * @param onConfirm    确认回调，返回 Base64 编码字符串
 * @param onDismiss    取消/返回回调
 */
@Composable
fun CropScreen(
    imageUri: String,
    config: CropConfig,
    title: String = "裁剪图片",
    confirmText: String = "使用",
    hintText: String = "双指缩放 · 单指移动",
    isCircle: Boolean = false,
    maxDimension: Int = 512,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cropState = remember { CropState(config) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    // ── 异步加载图片原始尺寸 ──────────
    LaunchedEffect(imageUri) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
                imageSize = Size(options.outWidth.toFloat(), options.outHeight.toFloat())
            }
            if (containerSize != IntSize.Zero && imageSize != Size.Zero) {
                cropState.initContainer(
                    size = Size(containerSize.width.toFloat(), containerSize.height.toFloat()),
                    imageSize = imageSize
                )
            }
        } catch (_: Exception) {}
    }

    // ── 容器/图片就绪后自动初始化 ──────
    LaunchedEffect(containerSize, imageSize) {
        if (containerSize != IntSize.Zero && imageSize != Size.Zero) {
            cropState.initContainer(
                size = Size(containerSize.width.toFloat(), containerSize.height.toFloat()),
                imageSize = imageSize
            )
        }
    }

    // ── 整体布局 ────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── 标题栏 ────────────────────
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
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                coroutineScope.launch {
                    convertToBase64(context, imageUri, maxDimension)?.let { base64 ->
                        onConfirm(base64)
                    } ?: onDismiss()
                }
            }) {
                Text(confirmText, color = PrimaryOrange, fontWeight = FontWeight.Medium)
            }
        }

        // ── 图片预览区 + 裁剪遮罩 ────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        cropState.onZoom(zoomFactor = zoom)
                        cropState.onPan(pan = pan)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = Uri.parse(imageUri),
                contentDescription = "裁剪预览",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = cropState.scale
                        scaleY = cropState.scale
                        translationX = cropState.offset.x
                        translationY = cropState.offset.y
                    }
            )

            CropOverlay(
                config = config,
                isCircle = isCircle,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── 提示文字 ──────────────────
        Text(
            text = hintText,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 12.dp)
        )
    }
}

/**
 * 将图片 URI 转为 Base64 编码字符串
 *
 * @param context Android Context
 * @param imageUri 图片 URI 字符串（content:// 或 file://）
 * @param maxDimension 最大边长（像素），超过则等比缩小
 * @param quality JPEG 压缩质量 (0-100)
 * @return Base64 编码字符串，失败返回 null
 */
suspend fun convertToBase64(
    context: Context,
    imageUri: String,
    maxDimension: Int = 512,
    quality: Int = 85
): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream: InputStream = context.contentResolver.openInputStream(Uri.parse(imageUri)) ?: return@withContext null
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
        inputStream.close()

        val bitmap = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
            val ratio = maxDimension.toFloat() / maxOf(originalBitmap.width, originalBitmap.height).toFloat()
            val newW = (originalBitmap.width * ratio).toInt()
            val newH = (originalBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newW, newH, true).also {
                if (it !== originalBitmap) originalBitmap.recycle()
            }
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        outputStream.close()

        android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}
