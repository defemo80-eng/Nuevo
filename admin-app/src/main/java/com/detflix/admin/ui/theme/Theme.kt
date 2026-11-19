package com.detflix.admin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DetflixAdminColorScheme = darkColorScheme(
    primary = DetflixRed,
    onPrimary = DetflixWhite,
    primaryContainer = DetflixRedDark,
    onPrimaryContainer = DetflixWhite,
    secondary = DetflixGold,
    onSecondary = DetflixBlack,
    secondaryContainer = DetflixSurfaceElevated,
    onSecondaryContainer = DetflixWhite,
    tertiary = DetflixRedGlow,
    onTertiary = DetflixWhite,
    background = DetflixBlack,
    onBackground = DetflixWhite,
    surface = DetflixSurface,
    onSurface = DetflixWhite,
    surfaceVariant = DetflixSurfaceElevated,
    onSurfaceVariant = DetflixTextSecondary,
    outline = DetflixBorder,
    outlineVariant = DetflixSurfaceHighlight,
    error = DetflixRedGlow,
    onError = DetflixWhite
)

private val DetflixAdminShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DetflixAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DetflixAdminColorScheme,
        typography = DetflixAdminTypography,
        shapes = DetflixAdminShapes,
        content = content
    )
}
