package com.seogoapp.data.db

import androidx.room.*
import com.seogoapp.data.model.SceneTag
import com.seogoapp.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Delete
    suspend fun deleteTag(tag: Tag)

    // SceneTag 연결
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSceneTag(sceneTag: SceneTag)

    @Delete
    suspend fun deleteSceneTag(sceneTag: SceneTag)

    @Query("DELETE FROM scene_tags WHERE scene_id = :sceneId")
    suspend fun deleteAllTagsForScene(sceneId: String)

    // 특정 서랍에서 사용된 태그
    @Query("""
        SELECT DISTINCT t.* FROM tags t
        INNER JOIN scene_tags st ON st.tag_id = t.tag_id
        INNER JOIN scenes s ON s.scene_id = st.scene_id
        WHERE s.folder_id = :folderId
        ORDER BY t.name ASC
    """)
    fun getTagsInFolder(folderId: Long): Flow<List<Tag>>
}
