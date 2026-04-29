package com.reading.my.domain.repository

import android.net.Uri
import com.reading.my.domain.model.Book
import com.reading.my.domain.model.Chapter
import com.reading.my.domain.model.SyncImportResult
import com.reading.my.domain.model.SyncPayload
import kotlinx.coroutines.flow.Flow

/**
 * 书籍仓储接口
 *
 * 遵循 Clean Architecture 原则，定义在 domain 层。
 * 实现在 data 层通过 Room + DocxParser 完成。
 */
interface BookRepository {

    // ==================== 书籍 CRUD ====================

    /** 观察所有本地书籍列表（Flow形式，数据变化时自动通知UI） */
    fun observeAllBooks(): Flow<List<Book>>

    /** 根据ID获取书籍详情 */
    suspend fun getBookById(bookId: Long): Book?

    /** 搜索书籍（按标题模糊匹配） */
    suspend fun searchBooks(keyword: String): List<Book>

    // ==================== 导入流程 ====================

    /**
     * 解析并导入一本 docx 文件
     *
     * @param filePath 文件绝对路径
     * @param authorName 作者名
     * @return 导入成功后的 Book；失败返回 null
     */
    suspend fun importBook(filePath: String, authorName: String = "阅读者"): Book?

    /**
     * 从 SAF 文件选择器返回的 URI 解析并导入 docx 文件
     *
     * 流程：读取URI内容 → 复制到App缓存目录 → DocxParser解析 → 存入DB
     *
     * @param uri SAF 返回的文件 URI
     * @param authorName 作者名
     * @return 导入成功后的 Book；失败返回 null
     */
    suspend fun importFromUri(uri: Uri, authorName: String = "阅读者"): Book?

    /**
     * 从 Cwriter 写作APP同步导入书籍
     *
     * 支持增量同步：根据 bookId 匹配已有书籍，
     * 通过 contentHash 检测章节变化，仅更新有变化的章节。
     *
     * @param payload Cwriter 导出的 SyncPayload JSON 解析后的对象
     * @return 同步导入结果（含新增/更新统计）
     */
    suspend fun importFromSyncPayload(payload: SyncPayload): SyncImportResult

    // ==================== 章节 ====================

    /** 获取某本书的所有章节 */
    suspend fun getChaptersByBookId(bookId: Long): List<Chapter>

    /** 获取单个章节 */
    suspend fun getChapter(bookId: Long, chapterIndex: Int): Chapter?

    // ==================== 删除 ====================

    /** 删除一本书及其所有章节 */
    suspend fun deleteBook(bookId: Long)

    /** 清空所有本地书籍 */
    suspend fun deleteAllBooks()

    /** 更新书籍标题和简介（本地持久化） */
    suspend fun updateBookMeta(bookId: Long, title: String, description: String?)

    /** 更新书籍封面路径（本地持久化） */
    suspend fun updateBookCover(bookId: Long, coverPath: String)
}
