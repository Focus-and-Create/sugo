package com.seogoapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.seogoapp.data.model.*

@Database(
    entities = [
        Folder::class,
        Scene::class,
        Tag::class,
        SceneTag::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun sceneDao(): SceneDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "seogo_db"
    }
}
