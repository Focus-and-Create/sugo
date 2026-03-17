package com.seogoapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary          = SeogoAccent,
    onPrimary        = NotionBackground,
    primaryContainer = SeogoAccentMuted,
    background       = NotionBackground,
    onBackground     = NotionText,
    surface          = NotionSurface,
    onSurface        = NotionText,
    onSurfaceVariant = NotionTextSub,
    outline          = NotionBorder,
    outlineVariant   = NotionBorder
)

private val DarkColorScheme = darkColorScheme(
    primary          = SeogoAccent,
    onPrimary        = NotionBackground,
    primaryContainer = SeogoAccentMuted,
    background       = NotionBackgroundDark,
    onBackground     = NotionTextDark,
    surface          = NotionSurfaceDark,
    onSurface        = NotionTextDark,
    onSurfaceVariant = NotionTextSubDark,
    outline          = NotionSurfaceDark,
    outlineVariant   = NotionSurfaceDark
)

@Composable
fun SeogoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SeogoTypography,
        content     = content
    )
}
