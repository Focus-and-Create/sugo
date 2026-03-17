package com.seogoapp.data.db

import androidx.room.*
import com.seogoapp.data.model.Scene
import com.seogoapp.data.model.SceneWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface SceneDao {

    // ── 서랍 내 전체 씬 목록 (태그 포함) ──
    @Transaction
    @Query("""
        SELECT * FROM scenes
        WHERE folder_id = :folderId
        ORDER BY sort_order ASC, imported_at DESC
    """)
    fun getScenesInFolder(folderId: Long): Flow<List<SceneWithTags>>

    // ── 서랍 내 캐릭터 필터 ──
    @Transaction
    @Query("""
        SELECT * FROM scenes
        WHERE folder_id = :folderId
          AND characters LIKE '%' || :character || '%'
        ORDER BY sort_order ASC, imported_at DESC
    """)
    fun getScenesByCharacter(folderId: Long, character: String): Flow<List<SceneWithTags>>

    // ── 서랍 내 태그 필터 ──
    @Transaction
    @Query("""
        SELECT s.* FROM scenes s
        INNER JOIN scene_tags st ON st.scene_id = s.scene_id
        INNER JOIN tags t ON t.tag_id = st.tag_id
        WHERE s.folder_id = :folderId AND t.name = :tagName
        ORDER BY s.sort_order ASC, s.imported_at DESC
    """)
    fun getScenesByTag(folderId: Long, tagName: String): Flow<List<SceneWithTags>>

    // ── 서랍 내 검색 (제목 + 미리보기) ──
    @Transaction
    @Query("""
        SELECT * FROM scenes
        WHERE folder_id = :folderId
          AND (title LIKE '%' || :query || '%'
               OR preview_text LIKE '%' || :query || '%')
        ORDER BY sort_order ASC, imported_at DESC
    """)
    fun searchInFolder(folderId: Long, query: String): Flow<List<SceneWithTags>>

    // ── 전체 검색 (서고 화면) ──
    @Transaction
    @Query("""
        SELECT * FROM scenes
        WHERE title LIKE '%' || :query || '%'
           OR preview_text LIKE '%' || :query || '%'
        ORDER BY imported_at DESC
    """)
    fun searchAll(query: String): Flow<List<SceneWithTags>>

    // ── 단일 씬 조회 ──
    @Transaction
    @Query("SELECT * FROM scenes WHERE scene_id = :sceneId")
    suspend fun getSceneById(sceneId: String): SceneWithTags?

    // ── 서랍 내 모든 캐릭터 이름 수집 ──
    @Query("SELECT DISTINCT characters FROM scenes WHERE folder_id = :folderId")
    suspend fun getAllCharactersJson(folderId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: Scene)

    @Update
    suspend fun updateScene(scene: Scene)

    @Delete
    suspend fun deleteScene(scene: Scene)

    @Query("SELECT MAX(sort_order) FROM scenes WHERE folder_id = :folderId")
    suspend fun getMaxSortOrder(folderId: Long): Int?
}
