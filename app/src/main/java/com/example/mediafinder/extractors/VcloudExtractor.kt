package com.example.mediafinder.extractors

import com.example.mediafinder.api.Extractor
import com.example.mediafinder.api.VideoFile
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup

class VcloudExtractor : Extractor {
    override val name = "Vcloud"
    override val mainUrl = "vcloud.lol"
    private val client = HttpClient(Android)

    override suspend fun extract(url: String): List<VideoFile> {
        return try {
            // Step 1: Get the HTML content of the vcloud page.
            val htmlContent = client.get(url).bodyAsText()

            // Find the intermediate URL from the script using Regex.
            val urlRegex = """var\s*url\s*=\s*'(https?://.*?)'""".toRegex()
            val intermediateUrl = urlRegex.find(htmlContent)?.groupValues?.get(1)

            if (intermediateUrl == null) {
                println("VCLOUD_EXTRACTOR: Could not find the intermediate URL in the script.")
                return emptyList()
            }

            // Step 2: Follow the intermediate URL to get the final page's HTML.
            val finalPageHtml = client.get(intermediateUrl).bodyAsText()

            // --- STEP 3: Find ALL relevant links using multiple Regex patterns ---

            // Create a list to hold all the links we find.
            val foundLinks = mutableListOf<VideoFile>()

            // Regex for .m3u8 streaming links
            val m3u8Regex = """(https?://[^\s'"]+\.m3u8[^\s'"]*)""".toRegex()
            m3u8Regex.findAll(finalPageHtml).forEach { match ->
                foundLinks.add(VideoFile(url = match.value, quality = "HLS Stream"))
            }

            // Regex for .mkv direct download links
            val mkvRegex = """(https?://[^\s'"]+\.mkv)""".toRegex()
            mkvRegex.findAll(finalPageHtml).forEach { match ->
                foundLinks.add(VideoFile(url = match.value, quality = "Direct Download (MKV)"))
            }

            // Regex for pixeldrain.dev API links
            val pixeldrainRegex = """(https?://pixeldrain\.dev/api/file/[^\s'"]+)""".toRegex()
            pixeldrainRegex.findAll(finalPageHtml).forEach { match ->
                foundLinks.add(VideoFile(url = match.value, quality = "Pixeldrain Link"))
            }

            // Return the combined list of all found links.
            foundLinks

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}