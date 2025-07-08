package com.example.mediafinder.api

// Holds the final, playable video file URL and its quality.
data class VideoFile(
    val url: String,
    val quality: String
)

// The interface that all extractors (e.g., Vcloud, Doodstream) must implement.
interface Extractor {
    // The name of the extractor, e.g., "Vcloud"
    val name: String

    // The main URL to identify which extractor to use, e.g., "vcloud.lol"
    val mainUrl: String

    // The function that takes a link and returns the final video files.
    suspend fun extract(url: String): List<VideoFile>
}