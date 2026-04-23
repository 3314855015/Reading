package com.reading.my.ui.imagecrop

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect

/**
 * 裁剪配置（不可变，由调用方定义）
 *
 * @param aspectRatioW 宽比例（如 1:1 → w=1, 3:4 → w=3）
 * @param aspectRatioH 高比例（如 1:1 → h=1, 3:4 → h=4）
 * @param minScale 最小缩放倍数（图片至少放大到填满裁剪框）
 */
@Stable
data class CropConfig(
    val aspectRatioW: Float = 1f,
    val aspectRatioH: Float = 1f,
    val minScale: Float = 1f,
    val maxScale: Float = 5f,
) {
    /** 比例值，如 1:1 → 1.0, 3:4 → 0.75 */
    val aspectRatio: Float get() = aspectRatioW / aspectRatioH

    companion object {
        /** 头像默认：正方形 1:1 */
        val AvatarSquare = CropConfig(aspectRatioW = 1f, aspectRatioH = 1f)

        /** 封面默认：竖版 3:4 */
        val CoverPortrait = CropConfig(aspectRatioW = 3f, aspectRatioH = 4f)
    }
}

/**
 * 裁剪交互状态（可变）
 *
 * 管理图片的缩放、平移，并提供边界限制：
 * - 图片不能缩小到裁剪框之外（保证裁剪区域始终有内容）
 * - 图片移动范围限制在裁剪框内（不能拖出空白）
 */
@Stable
class CropState(
    private val config: CropConfig
) {
    var scale: Float = 1f
        internal set

    var offset: Offset = Offset.Zero
        internal set

    /** 当前画布尺寸（由外部传入，用于计算裁剪框位置） */
    private var containerSize: Size = Size.Zero

    /**
     * 初始化 / 更新容器尺寸时调用。
     * 自动计算让图片填满裁剪框所需的初始 scale 和居中 offset。
     */
    fun initContainer(size: Size, imageSize: Size) {
        containerSize = size

        // 计算裁剪框尺寸（占容器的 70% 宽度）
        val cropW = size.width * 0.7f
        val cropH = cropW * (config.aspectRatioH / config.aspectRatioW)

        // 计算图片初始 scale：让图片短边刚好覆盖裁剪框对应边
        val scaleX = cropW / imageSize.width
        val scaleY = cropH / imageSize.height
        scale = maxOf(scaleX, scaleY, config.minScale)

        // 居中
        offset = Offset.Zero
    }

    /**
     * 处理双指缩放手势
     * @return 实际生效的 scale（被 clamp 后的值）
     */
    fun onZoom(zoomFactor: Float): Float {
        val newScale = (scale * zoomFactor).coerceIn(config.minScale, config.maxScale)
        scale = newScale

        // 缩放后重新限制位移，防止图片被拖出裁剪框
        clampOffset()

        return newScale
    }

    /**
     * 处理单指平移手势
     * @return 实际生效的 offset（被 clamp 后的值）
     */
    fun onPan(pan: Offset): Offset {
        offset += pan
        clampOffset()
        return offset
    }

    /**
     * 核心方法：将 offset 限制在合法范围内
     *
     * 合法范围 = 图片经过缩放后的边缘不能进入裁剪框内部
     * 即：图片必须完全覆盖裁剪框区域（允许超出）
     */
    fun clampOffset() {
        if (containerSize == Size.Zero) return

        val cropRect = getCropRect()
        val scaledImageSize = getScaledImageSize()

        // 图片在容器中的实际可见范围
        // 图片中心 = 容器中心 + offset
        // 图片左边缘 = centerX - scaledWidth/2 + offsetX
        // 图片右边缘 = centerX + scaledWidth/2 + offsetX
        // （上下同理）

        val imgHalfW = scaledImageSize.width / 2f
        val imgHalfH = scaledImageSize.height / 2f

        // 允许的最大偏移量：图片边缘刚好贴到裁剪框对边
        // 左方向最大偏移 = 图片右边缘 - 裁剪框右边缘
        // 但更直观的方式是：
        //   offset.x 的范围使得图片左边缘 ≤ 裁剪框左边缘 且 图片右边缘 ≥ 裁剪框右边缘

        val containerCenterX = containerSize.width / 2f
        val containerCenterY = containerSize.height / 2f

        // 图片四边位置（相对于容器左上角）
        val imgLeft = containerCenterX - imgHalfW + offset.x
        val imgRight = containerCenterX + imgHalfW + offset.x
        val imgTop = containerCenterY - imgHalfH + offset.y
        val imgBottom = containerCenterY + imgHalfH + offset.y

        // 限制：图片不能露出裁剪框内的空白
        // 即：imgLeft ≤ cropLeft, imgRight ≥ cropRight（水平方向）
        //     imgTop ≤ cropTop,   imgBottom ≥ cropBottom（垂直方向）

        var clampedX = offset.x
        var clampedY = offset.y

        if (scaledImageSize.width >= cropRect.width) {
            // 图片宽度足够覆盖裁剪框 → 限制左右不越界
            // 左边界：图片左边不能超过裁剪框左边
            val maxLeftShift = cropRect.left - imgLeft
            // 右边界：图片右边不能超过裁剪框右边
            val maxRightShift = cropRect.right - imgRight
            clampedX = offset.x.coerceIn(maxRightShift, maxLeftShift)
        } else {
            // 图片宽度不足 → 强制居中（不应该发生，因为 minScale 保证覆盖）
            clampedX = 0f
        }

        if (scaledImageSize.height >= cropRect.height) {
            val maxTopShift = cropRect.top - imgTop
            val maxBottomShift = cropRect.bottom - imgBottom
            clampedY = offset.y.coerceIn(maxBottomShift, maxTopShift)
        } else {
            clampedY = 0f
        }

        offset = Offset(clampedX, clampedY)
    }

    /**
     * 获取裁剪框在容器坐标系中的矩形
     */
    fun getCropRect(): Rect {
        if (containerSize == Size.Zero) return Rect.Zero

        val cropW = containerSize.width * 0.7f
        val cropH = cropW * (config.aspectRatioH / config.aspectRatioW)
        val left = (containerSize.width - cropW) / 2f
        val top = (containerSize.height - cropH) / 2f

        return Rect(left, top, left + cropW, top + cropH)
    }

    /**
     * 获取缩放后图片的实际显示尺寸
     */
    fun getScaledImageSize(): Size {
        // 注意：这里假设原始图片是 fit 到容器的
        // 实际尺寸需要在知道原始图片尺寸后才能准确计算
        // 此处返回基于容器和 scale 的近似值
        return Size(containerSize.width * scale, containerSize.height * scale)
    }
}
