package com.example.mediafinder.api

// Add the optional 'season' parameter
data class VideoLink(
    val name: String,
    val url: String,
    val season: Int? = null // e.g., 1, 2. Null if it's a movie.
)

// No changes needed for LoadData
data class LoadData(
    val title: String,
    val description: String,
    val posterUrl: String,
    val videoLinks: List<VideoLink>
)