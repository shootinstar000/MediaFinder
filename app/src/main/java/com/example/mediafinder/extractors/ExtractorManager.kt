package com.example.mediafinder.extractors

import com.example.mediafinder.api.Extractor
import com.example.mediafinder.api.VideoFile

object ExtractorManager {
    // A list of all available extractors.
    private val allExtractors: List<Extractor> = listOf(
        VcloudExtractor(),   // The one for vcloud.lol
        FastDlExtractor(),
        HubCloudExtractor()// Our new, special one for fastdl.icu
    )

    // The rest of the file remains the same.
    suspend fun extract(url: String): List<VideoFile> {
        val extractor = allExtractors.find { url.contains(it.mainUrl) }
        return if (extractor != null) {
            println("EXTRACTOR_DEBUG: Using extractor '${extractor.name}' for URL: $url")
            try {
                extractor.extract(url)
            } catch (e: Exception) {
                println("EXTRACTOR_DEBUG: Extractor '${extractor.name}' failed.")
                e.printStackTrace()
                emptyList()
            }
        } else {
            println("EXTRACTOR_DEBUG: No suitable extractor found for URL: $url")
            emptyList()
        }
    }
}