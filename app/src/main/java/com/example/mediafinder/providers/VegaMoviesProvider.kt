package com.example.mediafinder.providers

import com.example.mediafinder.api.*
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup

class VegaMoviesProvider : Provider {
    override val name: String = "VegaMovies"
    private val mainUrl = ApiUrls.VEGA_MOVIES
    private val client = HttpClient(Android)

    // The search function is working perfectly. No changes needed.
    override suspend fun search(query: String): List<SearchResult> {
        try {
            val searchUrl = "$mainUrl/page/1/?s=$query"
            val response = client.get(searchUrl) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0")
                header("Referer", "$mainUrl/")
            }
            val htmlContent = response.bodyAsText()
            val document = Jsoup.parse(htmlContent)
            val articles = document.select("article")
            return articles.mapNotNull { article ->
                val titleElement = article.selectFirst("h2.post-title a")
                val imageElement = article.selectFirst("div.post-thumbnail img")
                if (titleElement != null && imageElement != null) {
                    val title = titleElement.text()
                    val href = titleElement.attr("href")
                    val posterUrl = imageElement.attr("src")
                    if (posterUrl.isNotBlank()) {
                        SearchResult(title, posterUrl, href)
                    } else { null }
                } else { null }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    // THIS IS THE FINAL, WORKING `load` FUNCTION
    override suspend fun load(url: String): LoadData? {
        try {
            // --- STEP 1: Get the main movie page (This part is already working) ---
            val mainPageResponse = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0")
                header("Referer", "$mainUrl/")
            }
            val mainPageHtml = mainPageResponse.bodyAsText()
            val mainPageDocument = Jsoup.parse(mainPageHtml)

            val title = mainPageDocument.selectFirst("meta[property=og:title]")?.attr("content") ?: "No Title"
            val posterUrl = mainPageDocument.selectFirst("meta[property=og:image]")?.attr("content") ?: ""
            val contentDiv = mainPageDocument.selectFirst(".entry-content, .entry-inner")
            val description = contentDiv?.selectFirst("h3:contains(Series-SYNOPSIS/PLOT:)")
                ?.nextElementSibling()?.text() ?: "No Description"

            // --- STEP 2: Find all intermediate links and follow them (The new logic) ---
            val buttonLinks = contentDiv?.select("a:has(button)") ?: emptyList()

            // We use mapNotNull which allows us to process each link and discard any that fail.
            val videoLinks = buttonLinks.mapNotNull { buttonLink ->
                try {
                    val intermediateUrl = buttonLink.attr("href")
                    val linkName = buttonLink.text() // e.g., "⚡ V-Cloud [Resumable] ⚡"

                    // Make the SECOND network request to the intermediate page
                    val secondPageHtml = client.get(intermediateUrl) {
                        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0")
                        header("Referer", mainUrl)
                    }.bodyAsText()

                    val secondPageDoc = Jsoup.parse(secondPageHtml)

                    // Now, search this SECOND page for the final video link
                    val finalUrl = secondPageDoc.select("a").firstOrNull { a ->
                        val href = a.attr("href")
                        // Look for the real video hosts, as seen in the reference code
                        href.contains("vcloud.lol") || href.contains("fastdl.icu") || href.contains("dood")
                    }?.attr("href")

                    // If we found a final URL, create the VideoLink object.
                    if (finalUrl != null) {
                        VideoLink(name = linkName, url = finalUrl)
                    } else {
                        null // Otherwise, discard this link.
                    }
                } catch (e: Exception) {
                    // If following an intermediate link fails, print the error and discard it.
                    e.printStackTrace()
                    null
                }
            }

            return LoadData(title, description, posterUrl, videoLinks)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}