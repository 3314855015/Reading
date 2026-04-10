package com.reading.my.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 章节实体
 *
 * 对应书籍的每一个章节，存储解析后的标题和正文内容。
 * 通过 bookId 外键关联到 local_book 表。
 */
@Entity(
    tableName = "chapter",
    foreignKeys = [
        ForeignKey(
            entity = LocalBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 所属书籍 ID（外键） */
    val bookId: Long,
    /** 在书中的顺序索引（从0开始） */
    val chapterIndex: Int,
    /** 章节标题 */
    val title: String,
    /** 章节完整正文内容 */
    val content: String,
    /** 所属卷名（可为null） */
    val volumeName: String? = null,
    /** 字数统计 */
    val wordCount: Int = 0,
)
