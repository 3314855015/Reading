package com.reading.my.data.local.parser

import android.util.Log
import com.reading.my.domain.model.Chapter
import com.reading.my.domain.model.ParseResult
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * DOCX/DOC 文件解析器
 *
 * 【设计决策】不使用 Apache POI（6MB+ 重量级），而是利用 DOCX 本质是 ZIP + XML 的特性，
 * 通过 Android 内置的 java.util.zip + 手写轻量 XML 提取器直接解析。
 *
 * 解析流程：
 * 1. 将 .docx 视为 ZIP 压缩包解压
 * 2. 定位并读取 word/document.xml（正文主文件）
 * 3. 按 <w:p>（段落）遍历：
 *    - 检测 Heading 样式 → 新建章节
 *    - 普通段落 → 追加到当前章节内容
 * 4. 输出 ParseResult（元数据 + 全部章节列表）
 */
object DocxParser {

    private const val TAG = "DocxParser"

    /** 支持的文件格式 */
    val supportedExtensions = listOf("docx")

    /** 是否支持该格式 */
    fun supportsFormat(file: File): Boolean {
        return file.extension.lowercase() in supportedExtensions
    }

    /**
     * 解析 DOCX 文件
     *
     * @param file 要解析的 .docx 文件
     * @param authorName 作者名（默认用户自己的名字）
     * @return ParseResult 包含书名、简介、章节列表；失败返回 null
     */
    fun parse(file: File, authorName: String = "阅读者"): ParseResult? {
        if (!file.exists()) {
            Log.e(TAG, "文件不存在: ${file.absolutePath}")
            return null
        }

        if (!supportsFormat(file)) {
            Log.e(TAG, "不支持的格式: ${file.extension}")
            return null
        }

        return try {
            val documentXml = extractDocumentXml(file)
                ?: run { Log.e(TAG, "未找到 word/document.xml"); null }
                ?: return null

            parseDocumentXml(documentXml, file.nameWithoutExtension, authorName)
        } catch (e: Exception) {
            Log.e(TAG, "解析失败", e)
            null
        }
    }

    // ==================== 内部方法 ====================

    /** 从 ZIP 中提取 word/document.xml 的文本内容 */
    private fun extractDocumentXml(file: File): String? {
        var result: String? = null
        ZipInputStream(BufferedInputStream(file.inputStream())).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    result = zipIn.bufferedReader(Charsets.UTF_8).readText()
                    break
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        return result
    }

    /**
     * 解析 document.xml 字符串
     *
     * 核心逻辑：
     * - 按 <w:p> 分割出每个段落
     * - 每个段落内检测 <w:pStyle> 判断是否为标题样式
     * - 收集 <w:t> 标签内的纯文本
     */
    private fun parseDocumentXml(
        xmlContent: String,
        fallbackTitle: String,
        authorName: String
    ): ParseResult {
        // ---- Step 1: 提取段落列表 ----
        val rawParagraphs = extractParagraphs(xmlContent)

        // ---- Step 2: 分类为标题段和正文段 ----
        data class ParaData(val isHeading: Boolean, val headingLevel: Int, val text: String)

        val paragraphs = rawParagraphs.map { pXml ->
            val styleVal = extractStyleValue(pXml)
            val text = extractTextContent(pXml).trim()
            when {
                styleVal?.startsWith("Heading", ignoreCase = true) == true -> {
                    // "Heading1" -> level=1, "Heading2" -> level=2
                    val level = styleVal.drop(7).toIntOrNull() ?: 1
                    ParaData(isHeading = true, headingLevel = level.coerceIn(1, 9), text = text)
                }
                else -> ParaData(isHeading = false, headingLevel = 0, text = text)
            }
        }.filter { it.text.isNotBlank() }

        if (paragraphs.isEmpty()) {
            Log.w(TAG, "未提取到任何有效段落")
            return ParseResult(
                title = fallbackTitle,
                author = authorName,
                chapters = emptyList()
            )
        }

        // ---- Step 3: 组装章节 ----
        val chapters = mutableListOf<Chapter>()
        var currentTitle = ""
        var currentContent = StringBuilder()
        var chapterIndex = 0
        var firstHeadingSeen = false

        for ((index, para) in paragraphs.withIndex()) {
            when {
                // 标题段落 → 结束上一章，开始新章
                para.isHeading -> {
                    // 保存上一章（如果有内容）
                    if (currentContent.isNotBlank()) {
                        chapters.add(
                            Chapter(
                                bookId = 0L,
                                chapterIndex = chapterIndex++,
                                title = currentTitle.ifBlank { "序言" },
                                content = currentContent.toString().trim()
                            )
                        )
                        currentContent = StringBuilder()
                    }
                    currentTitle = para.text
                    firstHeadingSeen = true
                }
                // 正文段落 → 追加到当前章节
                else -> {
                    if (currentContent.isNotEmpty()) currentContent.append("\n")
                    currentContent.append(para.text)
                }
            }
        }

        // 保存最后一章
        if (currentContent.isNotBlank()) {
            chapters.add(
                Chapter(
                    bookId = 0L,
                    chapterIndex = chapterIndex,
                    title = currentTitle.ifBlank { if (!firstHeadingSeen) "全文" else "尾声" },
                    content = currentContent.toString().trim()
                )
            )
        }

        // 如果完全没有 Heading（整篇是普通段落），合并为一个"全文"章节
        if (chapters.isEmpty() && paragraphs.isNotEmpty()) {
            val fullText = paragraphs.joinToString("\n") { it.text }
            chapters.add(
                Chapter(bookId = 0L, chapterIndex = 0, title = "全文", content = fullText)
            )
        }

        // ---- Step 4: 提取元数据 ----
        val title = chapters.firstOrNull()?.title ?: fallbackTitle
        val description = if (chapters.size > 1) {
            "${chapters.size} 章 · 共 ${chapters.sumOf { it.content.length }} 字"
        } else null

        Log.i(TAG, "解析完成: 「$title」${chapters.size}章")

        return ParseResult(
            title = title,
            author = authorName,
            description = description,
            chapters = chapters
        )
    }

    // ==================== XML 轻量提取工具 ====================

    /** 从 document.xml 中提取所有 <w:p>...</w:p> 段落的原始 XML */
    private fun extractParagraphs(xml: String): List<String> {
        val results = mutableListOf<String>()
        var start = 0
        while (true) {
            val openIdx = xml.indexOf("<w:p ", start)
            if (openIdx == -1) break
            val closeIdx = xml.indexOf("</w:p>", openIdx)
            if (closeIdx == -1) break
            results.add(xml.substring(openIdx, closeIdx + "</w:p>".length))
            start = closeIdx + "</w:p>".length
        }
        return results
    }

    /** 从段落XML中提取 <w:pStyle w:val="xxx"/> 的值 */
    private fun extractStyleValue(paraXml: String): String? {
        // 匹配 <w:pStyle w:val="HeadingX"/> 或 <w:pStyle w:val="HeadingX" />
        val regex = Regex("""<w:pStyle[^>]*w:val=["']([^"']+)["'][^>]*/?>""")
        return regex.find(paraXml)?.groupValues?.get(1)
    }

    /** 从段落XML中提取所有 <w:t> 的文本内容并拼接 */
    private fun extractTextContent(paraXml: String): String {
        val results = mutableListOf<String>()
        var searchFrom = 0
        while (true) {
            val openTag = paraXml.indexOf("<w:t", searchFrom)
            if (openTag == -1) break
            val closeTagStart = paraXml.indexOf(">", openTag)
            if (closeTagStart == -1) break
            val endTag = paraXml.indexOf("</w:t>", closeTagStart)
            if (endTag == -1) break
            results.add(paraXml.substring(closeTagStart + 1, endTag))
            searchFrom = endTag + "</w:t>".length
        }
        return results.joinToString("")
    }
}
