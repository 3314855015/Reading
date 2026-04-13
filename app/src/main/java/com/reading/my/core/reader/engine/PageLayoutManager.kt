package com.reading.my.core.reader.engine

import android.util.Log
import com.reading.my.core.reader.domain.ChapterPages
import com.reading.my.core.reader.domain.Page
import com.reading.my.core.reader.domain.PageLayoutConfig

/**
 * 分页引擎（Phase 1 核心组件）
 *
 * 职责：将章节纯文本按照排版参数切分为一系列 [Page]。
 *
 * 算法概要：
 * 1. 按 `\n` 将章节内容拆分为段落列表
 * 2. 根据 [PageLayoutConfig] 计算每行可容纳字符数、每页可容纳行数
 * 3. 逐段填充页面，满一页则生成一个 Page 对象，超长段落跨页拆分
 * 4. 输出 [ChapterPages]（包含该章所有页的有序列表）
 */
class PageLayoutManager(private val config: PageLayoutConfig) {

    private val TAG = "PageLayoutManager"

    /** 每行可容纳的中文字符数 */
    val charsPerLine: Int get() = config.charsPerLine

    /** 每页可容纳的行数 */
    val linesPerPage: Int get() = config.linesPerPage

    /** 首行实际可用字符数（扣除缩进占用） */
    private val effectiveCharsFirstLine: Int
        get() = (config.charsPerLine - config.firstLineIndentChars).coerceAtLeast(5)

    /**
     * 分页过程中的可变状态
     */
    private data class PaginationState(
        var pageIndex: Int = 0,
        var globalCharOffset: Int = 0,
        var currentLinesUsed: Int = 0,
        var currentPageStartChar: Int = 0,
        val pageTextBuilder: StringBuilder = StringBuilder(),
    ) {
        fun freeLines(totalLines: Int): Int = totalLines - currentLinesUsed

        fun resetForNewPage(newStartChar: Int) {
            currentPageStartChar = newStartChar
            currentLinesUsed = 0
            pageTextBuilder.clear()
        }
    }

    // ==================== 公开 API ====================

    /**
     * 对单个章节执行分页
     */
    fun paginateChapter(chapterIndex: Int, content: String): ChapterPages {
        if (content.isBlank()) return emptyChapter(chapterIndex)

        Log.d(TAG, "分页配置: 屏幕${config.screenWidthPx}x${config.screenHeightPx}px d=${config.density}, " +
                "charsPerLine=$charsPerLine, linesPerPage=$linesPerPage, indent=${config.firstLineIndentChars}字")

        val pages = mutableListOf<Page>()
        val paragraphs = content.split('\n')
        val state = PaginationState()

        for ((paraIndex, paragraph) in paragraphs.withIndex()) {
            if (paragraph.isEmpty()) {
                state.globalCharOffset++
                continue
            }

            // 预估该段落需要的行数（首行因缩进少放几个字符）
            val paraLines = calculateLinesForText(paragraph)
            val spacingNeeded = if (state.currentLinesUsed > 0) 1 else 0

            if (state.currentLinesUsed + paraLines + spacingNeeded > linesPerPage) {
                if (state.currentLinesUsed > 0) {
                    flushPage(pages, chapterIndex, state)
                    state.resetForNewPage(state.globalCharOffset)
                }
            }

            // 写入段落间空行
            if (state.pageTextBuilder.isNotEmpty()) {
                state.pageTextBuilder.append('\n')
                state.currentLinesUsed++
            }

            writeParagraph(paragraph, pages, chapterIndex, state)
        }

        // 最后一页
        if (state.pageTextBuilder.isNotEmpty()) {
            flushPage(pages, chapterIndex, state)
        } else if (pages.isEmpty()) {
            pages.add(Page(chapterIndex, 0, 0, 0, ""))
        }

        Log.d(TAG, "章节$chapterIndex → ${pages.size}页 (${content.length}字)")
        return ChapterPages(chapterIndex = chapterIndex, pages = pages)
    }

    // ==================== 内部实现 ====================

    /**
     * 将一个段落的文本写入当前页，支持跨页拆分。
     * 首行自动预留缩进空间（扣减有效字符数）。
     */
    private fun writeParagraph(
        paragraph: String,
        pages: MutableList<Page>,
        chapterIndex: Int,
        state: PaginationState,
    ) {
        var remaining = paragraph
        var isFirstChunk = true  // 是否为段落的第一块（需要缩进）

        while (remaining.isNotEmpty()) {
            val free = state.freeLines(linesPerPage)
            if (free <= 0) {
                flushPage(pages, chapterIndex, state)
                state.resetForNewPage(state.globalCharOffset)
                isFirstChunk = true
                continue
            }

            // 首行因缩进可用字符更少
            val maxCharsThisLine = if (isFirstChunk && config.firstLineIndentChars > 0)
                minOf(remaining.length, free * effectiveCharsFirstLine)
            else
                minOf(remaining.length, free * charsPerLine)

            val chunk = remaining.substring(0, maxCharsThisLine)

            if (state.pageTextBuilder.isNotEmpty()) state.pageTextBuilder.append('\n')
            state.pageTextBuilder.append(chunk)

            val consumedLines = ceilDiv(chunk.length,
                if (isFirstChunk && config.firstLineIndentChars > 0) effectiveCharsFirstLine else charsPerLine
            )
            state.currentLinesUsed += consumedLines
            state.globalCharOffset += chunk.length

            remaining = remaining.substring(maxCharsThisLine)
            isFirstChunk = false

            if (remaining.isNotEmpty()) {
                flushPage(pages, chapterIndex, state)
                state.resetForNewPage(state.globalCharOffset)
                isFirstChunk = true
            }
        }
    }

    /** 将当前页内容保存为 Page 对象 */
    private fun flushPage(
        pages: MutableList<Page>,
        chapterIndex: Int,
        state: PaginationState,
    ) {
        val pageText = state.pageTextBuilder.toString().trimEnd()
        pages.add(
            Page(
                chapterIndex = chapterIndex,
                pageIndex = state.pageIndex++,
                startCharIndex = state.currentPageStartChar,
                endCharIndex = state.globalCharOffset,
                text = pageText,
            )
        )
    }

    /**
     * 计算一段文本需要的行数（考虑首行缩进）
     */
    internal fun calculateLinesForText(text: String): Int {
        if (text.isEmpty()) return 1
        // 第一行可用字符较少
        val firstLineChars = effectiveCharsFirstLine.coerceAtLeast(text.length)
        val remaining = text.length - firstLineChars
        if (remaining <= 0) return 1
        return 1 + ceilDiv(remaining, charsPerLine)
    }

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

    private fun emptyChapter(chapterIndex: Int): ChapterPages = ChapterPages(
        chapterIndex = chapterIndex,
        pages = listOf(Page(chapterIndex, 0, 0, 0, "")),
    )

    companion object {
        fun createDefault(displayMetrics: android.util.DisplayMetrics): PageLayoutManager {
            val config = PageLayoutConfig.default(
                screenWidthPx = displayMetrics.widthPixels,
                screenHeightPx = displayMetrics.heightPixels,
                density = displayMetrics.density,
            )
            return PageLayoutManager(config)
        }
    }
}
