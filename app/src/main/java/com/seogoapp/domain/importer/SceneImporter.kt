package com.seogoapp.domain.importer

import android.content.Context
import android.net.Uri
import com.seogoapp.data.model.Scene
import com.seogoapp.data.repository.SceneRepository
import com.seogoapp.domain.parser.HtmlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ImportResult {
    data class Success(val sceneId: String, val title: String) : ImportResult()
    /** 파싱 실패 — 수동 제목 입력 필요 */
    data class NeedManualTitle(val sceneId: String, val contentHtml: String) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

@Singleton
class SceneImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sceneRepository: SceneRepository
) {
    /**
     * SAF Uri → HTML 읽기 → 파싱 → DB 저장.
     * folderId: 저장할 서랍
     * uri: SAF로 선택한 파일 Uri
     */
    suspend fun importFromUri(folderId: Long, uri: Uri): ImportResult {
        return try {
            val htmlContent = readTextFromUri(uri) ?: return ImportResult.Error("파일을 읽을 수 없습니다.")
            val filename = getFilename(uri)
            val titleFromFile = filename
                .removeSuffix(".html")
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()

            val sceneId = UUID.randomUUID().toString()
            val parsed = HtmlParser.parse(htmlContent)

            if (parsed != null) {
                val scene = Scene(
                    sceneId = sceneId,
                    folderId = folderId,
                    title = titleFromFile.ifBlank { "제목 없음" },
                    previewText = parsed.previewText,
                    contentHtml = parsed.contentHtml,
                    characters = HtmlParser.toJson(parsed.characters),
                    tootCount = parsed.tootCount,
                    hasMedia = if (parsed.hasMedia) 1 else 0,
                    mediaUrls = HtmlParser.toJson(parsed.mediaUrls)
                )
                sceneRepository.saveScene(scene)
                ImportResult.Success(sceneId, scene.title)
            } else {
                // 파싱 실패: 원본 HTML만 저장하고 수동 제목 입력 대기
                val fallbackScene = Scene(
                    sceneId = sceneId,
                    folderId = folderId,
                    title = titleFromFile.ifBlank { "제목 없음" },
                    previewText = "",
                    contentHtml = htmlContent
                )
                sceneRepository.saveScene(fallbackScene)
                ImportResult.NeedManualTitle(sceneId, htmlContent)
            }
        } catch (e: Exception) {
            ImportResult.Error(e.localizedMessage ?: "알 수 없는 오류")
        }
    }

    private fun readTextFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFilename(uri: Uri): String {
        // content:// URI에서 파일명 추출
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return it.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: "scene.html"
    }
}
