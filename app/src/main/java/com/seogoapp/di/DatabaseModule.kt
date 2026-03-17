package com.seogoapp.di

import android.content.Context
import androidx.room.Room
import com.seogoapp.data.db.AppDatabase
import com.seogoapp.data.db.FolderDao
import com.seogoapp.data.db.SceneDao
import com.seogoapp.data.db.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideSceneDao(db: AppDatabase): SceneDao = db.sceneDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
}
