package com.seogoapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["folder_id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folder_id")]
)
data class Scene(
    @PrimaryKey
    @ColumnInfo(name = "scene_id") val sceneId: String,          // UUID 문자열
    @ColumnInfo(name = "folder_id") val folderId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "preview_text") val previewText: String,  // mention 제거 후 60자
    @ColumnInfo(name = "content_html") val contentHtml: String,  // 원본 HTML blob
    @ColumnInfo(name = "characters") val characters: String = "[]",   // JSON 배열 (표시명)
    @ColumnInfo(name = "toot_count") val tootCount: Int = 0,
    @ColumnInfo(name = "has_media") val hasMedia: Int = 0,       // 0/1
    @ColumnInfo(name = "media_urls") val mediaUrls: String = "[]",    // JSON 배열
    @ColumnInfo(name = "memo") val memo: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "imported_at") val importedAt: Long = System.currentTimeMillis()
)
