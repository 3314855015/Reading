package com.reading.my.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reading.my.data.local.database.entity.RenderCacheEntity

/**
 * 渲染缓存 DAO（L2 二级缓存数据访问）
 *
 * 提供分页结果的持久化读写操作。
 */
@Dao
interface RenderCacheDao {

    /**
     * 插入或更新缓存记录（cacheKey 冲突时替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: RenderCacheEntity)

    /**
     * 根据缓存主键查询
     */
    @Query("SELECT * FROM render_cache WHERE cacheKey = :cacheKey")
    suspend fun getByKey(cacheKey: String): RenderCacheEntity?

    /**
     * 查询指定书籍和章节的缓存（不限定 configHash，用于检测是否存在）
     */
    @Query("SELECT * FROM render_cache WHERE bookId = :bookId AND chapterIndex = :chapterIndex LIMIT 1")
    suspend fun getByBookAndChapter(bookId: String, chapterIndex: Int): RenderCacheEntity?

    /**
     * 删除指定缓存记录
     */
    @Query("DELETE FROM render_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String): Int

    /**
     * 删除指定书籍的所有渲染缓存（切换书籍时清理）
     */
    @Query("DELETE FROM render_cache WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: String): Int

    /**
     * 删除指定章节的缓存（章节内容变化时清理）
     */
    @Query("DELETE FROM render_cache WHERE bookId = :bookId AND chapterIndex = :chapterIndex")
    suspend fun deleteByBookAndChapter(bookId: String, chapterIndex: Int): Int

    /**
     * 清空全部渲染缓存
     */
    @Query("DELETE FROM render_cache")
    suspend fun deleteAll()

    /** 获取指定书籍的缓存总条数 */
    @Query("SELECT COUNT(*) FROM render_cache WHERE bookId = :bookId")
    suspend fun countByBookId(bookId: String): Int

    /** 获取全部缓存条数 */
    @Query("SELECT COUNT(*) FROM render_cache")
    suspend fun countAll(): Int

    /**
     * 批量插入或更新
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(entities: List<RenderCacheEntity>)
}
