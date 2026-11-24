package com.mymovie.log.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.time.format.DateTimeFormatter

/** User-facing text held by a ViewModel, which has no access to string resources. */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(@StringRes val id: Int) : UiText

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(id)
    }
}

/** The exception's own message when it has one, otherwise [fallback]. */
fun Throwable.toUiText(@StringRes fallback: Int): UiText =
    message?.let(UiText::Dynamic) ?: UiText.Resource(fallback)

@Composable
fun rememberDateTimeFormatter(@StringRes pattern: Int): DateTimeFormatter {
    val value = stringResource(pattern)
    return remember(value) { DateTimeFormatter.ofPattern(value) }
}
