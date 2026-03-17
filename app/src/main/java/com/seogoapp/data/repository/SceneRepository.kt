package com.seogoapp.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.seogoapp.data.db.SceneDao
import com.seogoapp.data.db.TagDao
import com.seogoapp.data.model.Scene
import com.seogoapp.data.model.SceneTag
import com.seogoapp.data.model.SceneWithTags
import com.seogoapp.data.model.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SceneRepository @Inject constructor(
    private val sceneDao: SceneDao,
    private val tagDao: TagDao
) {
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    // ── 조회 ──

    fun getScenesInFolder(folderId: Long): Flow<List<SceneWithTags>> =
        sceneDao.getScenesInFolder(folderId)

    fun searchInFolder(folderId: Long, query: String): Flow<List<SceneWithTags>> =
        sceneDao.searchInFolder(folderId, query)

    fun filterByCharacter(folderId: Long, character: String): Flow<List<SceneWithTags>> =
        sceneDao.getScenesByCharacter(folderId, character)

    fun filterByTag(folderId: Long, tagName: String): Flow<List<SceneWithTags>> =
        sceneDao.getScenesByTag(folderId, tagName)

    fun searchAll(query: String): Flow<List<SceneWithTags>> =
        sceneDao.searchAll(query)

    suspend fun getSceneById(sceneId: String): SceneWithTags? =
        sceneDao.getSceneById(sceneId)

    /** 서랍에 등장한 고유 캐릭터명 목록 */
    suspend fun getCharactersInFolder(folderId: Long): List<String> {
        val jsonList = sceneDao.getAllCharactersJson(folderId)
        return jsonList.flatMap { json ->
            runCatching { gson.fromJson<List<String>>(json, listType) }.getOrDefault(emptyList())
        }.distinct().sorted()
    }

    // ── 저장 ──

    suspend fun saveScene(scene: Scene, tagNames: List<String> = emptyList()) {
        val maxOrder = sceneDao.getMaxSortOrder(scene.folderId) ?: -1
        val ordered = scene.copy(sortOrder = maxOrder + 1)
        sceneDao.insertScene(ordered)
        saveTagsForScene(scene.sceneId, tagNames)
    }

    suspend fun updateMemo(scene: Scene, memo: String?) {
        sceneDao.updateScene(scene.copy(memo = memo))
    }

    suspend fun updateTitle(scene: Scene, title: String) {
        sceneDao.updateScene(scene.copy(title = title))
    }

    suspend fun updateTags(sceneId: String, tagNames: List<String>) {
        tagDao.deleteAllTagsForScene(sceneId)
        saveTagsForScene(sceneId, tagNames)
    }

    suspend fun deleteScene(scene: Scene) {
        sceneDao.deleteScene(scene)
    }

    private suspend fun saveTagsForScene(sceneId: String, tagNames: List<String>) {
        tagNames.forEach { name ->
            val trimmed = name.trim().ifBlank { return@forEach }
            var tag = tagDao.getTagByName(trimmed)
            if (tag == null) {
                val id = tagDao.insertTag(Tag(name = trimmed))
                tag = Tag(tagId = id, name = trimmed)
            }
            tagDao.insertSceneTag(SceneTag(sceneId = sceneId, tagId = tag.tagId))
        }
    }
}
