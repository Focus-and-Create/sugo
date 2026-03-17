package com.seogoapp.data.repository

import com.seogoapp.data.db.FolderDao
import com.seogoapp.data.model.Folder
import com.seogoapp.data.model.FolderWithSceneCount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao
) {
    fun getAllFolders(): Flow<List<FolderWithSceneCount>> =
        folderDao.getAllFoldersWithCount()

    suspend fun getFolderById(id: Long): Folder? =
        folderDao.getFolderById(id)

    suspend fun createFolder(name: String): Long {
        val nextOrder = (folderDao.getMaxSortOrder() ?: -1) + 1
        return folderDao.insertFolder(
            Folder(name = name, sortOrder = nextOrder)
        )
    }

    suspend fun renameFolder(folder: Folder, newName: String) {
        folderDao.updateFolder(folder.copy(name = newName))
    }

    suspend fun deleteFolder(folder: Folder) {
        folderDao.deleteFolder(folder)
    }
}
