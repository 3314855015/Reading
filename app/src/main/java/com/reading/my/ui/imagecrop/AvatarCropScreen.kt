package com.reading.my.ui.imagecrop

import androidx.compose.runtime.Composable

/**
 * 头像裁剪页（薄包装层）
 *
 * 复用通用 [CropScreen]，仅传入头像专属参数：
 * - 1:1 正方形裁剪
 * - 圆形裁剪框预览
 * - 输出 512px 以内的 Base64
 */
@Composable
fun AvatarCropScreen(
    imageUri: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    CropScreen(
        imageUri    = imageUri,
        config      = CropConfig.AvatarSquare,
        title       = "裁剪头像",
        confirmText = "使用",
        hintText    = "拖动调整头像位置",
        isCircle    = true,
        maxDimension = 512,
        onConfirm   = onConfirm,
        onDismiss   = onDismiss
    )
}
