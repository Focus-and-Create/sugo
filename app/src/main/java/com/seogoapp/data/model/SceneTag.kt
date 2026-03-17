package com.seogoapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "scene_tags",
    primaryKeys = ["scene_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = Scene::class,
            parentColumns = ["scene_id"],
            childColumns = ["scene_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["tag_id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scene_id"), Index("tag_id")]
)
data class SceneTag(
    @ColumnInfo(name = "scene_id") val sceneId: String,
    @ColumnInfo(name = "tag_id") val tagId: Long
)
