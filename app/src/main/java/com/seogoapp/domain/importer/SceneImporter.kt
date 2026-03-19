package com.seogoapp.domain.importer

import android.content.Context
import android.net.Uri
import com.seogoapp.data.model.Scene
import com.seogoapp.data.repository.SceneRepository
import com.seogoapp.domain.parser.HtmlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** 이미지 타입 씬을 구별하기 위한 마커 */
const val IMAGE_SCENE_MARKER = "__IMAGE_SCENE__"

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

    /**
     * 이미지 파일을 씬으로 가져오기.
     * 이미지를 앱 내부 저장소에 복사하고, 웹툰 스타일로 볼 수 있는 씬 생성.
     */
    suspend fun importImageFromUri(folderId: Long, uri: Uri): ImportResult {
        return try {
            val filename = getFilename(uri)
            val titleFromFile = filename
                .substringBeforeLast('.')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()

            val sceneId = UUID.randomUUID().toString()

            // 이미지를 앱 내부 저장소에 복사
            val imageDir = File(context.filesDir, "images")
            if (!imageDir.exists()) imageDir.mkdirs()
            val ext = filename.substringAfterLast('.', "jpg")
            val destFile = File(imageDir, "$sceneId.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return ImportResult.Error("이미지를 읽을 수 없습니다.")

            val localPath = destFile.absolutePath

            val scene = Scene(
                sceneId = sceneId,
                folderId = folderId,
                title = titleFromFile.ifBlank { "이미지" },
                previewText = "이미지 씬",
                contentHtml = IMAGE_SCENE_MARKER,
                hasMedia = 1,
                mediaUrls = HtmlParser.toJson(listOf(localPath))
            )
            sceneRepository.saveScene(scene)
            ImportResult.Success(sceneId, scene.title)
        } catch (e: Exception) {
            ImportResult.Error(e.localizedMessage ?: "알 수 없는 오류")
        }
    }

    /**
     * 여러 이미지를 하나의 씬으로 가져오기 (웹툰 스타일).
     */
    suspend fun importMultipleImagesFromUris(folderId: Long, uris: List<Uri>): ImportResult {
        return try {
            val sceneId = UUID.randomUUID().toString()
            val imageDir = File(context.filesDir, "images")
            if (!imageDir.exists()) imageDir.mkdirs()

            val localPaths = mutableListOf<String>()
            uris.forEachIndexed { index, uri ->
                val filename = getFilename(uri)
                val ext = filename.substringAfterLast('.', "jpg")
                val destFile = File(imageDir, "${sceneId}_$index.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return ImportResult.Error("이미지를 읽을 수 없습니다.")
                localPaths.add(destFile.absolutePath)
            }

            val scene = Scene(
                sceneId = sceneId,
                folderId = folderId,
                title = "이미지 (${uris.size}장)",
                previewText = "이미지 씬",
                contentHtml = IMAGE_SCENE_MARKER,
                hasMedia = 1,
                mediaUrls = HtmlParser.toJson(localPaths)
            )
            sceneRepository.saveScene(scene)
            ImportResult.Success(sceneId, scene.title)
        } catch (e: Exception) {
            ImportResult.Error(e.localizedMessage ?: "알 수 없는 오류")
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
