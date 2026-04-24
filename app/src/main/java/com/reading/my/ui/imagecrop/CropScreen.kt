package com.reading.my.ui.imagecrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
 * - 图片尺寸异步加载 + 容器尺寸跟踪 + 自动初始化 CropState
 * - 单指拖动平移手势处理（带边界限制，已移除缩放）
 * - AsyncImage 渲染 + CropOverlay 遮罩层
 * - 保存时按裁剪框实际裁剪图片原始像素后输出 Base64
 * - 标准布局：标题栏(返回+标题+确认) + 预览区 + 提示文字
 *
 * @param imageUri     图片 URI 字符串
 * @param config       裁剪配置（比例）
 * @param title        顶部标题文字
 * @param confirmText  确认按钮文字
 * @param hintText     底部提示文字
 * @param isCircle     是否圆形裁剪框
 * @param maxDimension 输出图片最大边长（像素）
 * @param onConfirm    确认回调，返回裁剪后的 Base64 编码字符串
 * @param onDismiss    取消/返回回调
 */
@Composable
fun CropScreen(
    imageUri: String,
    config: CropConfig,
    title: String = "裁剪图片",
    confirmText: String = "使用",
    hintText: String = "单指移动调整位置",
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

    // ── 异步加载图片原始尺寸（仅读取尺寸，不解码完整位图）──
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

    // ── 容器/图片两者都就绪后自动初始化 ──
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
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "取消",
                    tint = Color.White
                )
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
                    cropAndConvertToBase64(
                        context = context,
                        imageUri = imageUri,
                        cropState = cropState,
                        containerSizePx = containerSize,
                        maxDimension = maxDimension
                    )?.let { base64 ->
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
                .clipToBounds()
                .pointerInput(Unit) {
                    // 仅保留单指拖动，已移除双指缩放
                    detectDragGestures { _, dragAmount ->
                        cropState.onPan(pan = dragAmount)
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
 * 按裁剪框实际裁剪图片并转为 Base64
 *
 * 算法：
 * 1. 解码原图完整位图
 * 2. 根据 cropState 的 scale/offset/baseFitSize 还原裁剪框在原图中的像素坐标
 * 3. 从原图中 createBitmap 裁剪出对应区域
 * 4. 按 maxDimension 等比缩放
 * 5. 压缩为 JPEG 并输出 Base64
 *
 * @param context          Android Context
 * @param imageUri         图片 URI 字符串
 * @param cropState        当前裁剪交互状态
 * @param containerSizePx  预览容器的像素尺寸（IntSize）
 * @param maxDimension     输出图片最大边长（像素）
 * @param quality          JPEG 压缩质量 0-100
 * @return Base64 字符串，失败返回 null
 */
suspend fun cropAndConvertToBase64(
    context: Context,
    imageUri: String,
    cropState: CropState,
    containerSizePx: IntSize,
    maxDimension: Int = 512,
    quality: Int = 85
): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream: InputStream =
            context.contentResolver.openInputStream(Uri.parse(imageUri))
                ?: return@withContext null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: return@withContext null
        inputStream.close()

        // ── 1. 容器中心坐标 ──
        val containerW = containerSizePx.width.toFloat()
        val containerH = containerSizePx.height.toFloat()
        val containerCenterX = containerW / 2f
        val containerCenterY = containerH / 2f

        // ── 2. 当前已缩放的图片显示尺寸 ──
        val scaledSize = cropState.getScaledImageSize()

        // ── 3. 图片在容器中的左上角坐标（含 offset 平移）──
        val imgLeft = containerCenterX - scaledSize.width / 2f + cropState.offset.x
        val imgTop  = containerCenterY - scaledSize.height / 2f + cropState.offset.y

        // ── 4. 裁剪框在容器中的矩形 ──
        val cropRect = cropState.getCropRect()

        // ── 5. 从容器像素到原图像素的缩放系数（Fit 是等比缩放，X/Y 系数相同）──
        val scaleToImage = cropState.imageOriginalSize.width / scaledSize.width

        // ── 6. 裁剪框在原图像素坐标系中的位置 ──
        val srcLeft   = ((cropRect.left   - imgLeft) * scaleToImage).toInt()
            .coerceIn(0, originalBitmap.width)
        val srcTop    = ((cropRect.top    - imgTop)  * scaleToImage).toInt()
            .coerceIn(0, originalBitmap.height)
        val srcWidth  = (cropRect.width   * scaleToImage).toInt()
            .coerceIn(1, originalBitmap.width  - srcLeft)
        val srcHeight = (cropRect.height  * scaleToImage).toInt()
            .coerceIn(1, originalBitmap.height - srcTop)

        // ── 7. 从原图裁剪出目标区域 ──
        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap, srcLeft, srcTop, srcWidth, srcHeight
        )
        if (croppedBitmap !== originalBitmap) originalBitmap.recycle()

        // ── 8. 按 maxDimension 等比缩小 ──
        val outputBitmap = if (
            croppedBitmap.width > maxDimension || croppedBitmap.height > maxDimension
        ) {
            val ratio = maxDimension.toFloat() /
                    maxOf(croppedBitmap.width, croppedBitmap.height).toFloat()
            val newW = (croppedBitmap.width  * ratio).toInt()
            val newH = (croppedBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(croppedBitmap, newW, newH, true).also {
                if (it !== croppedBitmap) croppedBitmap.recycle()
            }
        } else {
            croppedBitmap
        }

        // ── 9. 压缩为 JPEG 并转 Base64 ──
        val outputStream = ByteArrayOutputStream()
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        if (outputBitmap !== croppedBitmap) outputBitmap.recycle()
        val byteArray = outputStream.toByteArray()
        outputStream.close()

        android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}
