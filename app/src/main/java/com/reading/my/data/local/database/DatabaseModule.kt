package com.reading.my.data.local.database

import android.content.Context
import androidx.room.Room
import com.reading.my.core.reader.engine.L2DatabaseCache
import com.reading.my.data.repository.BookRepositoryImpl
import com.reading.my.domain.repository.BookRepository
import com.reading.my.data.local.database.dao.ChapterDao
import com.reading.my.data.local.database.dao.LocalBookDao
import com.reading.my.data.local.database.dao.RenderCacheDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库 + Repository Hilt 依赖注入模块
 *
 * 提供绑定：
 *   - AppDatabase (Room Database 单例, version=2)
 *   - RenderCacheDao (L2 缓存 DAO)
 *   - L2DatabaseCache (二级缓存管理器)
 *   - BookRepository → BookRepositoryImpl (接口绑定)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                .addMigrations(AppDatabase.MIGRATION_1_2)     // Phase 5: 新增 render_cache 表
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideLocalBookDao(database: AppDatabase): LocalBookDao = database.localBookDao()

        @Provides
        @Singleton
        fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()

        /** Phase 5: L2 渲染缓存 DAO */
        @Provides
        @Singleton
        fun provideRenderCacheDao(database: AppDatabase): RenderCacheDao = database.renderCacheDao()

        /** Phase 5: L2 数据库缓存管理器 */
        @Provides
        @Singleton
        fun provideL2DatabaseCache(dao: RenderCacheDao): L2DatabaseCache = L2DatabaseCache(dao)
    }
}
