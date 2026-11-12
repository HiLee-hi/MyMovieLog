package com.mymovie.log.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/** Butter cream palette. Butter is too light for text, so text accents use [Caramel]. */
object PopcornColors {
    val Cream = Color(0xFFFFFCF5)
    val Surface = Color(0xFFFFFFFF)
    val ButterLight = Color(0xFFFFF5DC)
    val Butter = Color(0xFFFAD27A)
    val Caramel = Color(0xFF9A5214)
    val Ink = Color(0xFF3B2A1A)
    val InkMuted = Color(0xFF7A6652)
    val Outline = Color(0xFFF1E6D2)
    val OutlineStrong = Color(0xFFCDB99A)
    val Star = Color(0xFFF2A516)
    val StarEmpty = Color(0xFFE8DAC2)
    /** Warm tint for elevation shadows on posters and floating buttons */
    val Shadow = Caramel.copy(alpha = 0.3f)
}
