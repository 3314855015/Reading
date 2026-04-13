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
     *
     * currentLinesUsedF 使用浮点数精确追踪已用行高（含段落间距的小数部分），
     * 避免逐项向上取整导致的累积误差。只有文本行占用整数行，空行间距保留小数。
     */
    private data class PaginationState(
        var pageIndex: Int = 0,
        var globalCharOffset: Int = 0,
        var currentLinesUsedF: Float = 0f,   // 浮点数：精确追踪像素预算
        var currentPageStartChar: Int = 0,
        val pageTextBuilder: StringBuilder = StringBuilder(),
        /** 当前页是否为跨页续接（上一页某段落未完，本页继续） */
        var isContinuation: Boolean = false,
    ) {
        /** 剩余可用行数 */
        fun freeLines(totalLines: Int): Float = totalLines - currentLinesUsedF

        fun resetForNewPage(newStartChar: Int) {
            currentPageStartChar = newStartChar
            currentLinesUsedF = 0f
            pageTextBuilder.clear()
        }
    }

    // ==================== 公开 API ====================

    /**
     * 对单个章节执行分页
     */
    fun paginateChapter(chapterIndex: Int, content: String): ChapterPages {
        if (content.isBlank()) return emptyChapter(chapterIndex)

        Log.d(TAG, "=== 分页配置 === 屏幕${config.screenWidthPx}x${config.screenHeightPx}px d=${config.density}")
        Log.d(TAG, "  边距=${config.horizontalPaddingDp}dp(左右)x${config.verticalPaddingDp}dp(上下) → ${config.horizontalPaddingPx.toInt()}x${config.verticalPaddingPx.toInt()}px")
        Log.d(TAG, "  字号=${config.fontSizeSp}sp → ${config.fontSizePx.toInt()}px")
        Log.d(TAG, "  行高=${config.lineHeightMultiplier}x → ${config.lineHeightPx.toInt()}px")
        Log.d(TAG, "  内容区域: ${config.contentWidthPx.toInt()}x${config.contentHeightPx.toInt()}px")
        Log.d(TAG, "  charsPerLine=$charsPerLine, linesPerPage=$linesPerPage")
        Log.d(TAG, "  理论最大行数(内容高度/行高)= ${(config.contentHeightPx / config.lineHeightPx).toInt()}")
        Log.d(TAG, "  首行缩进=${config.firstLineIndentChars}字=${config.firstLineIndentPx.toInt()}px")

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
            // 段落间距：使用精确浮点成本，与渲染端完全一致
            val spacingCost = config.blankLineCostExact

            if (state.currentLinesUsedF + paraLines + spacingCost > linesPerPage) {
                if (state.currentLinesUsedF > 0) {
                    flushPage(pages, chapterIndex, state)
                    state.resetForNewPage(state.globalCharOffset)
                }
            }

            // 写入段落间空行（使用精确的浮点像素成本，与渲染端一致）
            if (state.pageTextBuilder.isNotEmpty()) {
                state.pageTextBuilder.append('\n')
                state.currentLinesUsedF += config.blankLineCostExact
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

        Log.d(TAG, "  [writePara] 段落${paragraph.length}字, 当前页已用${String.format("%.1f", state.currentLinesUsedF)}/${linesPerPage}行")

        while (remaining.isNotEmpty()) {
            val free = state.freeLines(linesPerPage)
            if (free <= 0) {
                Log.d(TAG, "    → 页满(free=${String.format("%.1f", free)}<=0), flush p${state.pageIndex}")
                flushPage(pages, chapterIndex, state)
                state.resetForNewPage(state.globalCharOffset)
                // 页满但段落未完 → 下页是续接页
                state.isContinuation = true
                isFirstChunk = false  // 续接页首行不缩进！
                continue
            }

            // 首行因缩进可用字符更少（仅当是段落的真正第一块时）
            // free 是浮点数，向下取整为整数行来计算能放多少字
            val freeInt = kotlin.math.floor(free.toDouble()).toInt().coerceAtLeast(0)
            val maxCharsThisLine = if (isFirstChunk && config.firstLineIndentChars > 0)
                minOf(remaining.length, freeInt * effectiveCharsFirstLine)
            else
                minOf(remaining.length, freeInt * charsPerLine)

            val chunk = remaining.substring(0, maxCharsThisLine)

            if (state.pageTextBuilder.isNotEmpty()) state.pageTextBuilder.append('\n')
            state.pageTextBuilder.append(chunk)

            // 正确计算 chunk 的实际占用行数：
            //   - 如果是首行有缩进的块：第一行用 effectiveCharsFirstLine，后续用 charsPerLine
            //   - 否则全部用 charsPerLine
            val effectiveCpl = if (isFirstChunk && config.firstLineIndentChars > 0)
                effectiveCharsFirstLine else charsPerLine
            val consumedLines = if (chunk.length <= effectiveCpl) {
                1
            } else {
                1 + ceilDiv(chunk.length - effectiveCpl, charsPerLine)
            }
            
            Log.d(TAG, "    写入${chunk.length}字(消耗${consumedLines}行, 首块=$isFirstChunk), 剩余${String.format("%.1f", linesPerPage - state.currentLinesUsedF - consumedLines)}行")
            
            state.currentLinesUsedF += consumedLines.toFloat()
            state.globalCharOffset += chunk.length

            remaining = remaining.substring(maxCharsThisLine)
            isFirstChunk = false

            if (remaining.isNotEmpty()) {
                // 段落未完 → 存档，下页继续
                Log.d(TAG, "    → 段落未完(剩${remaining.length}字), flush p${state.pageIndex}, 标记续接")
                flushPage(pages, chapterIndex, state)
                state.resetForNewPage(state.globalCharOffset)
                state.isContinuation = true   // 标记为续接页
                isFirstChunk = false          // 续接不缩进
            } else {
                // 段落写完了 → 如果后面还有内容写入此页，不再算续接
                // 但如果本页后续又写了新段落，新段落的首块应该有缩进（由外层循环控制 isFirstChunk=true）
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
        val actualLines = pageText.split('\n').size
        Log.d(TAG, "  [flush p${state.pageIndex}] 续接=${state.isContinuation}, " +
                "用了${String.format("%.1f", state.currentLinesUsedF)}行(估算), 文本${pageText.length}字(${actualLines}实际行), " +
                "剩余可容纳=${String.format("%.1f", linesPerPage - state.currentLinesUsedF)}行")
        pages.add(
            Page(
                chapterIndex = chapterIndex,
                pageIndex = state.pageIndex++,
                startCharIndex = state.currentPageStartChar,
                endCharIndex = state.globalCharOffset,
                text = pageText,
                isContinuation = state.isContinuation,
            )
        )
        // flush 后重置续接标记：下一页默认不是续接页
        state.isContinuation = false
    }

    /**
     * 计算一段文本需要的行数（考虑首行缩进）
     */
    internal fun calculateLinesForText(text: String): Int {
        if (text.isEmpty()) return 1
        // 第一行可用字符较少（受缩进限制）
        val firstLineChars = effectiveCharsFirstLine.coerceAtLeast(text.length)
        if (text.length <= firstLineChars) return 1
        // 后续行用完整容量
        val remaining = text.length - firstLineChars
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
