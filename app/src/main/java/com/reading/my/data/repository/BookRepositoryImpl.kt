package com.reading.my.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Log.e
import com.reading.my.data.local.database.dao.ChapterDao
import com.reading.my.data.local.database.dao.LocalBookDao
import com.reading.my.data.local.database.entity.ChapterEntity
import com.reading.my.data.local.database.entity.LocalBookEntity
import com.reading.my.data.local.parser.DocxParser
import com.reading.my.domain.model.Book
import com.reading.my.domain.model.BookSource
import com.reading.my.domain.model.Chapter
import com.reading.my.domain.repository.BookRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书籍仓储实现
 *
 * 通过 Room DAO + DocxParser 实现 BookRepository 接口。
 * 导入流程：解析文件 → 写入 book 表 → 批量写入 chapter 表
 */
@Singleton
class BookRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localBookDao: LocalBookDao,
    private val chapterDao: ChapterDao,
) : BookRepository {

    companion object {
        const val TAG = "BookRepo"

        /** App 内部缓存目录（存放从 URI 复制的文件） */
        private const val IMPORT_CACHE_DIR = "imported_books"
    }

    // ==================== 书籍 CRUD ====================

    override fun observeAllBooks(): Flow<List<Book>> {
        return localBookDao.getAllBooks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getBookById(bookId: Long): Book? {
        return localBookDao.getBookById(bookId)?.toDomainModel()
    }

    override suspend fun searchBooks(keyword: String): List<Book> {
        return localBookDao.searchBooks(keyword).map { it.toDomainModel() }
    }

    // ==================== 导入流程 ====================

    override suspend fun importBook(filePath: String, authorName: String): Book? {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "导入文件不存在: $filePath")
            return null
        }

        Log.i(TAG, "开始导入: ${file.name} (${file.length()} bytes)")

        // 1. 解析文件
        val parseResult = DocxParser.parse(file, authorName)
            ?: run { Log.e(TAG, "解析失败"); null }
            ?: return null

        Log.i(TAG, "解析结果: 「${parseResult.title}」${parseResult.chapters.size}章")

        // 2. 写入书籍元数据
        val bookEntity = LocalBookEntity(
            title = parseResult.title,
            author = parseResult.author,
            filePath = file.absolutePath,
            fileSize = file.length(),
            chapterCount = parseResult.chapters.size,
            description = parseResult.description,
        )
        val bookId = localBookDao.insertBook(bookEntity)

        // 3. 批量写入章节
        val chapterEntities = parseResult.chapters.mapIndexed { index, ch ->
            ChapterEntity(
                bookId = bookId,
                chapterIndex = index,
                title = ch.title,
                content = ch.content,
                volumeName = ch.volumeName,
                wordCount = ch.wordCount,
            )
        }
        chapterDao.insertChapters(chapterEntities)

        Log.i(TAG, "导入完成! bookId=$bookId, chapters=${chapterEntities.size}")

        // 4. 返回完整的 Book 对象
        return getBookById(bookId)
    }

    /**
     * 从 SAF URI 导入文件（核心：复制到 App 内部缓存 → 再解析）
     *
     * 为什么需要复制？
     * - SAF URI 可能是临时授权的，App 重启后可能失效
     * - DocxParser 需要 File 对象（ZIP 流读取）
     * - 复制到内部存储后，数据完全由 App 控制，无权限问题
     */
    override suspend fun importFromUri(uri: Uri, authorName: String): Book? {
        return try {
            Log.i(TAG, "开始从 URI 导入: $uri")

            // 1. 从 URI 读取内容 → 复制到 App 内部缓存目录
            val cacheFile = copyUriToCache(uri)
                ?: run { Log.e(TAG, "URI 复制失败"); null }
                ?: return null

            Log.i(TAG, "文件已复制到: ${cacheFile.absolutePath} (${cacheFile.length()} bytes)")

            // 2. 用已有的 importBook 方法解析并存入 DB
            val book = importBook(cacheFile.absolutePath, authorName)

            if (book != null) {
                // 3. 解析成功后可以保留缓存文件（作为原始备份），也可以删除
                Log.i(TAG, "URI 导入完成! bookId=${book.id}")
            }

            book
        } catch (e: Exception) {
            Log.e(TAG, "从 URI 导入异常", e)
            null
        }
    }

    /**
     * 将 SAF URI 的文件内容复制到 App 内部缓存目录
     *
     * 使用 ContentResolver 从 URI 读取字节流，
     * 写入 context.cacheDir/imported_books/ 下。
     */
    private fun copyUriToCache(uri: Uri): File? {
        return try {
            // 创建导入缓存子目录
            val cacheDir = File(context.cacheDir, IMPORT_CACHE_DIR).apply { mkdirs() }

            // 用原文件名作为缓存文件名（避免重名冲突加时间戳）
            val fileName = getFileNameFromUri(uri) ?: "${System.currentTimeMillis()}.docx"
            val destFile = File(cacheDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.e(TAG, "无法打开 URI 输入流: $uri")
                return null
            }

            destFile
        } catch (e: Exception) {
            Log.e(TAG, "复制 URI 到缓存失败", e)
            null
        }
    }

    /** 尝试从 URI 获取显示名称，失败则返回 null */
    private fun getFileNameFromUri(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    }.getOrNull()

    // ==================== 章节 ====================

    override suspend fun getChaptersByBookId(bookId: Long): List<Chapter> {
        return chapterDao.getChaptersByBookIdSync(bookId).map { it.toDomainModel() }
    }

    override suspend fun getChapter(bookId: Long, chapterIndex: Int): Chapter? {
        return chapterDao.getChapter(bookId, chapterIndex)?.toDomainModel()
    }

    // ==================== 删除 ====================

    override suspend fun deleteBook(bookId: Long) {
        val book = localBookDao.getBookById(bookId) ?: return
        chapterDao.deleteChaptersByBookId(bookId)
        localBookDao.deleteBook(book)
        Log.d(TAG, "已删除书籍: bookId=$bookId")
    }

    override suspend fun deleteAllBooks() {
        chapterDao.deleteChaptersByBookId(0)
        localBookDao.deleteAll()
        Log.d(TAG, "已清空所有本地书籍")
    }

    override suspend fun updateBookMeta(bookId: Long, title: String, description: String?) {
        val entity = localBookDao.getBookById(bookId) ?: return
        localBookDao.updateBook(entity.copy(title = title, description = description))
        Log.d(TAG, "已更新书籍元数据: bookId=$bookId, title=$title")
    }

    override suspend fun updateBookCover(bookId: Long, coverPath: String) {
        val entity = localBookDao.getBookById(bookId) ?: return
        android.util.Log.d(TAG, "updateBookCover: bookId=$bookId, coverPath prefix=${coverPath.take(80)}")
        localBookDao.updateBook(entity.copy(coverPath = coverPath))
        android.util.Log.d(TAG, "updateBookCover: DB write completed")
    }

    // ==================== Entity ↔ Domain 映射 ====================

    private fun LocalBookEntity.toDomainModel(): Book = Book(
        id = id,
        title = title,
        author = author,
        coverPath = coverPath,
        description = description,
        source = BookSource.LOCAL,
        chapterCount = chapterCount,
        filePath = filePath,
        createdAt = importTime,
    )

    private fun ChapterEntity.toDomainModel(): Chapter = Chapter(
        id = id,
        bookId = bookId,
        chapterIndex = chapterIndex,
        title = title,
        content = content,
        volumeName = volumeName,
        wordCount = wordCount,
    )
}
