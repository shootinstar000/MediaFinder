package com.example.mediafinder.extractors

import com.example.mediafinder.api.Extractor      // <-- FIX: ADD THIS IMPORT
import com.example.mediafinder.api.VideoSource    // <-- FIX: ADD THIS IMPORT
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import java.util.Base64

class HubCloudExtractor : Extractor {
    override val name: String = "vcloud"
    private val client = HttpClient(Android) {
        followRedirects = true
    }

    // --- FIX: RENAMED FUNCTION FROM 'extractUrl' to 'extract' ---
    override suspend fun extract(url: String): List<VideoSource>? {
        println("--- HubCloudExtractor: START ---")
        try {
            val baseUrl = url.split('/').take(3).joinToString("/")
            val streamLinks = mutableListOf<VideoSource>()

            println("HubCloudExtractor: Fetching initial page -> $url")
            val vLinkText = client.get(url).bodyAsText()

            var vcloudLink: String? = null

            val regex = Regex("""var\s+url\s*=\s*'([^']+)';""")
            val scriptUrl = regex.find(vLinkText)?.groupValues?.getOrNull(1)

            if (scriptUrl != null) {
                val encodedPart = scriptUrl.substringAfter("r=", "")
                if (encodedPart.isNotBlank()) {
                    vcloudLink = try {
                        String(Base64.getDecoder().decode(encodedPart))
                    } catch (e: Exception) { null }
                }
                if (vcloudLink == null) {
                    vcloudLink = scriptUrl
                }
            }

            if (vcloudLink == null) {
                println("HubCloudExtractor: JS variable not found, trying HTML fallback...")
                val doc = Jsoup.parse(vLinkText)
                vcloudLink = doc.selectFirst(".fa-file-download.fa-lg")?.parent()?.attr("href")
            }

            if (vcloudLink == null) {
                println("HubCloudExtractor: FAILED to find intermediate vcloudLink.")
                return null
            }

            if (vcloudLink.startsWith("/")) {
                vcloudLink = "$baseUrl$vcloudLink"
            }

            println("HubCloudExtractor: Found intermediate link -> $vcloudLink")

            val vcloudText = client.get(vcloudLink).bodyAsText()
            val doc = Jsoup.parse(vcloudText)

            val linkElements = doc.select(".btn-success.btn-lg.h6, .btn-danger, .btn-secondary")
            println("HubCloudExtractor: Found ${linkElements.size} final download buttons to analyze.")

            for (element in linkElements) {
                var link = element.attr("href") ?: continue

                when {
                    link.contains(".dev") && !link.contains("/?id=") -> {
                        streamLinks.add(VideoSource(link, "Cf Worker"))
                    }
                    link.contains("pixeld") -> {
                        val finalLink = if (!link.contains("api")) {
                            val token = link.split('/').lastOrNull()
                            val pixeldBaseUrl = link.split('/').dropLast(2).joinToString("/")
                            "$pixeldBaseUrl/api/file/$token?download"
                        } else {
                            link
                        }
                        streamLinks.add(VideoSource(finalLink, "Pixeldrain"))
                    }
                    link.contains("hubcloud") || link.contains("/?id=") -> {
                        try {
                            val finalRedirectResponse = client.get(link)
                            val finalUrl = finalRedirectResponse.request.url.toString()
                            val directLink = finalUrl.substringAfter("link=", finalUrl)
                            streamLinks.add(VideoSource(directLink, "hubcloud"))
                        } catch (e: Exception) {
                            println("HubCloudExtractor: Error during hubcloud redirect: ${e.message}")
                        }
                    }
                    link.contains("cloudflarestorage") -> {
                        streamLinks.add(VideoSource(link, "CfStorage"))
                    }
                    link.contains("fastdl") -> {
                        streamLinks.add(VideoSource(link, "FastDl"))
                    }
                    link.contains("hubcdn") -> {
                        streamLinks.add(VideoSource(link, "HubCdn"))
                    }
                }
            }
            println("HubCloudExtractor: Extracted ${streamLinks.size} final links.")
            println("--- HubCloudExtractor: FINISH ---")
            return streamLinks
        } catch (e: Exception) {
            println("--- HubCloudExtractor: CRITICAL ERROR ---")
            e.printStackTrace()
            return null
        }
    }
}