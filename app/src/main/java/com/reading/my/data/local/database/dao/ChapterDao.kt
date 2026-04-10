package com.reading.my.data.local.database.dao

import androidx.room.*
import com.reading.my.data.local.database.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

/**
 * 章节 DAO
 */
@Dao
interface ChapterDao {

    /** 获取某本书的所有章节（按 chapterIndex 升序） */
    @Query("SELECT * FROM chapter WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    fun getChaptersByBookId(bookId: Long): Flow<List<ChapterEntity>>

    /** 同步获取某本书的所有章节 */
    @Query("SELECT * FROM chapter WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersByBookIdSync(bookId: Long): List<ChapterEntity>

    /** 获取单个章节 */
    @Query("SELECT * FROM chapter WHERE bookId = :bookId AND chapterIndex = :index LIMIT 1")
    suspend fun getChapter(bookId: Long, index: Int): ChapterEntity?

    /** 批量插入章节（用于导入时一次性写入） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    /** 删除某本书的所有章节 */
    @Query("DELETE FROM chapter WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: Long)

    /** 统计某本书的章节数量 */
    @Query("SELECT COUNT(*) FROM chapter WHERE bookId = :bookId")
    suspend fun countByBookId(bookId: Long): Int
}
