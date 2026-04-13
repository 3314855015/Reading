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
 *
 * 使用方式：
 * ```kotlin
 * val config = PageLayoutConfig.default(screenWidth, screenHeight, density)
 * val manager = PageLayoutManager(config)
 * val result = manager.paginateChapter(chapterIndex, chapterContent)
 * // result.pages[0].text → 第一页的文字
 * // result.pageCount → 总页数
 * ```
 */
class PageLayoutManager(private val config: PageLayoutConfig) {

    private val TAG = "PageLayoutManager"

    /** 每行可容纳的中文字符数 */
    val charsPerLine: Int get() = config.charsPerLine

    /** 每页可容纳的行数 */
    val linesPerPage: Int get() = config.linesPerPage

    /**
     * 分页过程中的可变状态
     *
     * 封装在一个对象中传递，避免 Kotlin 无引用传参的问题。
     * 所有需要修改状态的方法接收这个对象即可直接读写。
     */
    private data class PaginationState(
        var pageIndex: Int = 0,
        /** 当前已处理到的全局字符位置（content 中的绝对偏移） */
        var globalCharOffset: Int = 0,
        /** 当前页已使用的行数 */
        var currentLinesUsed: Int = 0,
        /** 当前页起始字符在 content 中的位置 */
        var currentPageStartChar: Int = 0,
        /** 当前页文字内容构建器 */
        val pageTextBuilder: StringBuilder = StringBuilder(),
    ) {
        /** 当前页还剩多少空行 */
        fun freeLines(totalLines: Int): Int = totalLines - currentLinesUsed

        /** 重置为新的一页 */
        fun resetForNewPage(newStartChar: Int) {
            currentPageStartChar = newStartChar
            currentLinesUsed = 0
            pageTextBuilder.clear()
        }
    }

    // ==================== 公开 API ====================

    /**
     * 对单个章节执行分页
     *
     * @param chapterIndex 章节索引（从0开始）
     * @param content      章节完整纯文本（来自 Chapter.content）
     * @return 该章的分页结果
     */
    fun paginateChapter(chapterIndex: Int, content: String): ChapterPages {
        if (content.isBlank()) return emptyChapter(chapterIndex)

        val pages = mutableListOf<Page>()
        val paragraphs = content.split('\n')
        val state = PaginationState()

        for (paragraph in paragraphs) {
            if (paragraph.isEmpty()) {
                state.globalCharOffset++ // 跳过 '\n'
                continue
            }

            // 检查当前页是否放得下这个段落
            val paraLines = calculateLinesForText(paragraph)
            val spacingNeeded = if (state.currentLinesUsed > 0) 1 else 0

            if (state.currentLinesUsed + paraLines + spacingNeeded > linesPerPage) {
                if (state.currentLinesUsed > 0) {
                    flushPage(pages, chapterIndex, state)
                    state.resetForNewPage(state.globalCharOffset)
                }
            }

            // 写入段落间换行
            if (state.pageTextBuilder.isNotEmpty()) {
                state.pageTextBuilder.append('\n')
                state.currentLinesUsed++
            }

            // 写入段落（可能跨页拆分）
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
     * 将一个段落的文本写入当前页
     *
     * 支持跨页拆分：当段落太长无法放入当前页剩余空间时，
     * 会自动截断并保存当前页，剩余部分继续写入下一页。
     */
    private fun writeParagraph(
        paragraph: String,
        pages: MutableList<Page>,
        chapterIndex: Int,
        state: PaginationState,
    ) {
        var remaining = paragraph
        while (remaining.isNotEmpty()) {
            val free = state.freeLines(linesPerPage)
            if (free <= 0) {
                // 当前页满 → 存档开新页
                flushPage(pages, chapterIndex, state)
                state.resetForNewPage(state.globalCharOffset)
                continue
            }
            // 本次最多写入 free * charsPerLine 个字符
            val maxChars = minOf(remaining.length, free * charsPerLine)
            val chunk = remaining.substring(0, maxChars)

            if (state.pageTextBuilder.isNotEmpty()) state.pageTextBuilder.append('\n')
            state.pageTextBuilder.append(chunk)

            val linesConsumed = ceilDiv(chunk.length, charsPerLine)
            state.currentLinesUsed += linesConsumed
            state.globalCharOffset += chunk.length

            remaining = remaining.substring(maxChars)

            if (remaining.isNotEmpty()) {
                // 还有剩余 → 页满了，存档继续
                flushPage(pages, chapterIndex, state)
                state.resetForNewPage(state.globalCharOffset)
            }
        }
    }

    /** 将当前页内容保存为 Page 对象，加入列表 */
    private fun flushPage(
        pages: MutableList<Page>,
        chapterIndex: Int,
        state: PaginationState,
    ) {
        pages.add(
            Page(
                chapterIndex = chapterIndex,
                pageIndex = state.pageIndex++,
                startCharIndex = state.currentPageStartChar,
                endCharIndex = state.globalCharOffset,
                text = state.pageTextBuilder.toString().trimEnd()
            )
        )
    }

    /** 计算一段文本需要的行数 */
    internal fun calculateLinesForText(text: String): Int =
        if (text.isEmpty()) 1 else ceilDiv(text.length, charsPerLine).coerceAtLeast(1)

    /** 向上取整除法 */
    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

    /** 空章节的分页结果 */
    private fun emptyChapter(chapterIndex: Int): ChapterPages = ChapterPages(
        chapterIndex = chapterIndex,
        pages = listOf(Page(chapterIndex, 0, 0, 0, ""))
    )

    companion object {
        /**
         * 基于屏幕参数创建默认配置的分页管理器
         *
         * @param displayMetrics DisplayMetrics（从 Activity 或 Resources 获取）
         */
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
