package com.reading.my.core.reader.domain

import android.util.DisplayMetrics

/**
 * 排版参数配置
 *
 * 定义了影响分页计算的所有可调参数。
 * 修改这些值会改变每页能容纳的文字数量，从而影响总页数。
 *
 * 使用方式：
 * ```kotlin
 * // 默认配置
 * val config = PageLayoutConfig.default(screenWidthDp, screenHeightDp)
 *
 * // 自定义配置（用户调节字号后）
 * val custom = config.copy(fontSizeSp = 20, lineHeightMultiplier = 1.6f)
 * ```
 */
data class PageLayoutConfig(
    /** 字号（单位：sp） */
    val fontSizeSp: Float,

    /** 行高倍数（相对于字号，如 1.8 表示行高 = 字号 × 1.8） */
    val lineHeightMultiplier: Float,

    /** 段落间距（行高的倍数，段落之间额外留白） */
    val paragraphSpacingMultiplier: Float,

    /** 左右边距（dp） */
    val horizontalPaddingDp: Float,

    /** 上下边距（dp） */
    val verticalPaddingDp: Float,

    /** 屏幕可用宽度（px） */
    val screenWidthPx: Int,

    /** 屏幕可用高度（px） */
    val screenHeightPx: Int,

    /** 文字密度（DisplayMetrics.density），用于 dp→px 转换 */
    val density: Float,
) {
    companion object {
        /**
         * 创建默认排版配置
         *
         * @param screenWidthPx  屏幕宽度（px）
         * @param screenHeightPx 屏幕高度（px）
         * @param density        DisplayMetrics.density
         */
        fun default(
            screenWidthPx: Int,
            screenHeightPx: Int,
            density: Float = 2.0f, // 默认按 xxhdpi 估算
        ): PageLayoutConfig = PageLayoutConfig(
            fontSizeSp = 18f,
            lineHeightMultiplier = 1.8f,
            paragraphSpacingMultiplier = 0.5f,
            horizontalPaddingDp = 16f,
            verticalPaddingDp = 32f,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            density = density,
        )
    }

    // ==================== 派生计算属性 ====================

    /** 字号转 px */
    val fontSizePx: Float get() = fontSizeSp * density

    /** 单行行高（px）= 字号 × 行高倍数 */
    val lineHeightPx: Float get() = fontSizePx * lineHeightMultiplier

    /** 段落额外间距（px）= 行高 × 段落间距倍数 */
    val paragraphSpacingPx: Float get() = lineHeightPx * paragraphSpacingMultiplier

    /** 左右边距 px */
    val horizontalPaddingPx: Float get() = horizontalPaddingDp * density

    /** 上下边距 px */
    val verticalPaddingPx: Float get() = verticalPaddingDp * density

    /**
     * 文字区域有效宽度（px）
     *
     * 即屏幕宽度减去左右边距后的实际文字绘制宽度。
     * 用于估算每行可容纳多少个字符。
     */
    val contentWidthPx: Float
        get() = screenWidthPx - 2 * horizontalPaddingPx

    /**
     * 文字区域有效高度（px）
     *
     * 即屏幕高度减去上下边距后的实际文字绘制高度。
     */
    val contentHeightPx: Float
        get() = screenHeightPx - 2 * verticalPaddingPx

    /**
     * 每页可容纳的行数
     *
     * 计算公式：(可用高度) / (行高 + 段落间距)
     * 向下取整确保不溢出。
     */
    val linesPerPage: Int
        get() = ((contentHeightPx) / lineHeightPx).toInt().coerceAtLeast(1)

    /**
     * 估算每行可容纳的中文字符数
     *
     * 中文字符在大多数字体中近似等宽，
     * 粗略估算：字符宽度 ≈ 字号（对中文宋体/黑体误差 <10%）。
     * 后续渲染引擎若使用 Paint.measureText() 可得到精确值。
     */
    val charsPerLine: Int
        get() = (contentWidthPx / fontSizePx).toInt().coerceAtLeast(10)
}
