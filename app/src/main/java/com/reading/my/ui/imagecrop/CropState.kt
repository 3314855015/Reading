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
 */
@Stable
data class CropConfig(
    val aspectRatioW: Float = 1f,
    val aspectRatioH: Float = 1f,
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
 * 管理图片的平移，并提供边界限制：
 * - 图片在初始化时自动缩放到恰好填满裁剪框（不可再缩放）
 * - 图片移动范围限制保证裁剪区域内始终有内容，不能露出空白
 *
 * 尺寸模型：
 *   baseFitSize  = 图片用 ContentScale.Fit 填入容器时的显示尺寸
 *   scaledSize   = baseFitSize * scale（当前实际渲染大小，scale 固定）
 */
@Stable
class CropState(
    private val config: CropConfig
) {
    /**
     * 固定缩放系数：初始化时计算一次，保证图片恰好覆盖裁剪框，之后不再改变。
     */
    var scale: Float = 1f
        private set

    /** 平移偏移量（单位：容器像素） */
    var offset: Offset = Offset.Zero
        internal set

    private var containerSize: Size = Size.Zero

    /**
     * 图片原始像素尺寸（供保存时计算裁剪区域使用）
     */
    var imageOriginalSize: Size = Size.Zero
        private set

    /**
     * 图片在容器中以 ContentScale.Fit 显示的基础尺寸（scale=1.0 时的大小）。
     * 这是 AsyncImage 实际绘制到屏幕上的初始大小（供保存时计算裁剪区域使用）。
     */
    var baseFitSize: Size = Size.Zero
        private set

    /**
     * 初始化 / 更新容器和图片尺寸。
     * 计算 ContentScale.Fit 下图片的初始显示尺寸，
     * 再计算让图片覆盖裁剪框所需的最小缩放比（固定 scale）。
     */
    fun initContainer(size: Size, imageSize: Size) {
        containerSize = size
        imageOriginalSize = imageSize

        // ── 1. 计算 ContentScale.Fit 的基础显示尺寸 ──
        // Fit = 等比缩放图片使其完整放入容器，可能有留白
        val containerRatio = size.width / size.height
        val imageRatio = imageSize.width / imageSize.height
        baseFitSize = if (imageRatio > containerRatio) {
            // 图片更宽 → 以容器宽度为准
            Size(size.width, size.width / imageRatio)
        } else {
            // 图片更高 → 以容器高度为准
            Size(size.height * imageRatio, size.height)
        }

        // ── 2. 计算固定 scale（让图片覆盖裁剪框的短边）──
        val cropW = size.width * 0.7f
        val cropH = cropW * (config.aspectRatioH / config.aspectRatioW)
        val scaleX = cropW / baseFitSize.width
        val scaleY = cropH / baseFitSize.height
        scale = maxOf(scaleX, scaleY, 1f)

        // 重置偏移到中心
        offset = Offset.Zero
    }

    /**
     * 处理单指拖动平移。
     * @param pan 本次手势增量（容器像素）
     * @return 夹紧后的最终 offset
     */
    fun onPan(pan: Offset): Offset {
        offset += pan
        clampOffset()
        return offset
    }

    /**
     * 将 offset 限制在合法范围内，保证图片始终覆盖裁剪框，不露出空白。
     *
     * 推导（以 X 轴为例）：
     *   图片左边 = containerCenterX - imgHalfW + offset.x
     *   图片右边 = containerCenterX + imgHalfW + offset.x
     *
     *   要求图片左边 ≤ 裁剪框左边：
     *     offset.x ≤ cropRect.left - containerCenterX + imgHalfW   → maxOffsetX
     *
     *   要求图片右边 ≥ 裁剪框右边：
     *     offset.x ≥ cropRect.right - containerCenterX - imgHalfW  → minOffsetX
     *
     * 边界是固定的几何量，不随 offset 当前值变化。
     */
    fun clampOffset() {
        if (containerSize == Size.Zero || baseFitSize == Size.Zero) return

        val cropRect = getCropRect()
        val currentSize = getScaledImageSize()

        val containerCenterX = containerSize.width / 2f
        val containerCenterY = containerSize.height / 2f
        val imgHalfW = currentSize.width / 2f
        val imgHalfH = currentSize.height / 2f

        val clampedX = if (currentSize.width >= cropRect.width) {
            val minOffsetX = cropRect.right - containerCenterX - imgHalfW
            val maxOffsetX = cropRect.left - containerCenterX + imgHalfW
            offset.x.coerceIn(minOffsetX, maxOffsetX)
        } else {
            // 图片宽度小于裁剪框（正常不应发生），锁定居中
            0f
        }

        val clampedY = if (currentSize.height >= cropRect.height) {
            val minOffsetY = cropRect.bottom - containerCenterY - imgHalfH
            val maxOffsetY = cropRect.top - containerCenterY + imgHalfH
            offset.y.coerceIn(minOffsetY, maxOffsetY)
        } else {
            // 图片高度小于裁剪框（正常不应发生），锁定居中
            0f
        }

        offset = Offset(clampedX, clampedY)
    }

    /**
     * 获取裁剪框在容器坐标系中的矩形（宽度 = 容器宽度的 70%，高度按比例）
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
     * 获取当前 scale 下图片的实际显示尺寸（baseFitSize × scale）
     */
    fun getScaledImageSize(): Size = Size(
        baseFitSize.width * scale,
        baseFitSize.height * scale
    )
}
