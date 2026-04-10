package com.reading.my.data.local.database

import android.content.Context
import androidx.room.Room
import com.reading.my.data.repository.BookRepositoryImpl
import com.reading.my.domain.repository.BookRepository
import com.reading.my.data.local.database.dao.ChapterDao
import com.reading.my.data.local.database.dao.LocalBookDao
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
 *   - AppDatabase (Room Database 单例)
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
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideLocalBookDao(database: AppDatabase): LocalBookDao = database.localBookDao()

        @Provides
        @Singleton
        fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()
    }
}
