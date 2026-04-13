package com.reading.my.core.reader.domain

/**
 * 单页数据（分页引擎输出）
 *
 * 每个页面包含一个章节内的一段连续文本，记录了起始和结束的字符位置，
 * 用于渲染引擎定位绘制范围，以及缓存系统的 key 生成。
 */
data class Page(
    /** 所属章节索引 */
    val chapterIndex: Int,
    /** 页码（从0开始） */
    val pageIndex: Int,
    /** 本页文本在 chapter.content 中的起始字符位置（含） */
    val startCharIndex: Int,
    /** 本页文本在 chapter.content 中的结束字符位置（不含） */
    val endCharIndex: Int,
    /** 本页实际显示的纯文本内容 */
    val text: String,
    /**
     * 是否为跨页续接段落
     *
     * 当一个段落在上一页没写完，剩余部分延续到本页时为 true。
     * 渲染端据此判断：本页第一行不应有首行缩进，因为它是上一段的延续。
     */
    val isContinuation: Boolean = false,
)

/**
 * 章节的完整分页结果
 *
 * 一个 Chapter 被分页为 List<Page>，ReaderScreen 通过它来：
 * - 知道该章有多少页
 * - 按 pageIndex 快速取到对应页面的文本范围
 */
data class ChapterPages(
    val chapterIndex: Int,
    val pages: List<Page>,
) {
    /** 总页数 */
    val pageCount: Int get() = pages.size

    /** 根据页码获取页面（越界返回 null） */
    fun getPage(pageIndex: Int): Page? = pages.getOrNull(pageIndex)
}
