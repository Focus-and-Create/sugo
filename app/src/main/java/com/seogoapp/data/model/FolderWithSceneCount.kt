package com.seogoapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

/** 서고 화면 — 서랍당 씬 수 + 마지막 수정일 포함 DTO */
data class FolderWithSceneCount(
    @Embedded val folder: Folder,
    @ColumnInfo(name = "scene_count") val sceneCount: Int = 0,
    @ColumnInfo(name = "last_imported_at") val lastImportedAt: Long? = null
)
