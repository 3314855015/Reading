package com.reading.my.core.reader.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import com.reading.my.core.reader.domain.Page
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme

/**
 * 渲染单页内容为 Composable（带 L1 内存缓存）
 *
 * 首次渲染时将文本绘制到离屏 Bitmap 并缓存（RenderCache），
 * 后续翻页/重组时直接从缓存读取 Bitmap 绘制，避免重复的 Paint.breakText + drawText。
 *
 * 缓存失效条件：
 * - 排版参数变化（字号/行高/边距/间距/主题色）
 * - 切换书籍
 */
@Composable
fun BookPageRenderer(
    bookId: String,
    page: Page,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    modifier: Modifier = Modifier,
    showPageNumber: Boolean = false,
) {
    // Preview 模式下不使用缓存，确保 Android Studio 预览正常显示
    if (LocalInspectionMode.current) {
        UncachedBookPageRenderer(page, config, theme, modifier, showPageNumber)
        return
    }

    val bitmap = RenderCache.getOrRender(
        bookId = bookId,
        chapterIndex = page.chapterIndex,
        pageIndex = page.pageIndex,
        page = page,
        config = config,
        theme = theme,
    ) {
        renderPageToBitmap(page, config, theme, showPageNumber)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = theme.backgroundColor)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "第 ${page.pageIndex + 1} 页",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 无缓存版本 —— 用于 Preview 或特殊场景
 */
@Composable
private fun UncachedBookPageRenderer(
    page: Page,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    modifier: Modifier = Modifier,
    showPageNumber: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = theme.backgroundColor)
            .drawBehind {
                TextRender.renderPage(this, page, config, theme)

                if (showPageNumber) {
                    val pageNumPaint = android.graphics.Paint().apply {
                        color = TextRender.colorToArgb(theme.secondaryColor)
                        textSize = 12f * config.density
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${page.pageIndex + 1}",
                        size.width - config.horizontalPaddingPx,
                        size.height - config.verticalPaddingPx / 2,
                        pageNumPaint,
                    )
                }
            },
    )
}

/**
 * 将一页文本渲染到离屏 Bitmap
 */
private fun renderPageToBitmap(
    page: Page,
    config: PageLayoutConfig,
    theme: ReaderTheme,
    showPageNumber: Boolean,
): Bitmap {
    val width = config.screenWidthPx
    val height = config.screenHeightPx

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 绘制背景色
    canvas.drawColor(TextRender.colorToArgb(theme.backgroundColor))

    // 使用轻量 DrawScope 代理调用 TextRender
    val drawScope = SimpleDrawScope(width.toFloat(), height.toFloat(), canvas)
    TextRender.renderPage(drawScope, page, config, theme)

    // 可选：绘制页码
    if (showPageNumber) {
        val pageNumPaint = android.graphics.Paint().apply {
            color = TextRender.colorToArgb(theme.secondaryColor)
            textSize = 12f * config.density
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        canvas.drawText(
            "${page.pageIndex + 1}",
            width - config.horizontalPaddingPx,
            height - config.verticalPaddingPx / 2,
            pageNumPaint,
        )
    }

    return bitmap
}

/** 极简 DrawScope 实现 —— 仅暴露 TextRender 所需的最小接口 */
private class SimpleDrawScope(
    override val size: androidx.compose.ui.geometry.Size,
    private val canvas: Canvas,
) : androidx.compose.ui.graphics.drawscope.DrawScope {

    override fun drawContext(): androidx.compose.ui.graphics.drawscope.DrawContext =
        object : androidx.compose.ui.graphics.drawscope.DrawContext {
            override val canvas: androidx.compose.ui.graphics.Canvas
                get() = this@SimpleDrawScope.canvas.asComposeCanvas()
            override val size: androidx.compose.ui.geometry.Size
                get() = this@SimpleDrawScope.size
        }
}

/** 将 Android Canvas 包装为 Compose Canvas */
private fun Canvas.asComposeCanvas(): androidx.compose.ui.graphics.Canvas =
    androidx.compose.ui.graphics.AndroidCanvas(this)
