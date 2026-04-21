package com.reading.my.core.reader.engine

import com.reading.my.core.reader.domain.PageLayoutConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 分页引擎单元测试
 *
 * 测试覆盖：
 * 1. 短文本单页场景
 * 2. 长文本多页拆分
 * 3. 段落边界处理（不在段落中间断页）
 * 4. 超长段落的跨页拆分
 * 5. 空内容/空白章节的容错
 * 6. 字符位置索引的准确性
 */
class PageLayoutManagerTest {

    /** 模拟 1080×2340 屏幕, density=2.75 (xxhdpi+) 的排版配置 */
    private lateinit var manager: PageLayoutManager

    @Before
    fun setup() {
        // 模拟常见手机屏幕参数
        val config = PageLayoutConfig(
            fontSizeSp = 18f,
            lineHeightMultiplier = 1.8f,
            paragraphSpacingMultiplier = 0.5f,
            horizontalPaddingDp = 16f,
            verticalPaddingDp = 32f,
            screenWidthPx = 1080,
            screenHeightPx = 2000,   // 去掉状态栏后的可用高度
            density = 2.75f,
        )
        manager = PageLayoutManager(config)

        println("=== 排版参数 ===")
        println("字号: ${config.fontSizeSp}sp → ${config.fontSizePx}px")
        println("行高: ${config.lineHeightPx}px")
        println("内容区: ${config.contentWidthPx} × ${config.contentHeightPx} px")
        println("每行: ${config.charsPerLine} 字, 每页: ${config.linesPerPage} 行")
    }

    // ==================== 基础功能测试 ====================

    @Test
    fun `短文本应生成单页`() {
        val content = "这是一段简短的文字，不足一页。"
        val result = manager.paginateChapter(0, content)

        assertEquals("应只有1页", 1, result.pageCount)
        assertEquals("文字内容应完整", content.trim(), result.getPage(0)!!.text)
    }

    @Test
    fun `空内容应生成空页`() {
        val result = manager.paginateChapter(0, "")

        assertEquals(1, result.pageCount)
        assertEquals("", result.getPage(0)!!.text)
    }

    @Test
    fun `纯换行符内容不应崩溃`() {
        val result = manager.paginateChapter(0, "\n\n\n\n")

        assertEquals(1, result.pageCount)
    }

    @Test
    fun `Page 对象属性应正确`() {
        val content = "Hello World"
        val result = manager.paginateChapter(5, content)
        val page = result.pages.first()

        assertEquals(5, page.chapterIndex)
        assertEquals(0, page.pageIndex)
        assertEquals(0, page.startCharIndex)
        assertEquals(content.length, page.endCharIndex)
        assertEquals(content.trim(), page.text)
    }

    // ==================== 多页分页测试 ====================

    @Test
    fun `长文本应正确分为多页`() {
        val charsPerLine = manager.charsPerLine
        val linesPerPage = manager.linesPerPage
        val totalCharsNeeded = (charsPerLine * linesPerPage * 3) + 100 // 约3页多

        // 使用纯连续文本（无换行符），确保拼接可精确对比
        val content = buildString {
            var len = 0
            while (len < totalCharsNeeded) {
                append("这".repeat(minOf(charsPerLine, totalCharsNeeded - len)))
                len += minOf(charsPerLine, totalCharsNeeded - len)
            }
        }

        val result = manager.paginateChapter(0, content)

        assertTrue("长文本应产生多页，实际=${result.pageCount}", result.pageCount >= 3)

        // 纯文本（无 \n）时，页面拼接后等于原文
        val reconstructed = result.pages.joinToString("") { it.text }
        assertEquals(content, reconstructed)
    }

    @Test
    fun `所有页面字符范围应连续无重叠无遗漏`() {
        // 使用纯连续文本（不含任何 \n），确保字符偏移精确对应
        val charsPerLine = manager.charsPerLine
        val linesPerPage = manager.linesPerPage
        val totalChars = charsPerLine * linesPerPage * 3 + 200

        // 纯文本，无换行符，每段刚好填满一行
        val content = buildString {
            var len = 0
            while (len < totalChars) {
                append("好".repeat(minOf(charsPerLine, totalChars - len)))
                len += minOf(charsPerLine, totalChars - len)
            }
        }

        val result = manager.paginateChapter(0, content)

        var lastEndIndex = 0
        for (page in result.pages) {
            assertEquals("起始位置应衔接上一页结束", lastEndIndex, page.startCharIndex)
            assertTrue("起始应小于结束", page.startCharIndex < page.endCharIndex)
            lastEndIndex = page.endCharIndex
        }
        // 最后一页的 endCharIndex 应等于总长度（纯文本无 \n 时精确匹配）
        assertEquals("最终位置应等于文本长度", content.length, lastEndIndex)
    }

    // ==================== 段落处理测试 ====================

    @Test
    fun `段落间应有换行分隔`() {
        val content = "第一段内容。\n第二段内容。\n第三段内容。"
        val result = manager.paginateChapter(0, content)

        val fullText = result.pages.joinToString("") { it.text }
        // 验证三个段落都存在
        assertTrue("应包含第一段", fullText.contains("第一段"))
        assertTrue("应包含第二段", fullText.contains("第二段"))
        assertTrue("应包含第三段", fullText.contains("第三段"))
    }

    @Test
    fun `超长单段落应跨页拆分`() {
        val charsPerLine = manager.charsPerLine
        val linesPerPage = manager.linesPerPage
        // 一个超长的单行段落（没有\n），长度超过 2 页
        val longParagraph = "A".repeat(charsPerLine * linesPerPage * 2 + 50)

        val result = manager.paginateChapter(0, longParagraph)

        assertTrue("超长单段应产生多页，实际=${result.pageCount}",
            result.pageCount >= 2)

        // 拼接还原
        val reconstructed = result.pages.joinToString("") { it.text }
        assertEquals(longParagraph, reconstructed)
    }

    // ==================== 边界条件 ====================

    @Test
    fun `刚好一页的内容不应溢出到第二页`() {
        val charsPerLine = manager.charsPerLine
        val linesPerPage = manager.linesPerPage
        // 刚好填满一页少一点
        val content = "好".repeat(charsPerLine * (linesPerPage - 1))

        val result = manager.paginateChapter(0, content)

        assertEquals("刚好一页内容应为1页", 1, result.pageCount)
    }

    @Test
    fun `getPage 越界应返回null`() {
        val result = manager.paginateChapter(0, "test")
        assertNull(result.getPage(-1))
        assertNull(result.getPage(999))
    }

    @Test
    fun `不同章节应独立计算页码`() {
        // 使用纯连续文本（无换行符）
        val charsPerLine = manager.charsPerLine
        val linesPerPage = manager.linesPerPage
        val totalChars = charsPerLine * linesPerPage + 100

        val content = buildString {
            var len = 0
            while (len < totalChars) {
                append("测".repeat(minOf(charsPerLine, totalChars - len)))
                len += minOf(charsPerLine, totalChars - len)
            }
        }

        val ch0 = manager.paginateChapter(0, content)
        val ch1 = manager.paginateChapter(1, content)

        // 两章内容相同 → 页数相同
        assertEquals(ch0.pageCount, ch1.pageCount)
        // 章节索引应正确
        assertEquals(0, ch0.chapterIndex)
        assertEquals(1, ch1.chapterIndex)
        // 各章首页 pageIndex 都从 0 开始
        assertEquals(0, ch0.getPage(0)!!.pageIndex)
        assertEquals(0, ch1.getPage(0)!!.pageIndex)
    }

    // ==================== 辅助方法 ====================

    /** 生成指定长度的中文纯文本（无空段落，每行 charsPerLine 个字符） */
    private fun generatePureText(targetLength: Int): String {
        val cpl = manager.charsPerLine
        return buildString {
            var len = 0
            while (len < targetLength) {
                append("文".repeat(minOf(cpl, targetLength - len)))
                append('\n')
                len += cpl
            }
        }.trimEnd('\n')
    }
}
