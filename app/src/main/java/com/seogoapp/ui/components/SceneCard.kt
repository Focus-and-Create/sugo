package com.seogoapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seogoapp.data.model.SceneWithTags
import com.seogoapp.domain.parser.HtmlParser
import com.seogoapp.ui.theme.NotionTextSub
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SceneCard(
    sceneWithTags: SceneWithTags,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scene = sceneWithTags.scene
    val characters = HtmlParser.fromJson(scene.characters)
    val tags = sceneWithTags.tags

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ── 씬 제목 ──
            Text(
                text = scene.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // ── 미리보기 본문 ──
            if (scene.previewText.isNotBlank()) {
                Text(
                    text = scene.previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotionTextSub,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── 캐릭터 + 태그 칩 ──
            if (characters.isNotEmpty() || tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(characters) { char ->
                        CharacterChip(name = char)
                    }
                    items(tags) { tag ->
                        TagChip(tag = tag.name)
                    }
                }
            }

            // ── 하단 메타: toot 수 · 날짜 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${scene.tootCount}개 톳",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(scene.importedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("yy.MM.dd", Locale.KOREA)

private fun formatDate(epochMs: Long): String =
    dateFormat.format(Date(epochMs))
