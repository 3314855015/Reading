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

    /** 行高倍数（相对于字号，如 1.6 表示行高 = 字号 × 1.6） */
    val lineHeightMultiplier: Float,

    /** 左右边距（dp） */
    val horizontalPaddingDp: Float,

    /** 上下边距（dp） */
    val verticalPaddingDp: Float,

    /** 首行缩进字符数（0=不缩进，通常为2表示缩进两个中文字符宽度） */
    val firstLineIndentChars: Int,

    /**
     * 段落间空行的间距比例（相对于行高）
     *
     * 0.0 = 无空行间距（段落紧挨）
     * 0.5 = 空行占半行高
     * 1.0 = 空行占一整行高度（当前值，类似书籍排版）
     */
    val blankLineSpacingRatio: Float,

    /**
     * 段落后额外推进的比例（相对于行高）
     *
     * 在空行间距之外，每个段落的末尾再增加一点空间。
     * 与 blankLineSpacingRatio 叠加构成完整的段落间距。
     * 建议值：0.3~0.5
     */
    val paraEndSpacingRatio: Float,

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
            density: Float = 2.0f,
        ): PageLayoutConfig = PageLayoutConfig(
            fontSizeSp = 18f,
            lineHeightMultiplier = 1.6f,
            horizontalPaddingDp = 18f,
            verticalPaddingDp = 20f,
            firstLineIndentChars = 2,
            blankLineSpacingRatio = 0.5f,    // 段落空行 ≈ 半行高度（紧凑小说风格）
            paraEndSpacingRatio = 0.2f,      // 段后微调间距（轻微）
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

    /**
     * 段落间距总成本（以"行高"为单位，供分页端使用）
     *
     * 分页端需要保守估算：宁可多留空也不要丢字。
     * 总成本 = 空行间距 + 段后推进，向上取整为整行。
     */
    val blankLineCostLines: Int
        get() = kotlin.math.ceil(
            (blankLineSpacingRatio + paraEndSpacingRatio).toDouble()
        ).toInt()

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
     * 计算公式：(可用高度) / (行高)
     * 向下取整确保不溢出。
     *
     * 注意：段落间空行在渲染端只占 0.4x 行高（不是整行），
     * 因此这里不需要额外预留段落间距空间，直接用可用高度除以行高即可。
     */
    val linesPerPage: Int
        get() = ((contentHeightPx) / lineHeightPx).toInt().coerceAtLeast(1)

    /**
     * 每行可容纳的中文字符数（精确估算）
     *
     * 通过渲染端 Paint.breakText 实测反算：
     *   - 全宽(981px)实际容纳约 20 个中文字 → 单字宽度 ≈ 49px = fontSizePx × 1.0
     *   - 缩进后(896px)实际容纳约 17~18 个中文字
     *
     * 中文字符在默认字体下的实际宽度几乎等于 fontSize（ratio ≈ 1.0），
     * 此前使用 0.85f 导致分页高估每行容量，造成严重底部留白。
     * 使用 0.97f 留微小余量，确保分页不溢出。
     */
    val charsPerLine: Int
        get() = ((contentWidthPx / (fontSizePx * 0.97f)).toInt()).coerceAtLeast(10)

    /** 首行缩进占用的像素宽度（使用与 charsPerLine 相同的字符宽度比例） */
    val firstLineIndentPx: Float get() = firstLineIndentChars * fontSizePx * 0.97f

    /**
     * 段落间距像素成本（供分页端精确预算）
     *
     * 返回值以"行高"为单位的小数，不再取整为整数行。
     * 分页端应使用浮点累加来跟踪已用空间，避免逐项向上取整导致的累积误差。
     *
     * 实际渲染消耗 = blankLineSpacingRatio + paraEndSpacingRatio（单位：行高）
     */
    val blankLineCostExact: Float
        get() = blankLineSpacingRatio + paraEndSpacingRatio
}
