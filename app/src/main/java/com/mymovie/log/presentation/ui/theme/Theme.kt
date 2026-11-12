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

/**
 * Material roles for the butter cream palette.
 * primary is caramel so every text-colored default (TextButton, tabs, progress) stays readable;
 * butter fills containers instead: filled buttons via [popcornButtonColors] and the navigation
 * indicator via secondaryContainer.
 */
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

/** The design has a single light theme, so system dark mode and dynamic color are not applied. */
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

/** Butter filled button with ink text, the main call to action. */
@Composable
fun popcornButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
)
