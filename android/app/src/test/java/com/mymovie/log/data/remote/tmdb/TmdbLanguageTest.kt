package com.mymovie.log.data.remote.tmdb

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TmdbLanguageTest {

    private fun language(tag: String) = tmdbLanguage(Locale.forLanguageTag(tag))

    @Test
    fun sendsOnlyTheLanguageForMostLocales() {
        assertEquals("ko", language("ko-KR"))
        assertEquals("en", language("en-US"))
        assertEquals("en", language("en-KR"))
        assertEquals("ja", language("ja-JP"))
    }

    @Test
    fun keepsTheRegionWhereTranslationsDifferByRegion() {
        assertEquals("pt-BR", language("pt-BR"))
        assertEquals("zh-TW", language("zh-Hant-TW"))
        assertEquals("zh", language("zh-Hans-KR"))
    }

    @Test
    fun fallsBackToKoreanWithoutALanguage() {
        assertEquals("ko", tmdbLanguage(Locale.ROOT))
    }
}
