package com.reading.my.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.reading.my.data.local.database.dao.ChapterDao
import com.reading.my.data.local.database.dao.LocalBookDao
import com.reading.my.data.local.database.entity.ChapterEntity
import com.reading.my.data.local.database.entity.LocalBookEntity

/**
 * App Room 数据库
 *
 * 管理两张核心表：
 * - local_book：本地导入书籍元数据
 * - chapter：书籍章节内容
 */
@Database(
    entities = [LocalBookEntity::class, ChapterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun localBookDao(): LocalBookDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        const val DATABASE_NAME = "reading_app_db"
    }
}
