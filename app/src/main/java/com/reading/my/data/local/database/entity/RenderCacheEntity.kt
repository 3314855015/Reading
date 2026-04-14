package com.reading.my.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 渲染缓存表（L2 二级缓存）
 *
 * 将分页引擎的输出结果（Page 列表）持久化到 Room 数据库，
 * 避免每次打开阅读器时重复执行分页计算。
 *
 * 缓存策略：
 * - Key = "bookId_chapterIndex_configHash"
 * - Value = 序列化的 ChapterPages 数据（JSON 格式）
 * - 失效条件：切换书籍、排版参数变化、章节内容更新
 *
 * 对应架构文档 04-数据库设计文档 3.3 节 render_cache 表定义。
 */
@Entity(tableName = "render_cache")
data class RenderCacheEntity(
    /**
     * 缓存主键（唯一标识一次分页结果）
     *
     * 格式："{bookId}_{chapterIndex}_{configHash}"
     * 其中 configHash 由 PageLayoutConfig 的所有排版参数哈希生成
     */
    @PrimaryKey
    val cacheKey: String,

    /** 书籍标识 */
    val bookId: String,

    /** 章节索引 */
    val chapterIndex: Int,

    /** 排版配置哈希值（用于失效检测） */
    val configHash: Int,

    /** 总页数 */
    val pageCount: Int,

    /**
     * 分页结果数据（JSON 序列化的 Page 列表）
     *
     * 每个 Page 包含: chapterIndex, pageIndex, startCharIndex,
     * endCharIndex, text, isContinuation
     */
    val pageDataJson: String,

    /** 缓存创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),
)
