package com.seogoapp.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** Room 관계 쿼리용 DTO — Scene + 연결된 Tag 목록 */
data class SceneWithTags(
    @Embedded val scene: Scene,
    @Relation(
        parentColumn = "scene_id",
        entityColumn = "tag_id",
        associateBy = Junction(SceneTag::class)
    )
    val tags: List<Tag>
)
