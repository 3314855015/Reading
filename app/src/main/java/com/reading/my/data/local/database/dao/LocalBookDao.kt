package com.reading.my.data.local.database.dao

import androidx.room.*
import com.reading.my.data.local.database.entity.LocalBookEntity
import kotlinx.coroutines.flow.Flow

/**
 * 本地书籍 DAO
 */
@Dao
interface LocalBookDao {

    /** 获取所有本地书籍（按导入时间倒序） */
    @Query("SELECT * FROM local_book ORDER BY importTime DESC")
    fun getAllBooks(): Flow<List<LocalBookEntity>>

    /** 根据ID获取单本书 */
    @Query("SELECT * FROM local_book WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): LocalBookEntity?

    /** 根据标题模糊搜索 */
    @Query("SELECT * FROM local_book WHERE title LIKE '%' || :keyword || '%' ORDER BY importTime DESC")
    suspend fun searchBooks(keyword: String): List<LocalBookEntity>

    /** 插入书籍，返回新生成的行ID */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: LocalBookEntity): Long

    /** 更新书籍信息（如章节数量变化时） */
    @Update
    suspend fun updateBook(book: LocalBookEntity)

    /** 删除一本书及其级联章节 */
    @Delete
    suspend fun deleteBook(book: LocalBookEntity)

    /** 清空所有本地书籍数据 */
    @Query("DELETE FROM local_book")
    suspend fun deleteAll()

    /** 统计书籍数量 */
    @Query("SELECT COUNT(*) FROM local_book")
    suspend fun count(): Int
}
