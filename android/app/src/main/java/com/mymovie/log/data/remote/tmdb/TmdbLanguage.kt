package com.mymovie.log.data.remote.tmdb

import java.util.Locale

// TMDB translations differ by region for these
private val REGIONAL_LANGUAGES = mapOf(
    "pt" to setOf("BR", "PT"),
    "zh" to setOf("CN", "TW", "HK")
)

// others send no region, e.g. en-KR has no translation of its own
internal fun tmdbLanguage(locale: Locale = Locale.getDefault()): String {
    val language = locale.toLanguageTag().substringBefore('-')
    if (language == "und") return "ko"
    val isRegional = locale.country in REGIONAL_LANGUAGES[language].orEmpty()
    return if (isRegional) "$language-${locale.country}" else language
}
