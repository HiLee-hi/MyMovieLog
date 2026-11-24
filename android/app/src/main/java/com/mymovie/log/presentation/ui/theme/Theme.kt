package com.mymovie.log.presentation.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// primary = caramel so text-colored defaults (TextButton, tabs, progress) stay readable
// butter only fills containers: popcornButtonColors, nav indicator via secondaryContainer
private val PopcornColorScheme = lightColorScheme(
    primary = PopcornColors.Caramel,
    onPrimary = Color.White,
    primaryContainer = PopcornColors.Butter,
    onPrimaryContainer = PopcornColors.Ink,
    secondary = PopcornColors.Caramel,
    onSecondary = Color.White,
    secondaryContainer = PopcornColors.Butter,
    onSecondaryContainer = PopcornColors.Ink,
    tertiary = PopcornColors.InkMuted,
    onTertiary = Color.White,
    tertiaryContainer = PopcornColors.ButterLight,
    onTertiaryContainer = PopcornColors.Ink,
    background = PopcornColors.Cream,
    onBackground = PopcornColors.Ink,
    surface = PopcornColors.Cream,
    onSurface = PopcornColors.Ink,
    surfaceVariant = PopcornColors.ButterLight,
    onSurfaceVariant = PopcornColors.InkMuted,
    surfaceContainerLowest = PopcornColors.Surface,
    surfaceContainerLow = PopcornColors.Surface,
    surfaceContainer = PopcornColors.Surface,
    surfaceContainerHigh = PopcornColors.ButterLight,
    surfaceContainerHighest = PopcornColors.ButterLight,
    inverseSurface = PopcornColors.Ink,
    inverseOnSurface = PopcornColors.Cream,
    outline = PopcornColors.OutlineStrong,
    outlineVariant = PopcornColors.Outline,
)

private val PopcornShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// design is light only -> no dark mode / dynamic color
@Composable
fun MyMovieLogTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = PopcornColorScheme,
        typography = PopcornTypography,
        shapes = PopcornShapes,
        content = content
    )
}

// main CTA: butter fill, ink text
@Composable
fun popcornButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
)
