package com.seogoapp.data.db

import androidx.room.*
import com.seogoapp.data.model.Folder
import com.seogoapp.data.model.FolderWithSceneCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("""
        SELECT f.*,
               COUNT(s.scene_id) AS scene_count,
               MAX(s.imported_at) AS last_imported_at
        FROM folders f
        LEFT JOIN scenes s ON s.folder_id = f.folder_id
        GROUP BY f.folder_id
        ORDER BY f.sort_order ASC, f.created_at ASC
    """)
    fun getAllFoldersWithCount(): Flow<List<FolderWithSceneCount>>

    @Query("SELECT * FROM folders WHERE folder_id = :id")
    suspend fun getFolderById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("UPDATE folders SET sort_order = :order WHERE folder_id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("SELECT MAX(sort_order) FROM folders")
    suspend fun getMaxSortOrder(): Int?
}
