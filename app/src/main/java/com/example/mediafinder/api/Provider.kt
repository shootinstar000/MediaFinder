package com.example.mediafinder.api

// This holds the data for a single search result
data class SearchResult(
    val title: String,
    val posterUrl: String,
    val url: String
)

// The interface that all extensions (providers) must implement.
interface Provider {
    val name: String
    suspend fun search(query: String): List<SearchResult>

    // Add this new function. It takes a URL from a SearchResult
    // and returns our new LoadData object.
    suspend fun load(url: String): LoadData?
}