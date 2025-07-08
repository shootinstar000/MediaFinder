package com.example.mediafinder.extractors

import com.example.mediafinder.api.Extractor
import com.example.mediafinder.api.VideoFile // FIX: Uses your project's VideoFile data class
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import java.util.Base64

// FIX: This class now correctly implements your project's Extractor interface
class HubCloudExtractor : Extractor {
    override val name: String = "vcloud" // Name used to match "vcloud.lol" links
    override val mainUrl: String = "https://vcloud.lol" // FIX: Added the required mainUrl property

    private val client = HttpClient(Android) {
        followRedirects = true // Automatically follow redirects
    }

    // FIX: Function signature now perfectly matches your Extractor.kt interface
    override suspend fun extract(url: String): List<VideoFile> {
        println("--- HubCloudExtractor: START ---")
        try {
            val baseUrl = url.split('/').take(3).joinToString("/")
            // FIX: The list now correctly holds your VideoFile objects
            val streamLinks = mutableListOf<VideoFile>()

            // Step 1 & 2: Get initial page and find the intermediate link
            println("HubCloudExtractor: Fetching initial page -> $url")
            val vLinkText = client.get(url).bodyAsText()

            var vcloudLink: String? = null
            val regex = Regex("""var\s+url\s*=\s*'([^']+)';""")
            val scriptUrl = regex.find(vLinkText)?.groupValues?.getOrNull(1)

            if (scriptUrl != null) {
                val encodedPart = scriptUrl.substringAfter("r=", "")
                if (encodedPart.isNotBlank()) {
                    vcloudLink = try { String(Base64.getDecoder().decode(encodedPart)) } catch (e: Exception) { null }
                }
                if (vcloudLink == null) vcloudLink = scriptUrl
            }

            if (vcloudLink == null) {
                println("HubCloudExtractor: JS variable not found, trying HTML fallback...")
                val doc = Jsoup.parse(vLinkText)
                vcloudLink = doc.selectFirst(".fa-file-download.fa-lg")?.parent()?.attr("href")
            }

            if (vcloudLink == null) {
                println("HubCloudExtractor: FAILED to find intermediate vcloudLink.")
                return emptyList() // Return an empty list on failure
            }

            if (vcloudLink.startsWith("/")) vcloudLink = "$baseUrl$vcloudLink"
            println("HubCloudExtractor: Found intermediate link -> $vcloudLink")

            // Step 3: Get the final download links page
            val vcloudText = client.get(vcloudLink).bodyAsText()
            val doc = Jsoup.parse(vcloudText)

            // Step 4: Find and categorize all the download buttons
            val linkElements = doc.select(".btn-success.btn-lg.h6, .btn-danger, .btn-secondary")
            println("HubCloudExtractor: Found ${linkElements.size} final download buttons to analyze.")

            for (element in linkElements) {
                val link = element.attr("href") ?: continue
                when {
                    link.contains(".dev") && !link.contains("/?id=") -> streamLinks.add(VideoFile(link, "Cf Worker"))
                    link.contains("pixeld") -> {
                        val finalLink = if (!link.contains("api")) {
                            val token = link.split('/').lastOrNull()
                            val pixeldBaseUrl = link.split('/').dropLast(2).joinToString("/")
                            "$pixeldBaseUrl/api/file/$token?download"
                        } else link
                        streamLinks.add(VideoFile(finalLink, "Pixeldrain"))
                    }
                    link.contains("hubcloud") || link.contains("/?id=") -> {
                        try {
                            val finalRedirectResponse = client.get(link)
                            val finalUrl = finalRedirectResponse.request.url.toString()
                            val directLink = finalUrl.substringAfter("link=", finalUrl)
                            streamLinks.add(VideoFile(directLink, "hubcloud"))
                        } catch (e: Exception) { println("HubCloudExtractor: Error during hubcloud redirect: ${e.message}") }
                    }
                    link.contains("cloudflarestorage") -> streamLinks.add(VideoFile(link, "CfStorage"))
                    link.contains("fastdl") -> streamLinks.add(VideoFile(link, "FastDl"))
                    link.contains("hubcdn") -> streamLinks.add(VideoFile(link, "HubCdn"))
                }
            }
            println("HubCloudExtractor: Extracted ${streamLinks.size} final links.")
            println("--- HubCloudExtractor: FINISH ---")
            return streamLinks
        } catch (e: Exception) {
            println("--- HubCloudExtractor: CRITICAL ERROR ---")
            e.printStackTrace()
            return emptyList() // Always return an empty list on critical failure
        }
    }
}