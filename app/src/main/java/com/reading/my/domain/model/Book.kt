package com.reading.my.domain.model

/**
 * 书籍领域模型
 *
 * 对应数据库设计文档 §3.2 book 表 + §4.1 local_book 表
 * 统一了云端和本地书籍的概念，通过 source 区分来源
 */
data class Book(
    val id: Long = 0,
    val title: String,
    val author: String = "未知作者",
    val coverPath: String? = null,
    val description: String? = null,
    /** 来源：LOCAL(本地导入) / ONLINE(开源API) / GROUP(圈子) */
    val source: BookSource = BookSource.LOCAL,
    val chapterCount: Int = 0,
    /** 本地文件路径（source=LOCAL 时有效） */
    val filePath: String? = null,
    /** 外部 API 的书籍 ID（source=ONLINE 时有效） */
    val externalId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 书籍来源枚举
 * 对应数据库设计文档 §6.2 book.source 数据字典
 */
enum class BookSource {
    LOCAL,   // 本地导入（docx/doc）
    ONLINE,  // 开源 API（如 Open Library）
    GROUP,   // 圈子分享
}
