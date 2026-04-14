package com.reading.my.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.reading.my.data.local.database.dao.ChapterDao
import com.reading.my.data.local.database.dao.LocalBookDao
import com.reading.my.data.local.database.dao.RenderCacheDao
import com.reading.my.data.local.database.entity.ChapterEntity
import com.reading.my.data.local.database.entity.LocalBookEntity
import com.reading.my.data.local.database.entity.RenderCacheEntity

/**
 * App Room 数据库
 *
 * 管理三张表：
 * - local_book：本地导入书籍元数据
 * - chapter：书籍章节内容
 * - render_cache：渲染缓存（L2 二级缓存）
 */
@Database(
    entities = [LocalBookEntity::class, ChapterEntity::class, RenderCacheEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun localBookDao(): LocalBookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun renderCacheDao(): RenderCacheDao

    companion object {
        const val DATABASE_NAME = "reading_app_db"

        /** Version 1 → 2: 新增 render_cache 表 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 注意：表结构必须与 RenderCacheEntity 完全一致，
                // 否则 Room 验证会抛出 IllegalStateException
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `render_cache` (
                        `cacheKey` TEXT NOT NULL PRIMARY KEY,
                        `bookId` TEXT NOT NULL,
                        `chapterIndex` INTEGER NOT NULL,
                        `configHash` INTEGER NOT NULL,
                        `pageCount` INTEGER NOT NULL,
                        `pageDataJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
