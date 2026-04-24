package com.reading.my.ui.imagecrop

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

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
 * 管理图片的缩放和平移，并提供边界限制：
 * - 图片在初始化时自动缩放到恰好填满裁剪框
 * - 支持双指缩放 + 单指平移
 * - 图片移动/缩放范围限制保证裁剪区域内始终有内容，不露出空白
 *
 * 尺寸模型：
 *   baseFitSize  = 图片用 ContentScale.Fit 填入容器时的显示尺寸（scale=1.0）
 *   scaledSize   = baseFitSize × scale（当前实际渲染大小）
 */
@Stable
class CropState(
    private val config: CropConfig
) {
    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 4f
    }

    /** 当前缩放系数（Compose 可观察） */
    val scale: MutableState<Float> = mutableStateOf(1f)

    /** 平移偏移量（单位：容器像素，Compose 可观察） */
    val offset: MutableState<Offset> = mutableStateOf(Offset.Zero)

    private var containerSize: Size = Size.Zero

    /**
     * 图片原始像素尺寸（供保存时计算裁剪区域使用）
     */
    var imageOriginalSize: Size = Size.Zero
        private set

    /**
     * 图片在容器中以 ContentScale.Fit 显示的基础尺寸（scale=1.0 时的大小）。
     * 这是 AsyncImage 实际绘制到屏幕上的初始大小。
     */
    var baseFitSize: Size = Size.Zero
        private set

    /** 初始化时的最小 scale（恰好覆盖裁剪框），作为缩放下限 */
    private var initialMinScale = 1f

    /**
     * 初始化 / 更新容器和图片尺寸。
     */
    fun initContainer(size: Size, imageSize: Size) {
        containerSize = size
        imageOriginalSize = imageSize

        // ── 1. 计算 ContentScale.Fit 的基础显示尺寸 ──
        val containerRatio = size.width / size.height
        val imageRatio = imageSize.width / imageSize.height
        baseFitSize = if (imageRatio > containerRatio) {
            Size(size.width, size.width / imageRatio)
        } else {
            Size(size.height * imageRatio, size.height)
        }

        // ── 2. 计算初始最小 scale（让图片覆盖裁剪框的短边）──
        val cropW = size.width * 0.7f
        val cropH = cropW * (config.aspectRatioH / config.aspectRatioW)
        val scaleX = cropW / baseFitSize.width
        val scaleY = cropH / baseFitSize.height
        initialMinScale = maxOf(maxOf(scaleX, scaleY), MIN_SCALE)

        // 重置状态
        scale.value = initialMinScale
        offset.value = Offset.Zero

        Log.d("CropState", "=== INIT === container=${size.width.toInt()}x${size.height.toInt()} " +
                "original=${imageSize.width.toInt()}x${imageSize.height.toInt()} " +
                "baseFit=${baseFitSize.width.toInt()}x${baseFitSize.height.toInt()} " +
                "scale=${scale.value} minScale=$initialMinScale " +
                "cropRect=${cropW.toInt()}x${cropH.toInt()}")
    }

    /**
     * 处理双指缩放。
     * @param zoom 缩放因子（>1 放大，<1 缩小）
     * @param centroid 缩放中心点（容器坐标），用于保持中心不变
     */
    fun onZoom(zoom: Float, centroid: Offset) {
        val newScale = (scale.value * zoom).coerceIn(initialMinScale, MAX_SCALE)
        if (newScale == scale.value) return

        val oldScale = scale.value
        scale.value = newScale

        // 以缩放中心为锚点调整 offset：让 centroid 对应的图像位置不变
        val scaleChange = newScale / oldScale
        offset.value = centroid + (offset.value - centroid) * scaleChange

        clampOffset()
    }

    /**
     * 处理单指拖动平移。
     */
    fun onPan(pan: Offset): Offset {
        offset.value += pan
        clampOffset()
        return offset.value
    }

    /**
     * 将 offset 限制在合法范围内，保证图片始终覆盖裁剪框，不露出空白。
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
            offset.value.x.coerceIn(minOffsetX, maxOffsetX)
        } else { 0f }

        val clampedY = if (currentSize.height >= cropRect.height) {
            val minOffsetY = cropRect.bottom - containerCenterY - imgHalfH
            val maxOffsetY = cropRect.top - containerCenterY + imgHalfH
            offset.value.y.coerceIn(minOffsetY, maxOffsetY)
        } else { 0f }

        offset.value = Offset(clampedX, clampedY)
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
        baseFitSize.width * scale.value,
        baseFitSize.height * scale.value
    )
}
