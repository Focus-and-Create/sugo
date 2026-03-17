package com.seogoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.seogoapp.ui.theme.NotionTextSub
import com.seogoapp.ui.theme.SeogoAccentMuted
import com.seogoapp.ui.theme.SeogoCharacterChip

/** 캐릭터명 칩 (크림색 배경) */
@Composable
fun CharacterChip(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelSmall,
        color = NotionTextSub,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SeogoCharacterChip)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** 태그 칩 (인디고 배경) */
@Composable
fun TagChip(
    tag: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = tag,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SeogoAccentMuted)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
