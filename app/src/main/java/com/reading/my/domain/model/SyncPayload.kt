package com.reading.my.domain.model

/**
 * APP 间同步协议数据模型
 *
 * 对应 spec/sync-app-spec.md 定义的 SyncPayload 协议 v1.0
 * 由 Cwriter（写作APP）导出，Reading（阅读APP）接收并解析。
 */
data class SyncPayload(
    /** 书籍唯一标识（对应 Cwriter Work.syncId）*/
    val bookId: String,
    val bookTitle: String,
    val author: String = "未知作者",
    val description: String? = null,
    /** 同步版本号，每次 Cwriter 侧修改后自增 */
    val syncVersion: Int = 0,
    /** Cwriter 侧最后修改时间戳（毫秒） */
    val lastModified: Long = 0L,
    /** 章节列表 */
    val chapters: List<ChapterSyncItem> = emptyList(),
)

/**
 * 同步协议中的单章数据
 */
data class ChapterSyncItem(
    /** 章节唯一标识（对应 Cwriter Chapter.syncId） */
    val chapterId: String,
    /** 章节序索引（从0开始） */
    val chapterIndex: Int,
    val title: String,
    val content: String,
    /** MD5 内容哈希，用于增量同步时检测章节是否被修改 */
    val contentHash: String,
    /** 所属卷名（可为null） */
    val volumeName: String? = null,
)

/**
 * 同步导入结果
 */
data class SyncImportResult(
    val success: Boolean,
    val message: String,
    /** 导入/更新的书籍 ID（Room 自增主键） */
    val bookId: Long = 0,
    /** 总章节数 */
    val totalChapters: Int = 0,
    /** 新增章节数 */
    val newChapters: Int = 0,
    /** 更新章节数（内容变化） */
    val updatedChapters: Int = 0,
)

/**
 * 同步来源枚举
 */
enum class SyncSource {
    /** 来自 Cwriter 写作APP的本地 Intent 同步 */
    CWRITER_INTENT,
    /** 未来：来自云端的 HTTP 同步 */
    CLOUD,
}
