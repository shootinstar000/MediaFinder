package com.example.mediafinder.extractors

import com.example.mediafinder.api.Extractor
import com.example.mediafinder.api.VideoFile
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup

class FastDlExtractor : Extractor {
    override val name = "FastDL"
    override val mainUrl = "fastdl.icu"

    // We use a standard Ktor client. It will automatically follow the redirect
    // from the .../embed?download=... page to the .../dl.php?link=... page.
    private val client = HttpClient(Android)

    override suspend fun extract(url: String): List<VideoFile> {
        return try {
            println("FASTDL_EXTRACTOR: Following link to get final page HTML: $url")

            // Step 1: Get the HTML content of the page after the redirect.
            val finalPageHtml = client.get(url).bodyAsText()

            // Step 2: Parse the HTML with Jsoup.
            val document = Jsoup.parse(finalPageHtml)

            // Step 3: Find the direct download link.
            // We'll find an <a> tag where the 'href' attribute contains "googleusercontent.com".
            val googleLinkElement = document.selectFirst("a[href*=\"googleusercontent.com\"]")
            val finalUrl = googleLinkElement?.attr("href")

            if (finalUrl != null) {
                println("FASTDL_EXTRACTOR: SUCCESS! Found direct link by scraping: $finalUrl")
                // Return the found URL wrapped in our data class.
                listOf(VideoFile(url = finalUrl, quality = "FastDL Direct Link"))
            } else {
                println("FASTDL_EXTRACTOR: FAILURE. Could not find googleusercontent link on the page.")
                emptyList()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}