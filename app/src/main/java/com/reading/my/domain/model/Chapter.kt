package com.reading.my.domain.model

/**
 * 章节领域模型
 *
 * 一本书由多个 Chapter 组成，每个 Chapter 对应文档中的一个标题段落。
 * 卷（Volume）是章节的分组概念，用于多卷本小说。
 */
data class Chapter(
    val id: Long = 0,
    /** 所属书籍 ID */
    val bookId: Long,
    /** 章节在书中的顺序索引（从0开始） */
    val chapterIndex: Int,
    /** 章节标题 */
    val title: String,
    /** 章节完整正文内容（纯文本） */
    val content: String,
    /** 所属卷名（可为null，表示无卷分组） */
    val volumeName: String? = null,
    /** 字数统计 */
    val wordCount: Int = content.length,
)

/**
 * 解析结果
 *
 * DocxParser 解析 docx/doc 文件后的完整输出，
 * 包含元数据 + 所有章节，可直接存入数据库。
 */
data class ParseResult(
    /** 书籍标题（优先取文档属性，其次文件名） */
    val title: String,
    /** 作者（默认为当前用户名） */
    val author: String = "未知作者",
    /** 简介/描述（如有） */
    val description: String? = null,
    /** 解析出的所有章节列表（按顺序排列） */
    val chapters: List<Chapter>,
)
