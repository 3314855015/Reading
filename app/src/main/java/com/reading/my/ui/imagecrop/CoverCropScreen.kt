package com.reading.my.ui.imagecrop

import androidx.compose.runtime.Composable
import com.reading.my.ui.imagecrop.CropConfig
import com.reading.my.ui.imagecrop.CropScreen

/**
 * 封面裁剪页（薄包装层）
 *
 * 复用通用 [CropScreen]，仅传入封面专属参数：
 * - 3:4 竖版书籍比例
 * - 矩形裁剪框
 * - 输出 800px 以内的 Base64（封面需要更高分辨率）
 */
@Composable
fun CoverCropScreen(
    imageUri: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    CropScreen(
        imageUri = imageUri,
        config = CropConfig.CoverPortrait,
        title = "选择封面",
        confirmText = "使用此封面",
        hintText = "双指缩放 · 单指移动",
        isCircle = false,
        maxDimension = 800,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
