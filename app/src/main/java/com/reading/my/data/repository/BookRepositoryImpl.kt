package com.reading.my.data.repository

import android.util.Log
import com.reading.my.data.local.database.dao.ChapterDao
import com.reading.my.data.local.database.dao.LocalBookDao
import com.reading.my.data.local.database.entity.ChapterEntity
import com.reading.my.data.local.database.entity.LocalBookEntity
import com.reading.my.data.local.parser.DocxParser
import com.reading.my.domain.model.Book
import com.reading.my.domain.model.BookSource
import com.reading.my.domain.model.Chapter
import com.reading.my.domain.repository.BookRepository
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
    private val localBookDao: LocalBookDao,
    private val chapterDao: ChapterDao,
) : BookRepository {

    companion object {
        const val TAG = "BookRepo"
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
        chapterDao.deleteChaptersByBookId(0) // 不支持批量，逐本删
        localBookDao.deleteAll()
        Log.d(TAG, "已清空所有本地书籍")
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
