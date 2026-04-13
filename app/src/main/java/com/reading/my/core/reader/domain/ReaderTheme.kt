package com.reading.my.core.reader.domain

import androidx.compose.ui.graphics.Color

/**
 * 阅读主题配置
 *
 * 定义三套预设主题 + 用户自定义参数，用于渲染引擎和 UI 层统一配色。
 *
 * PRD 规则 R6：支持日间/夜间/护眼三种模式。
 */
data class ReaderTheme(
    /** 主题名称（显示用） */
    val name: String,

    // ===== 背景色系 =====
    /** 页面背景色 */
    val backgroundColor: Color,
    /** 顶部/底部栏背景 */
    val barBackgroundColor: Color,

    // ===== 文字色系 =====
    /** 正文文字颜色 */
    val textColor: Color,
    /** 标题/章节名文字颜色 */
    val titleColor: Color = textColor,
    /** 次要文字（页码、时间等） */
    val secondaryColor: Color = textColor.copy(alpha = 0.5f),

    // ===== 排版参数覆盖（可选，null 时使用 PageLayoutConfig 的默认值） */
    val fontSizeOverride: Float? = null,        // sp，如需主题自带字号
    val lineHeightMultiplierOverride: Float? = null,
) {
    companion object {
        /** 日间模式 — 白底黑字 */
        val DayLight = ReaderTheme(
            name = "日间",
            backgroundColor = Color(0xFFF5F5F0),
            barBackgroundColor = Color(0xFFEEEEEA),
            textColor = Color(0xFF333333),
        )

        /** 夜间模式 — 深灰背景 + 浅色文字（护眼低对比度） */
        val Night = ReaderTheme(
            name = "夜间",
            backgroundColor = Color(0xFF1A1A1A),
            barBackgroundColor = Color(0xFF252525),
            textColor = Color(0xFFCCCCCC),
        )

        /** 护眼模式 — 米黄底 + 深棕字（模拟纸张） */
        val EyeCare = ReaderTheme(
            name = "护眼",
            backgroundColor = Color(0xFFF5F5DC),  // Beige
            barBackgroundColor = Color(0xFFEFEEDD),
            textColor = Color(0xFF4A4A3A),
        )

        /** 所有内置主题列表 */
        val AllPresets: List<ReaderTheme> = listOf(DayLight, Night, EyeCare)
    }
}
