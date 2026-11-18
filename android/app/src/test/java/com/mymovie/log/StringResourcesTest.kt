package com.mymovie.log

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringResourcesTest {

    private fun load(folder: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$folder/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }

    private fun placeholders(text: String) =
        Regex("""%\d+\$[ds]""").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun englishHasEveryKoreanKey() {
        assertEquals(load("values").keys, load("values-en").keys)
    }

    @Test
    fun translationsKeepTheSamePlaceholders() {
        val english = load("values-en")
        load("values").forEach { (key, korean) ->
            assertEquals(key, placeholders(korean), placeholders(english.getValue(key)))
        }
    }
}
