package com.reading.my.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地书籍实体
 *
 * 对应数据库设计文档 §4.1 local_book 表
 * 存储用户导入的 docx/doc 书籍元数据
 */
@Entity(tableName = "local_book")
data class LocalBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "未知作者",
    /** 封面图片本地路径（暂无封面时为null） */
    val coverPath: String? = null,
    /** 原始文件路径 */
    val filePath: String? = null,
    /** 文件大小（字节） */
    val fileSize: Long = 0L,
    /** 章节总数 */
    val chapterCount: Int = 0,
    /** 简介/描述 */
    val description: String? = null,
    /** 导入时间戳 */
    val importTime: Long = System.currentTimeMillis(),
)
