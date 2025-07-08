package com.example.mediafinder.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediafinder.api.SearchResult
import com.example.mediafinder.providers.ProviderManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // This Job object helps us cancel a previous search if the user types too fast.
    private var searchJob: Job? = null

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        // Cancel any previously running search.
        searchJob?.cancel()

        // Don't search if the query is too short.
        if (newQuery.length < 3) {
            _searchResults.value = emptyList()
            return
        }

        // Launch a new search job.
        searchJob = viewModelScope.launch {
            // Add a small delay so we don't search on every single letter typed.
            delay(300)

            _isLoading.value = true
            // Clear previous results before starting a new search.
            _searchResults.value = emptyList()

            // Call the provider and store the results in a local variable.
            val results = ProviderManager.search(newQuery)

            // --- THIS IS THE NEW DEBUGGING PART ---
            println("VIEWMODEL_DEBUG: Search returned ${results.size} items from ProviderManager.")

            // Update the UI state with the new results.
            _searchResults.value = results

            println("VIEWMODEL_DEBUG: _searchResults state updated. It now has ${_searchResults.value.size} items.")
            // --- END OF DEBUGGING PART ---

            _isLoading.value = false
        }
    }

    // We no longer need the separate performSearch() function.

    fun onSearchResultClicked(result: SearchResult) {
        viewModelScope.launch {
            println("Clicked on: ${result.title}. Fetching details...")
            val provider = ProviderManager.getProvider("VegaMovies")
            val loadData = provider?.load(result.url)

            if (loadData != null) {
                println("Successfully loaded data for: ${loadData.title}")
                println("Description: ${loadData.description}")
                println("Found ${loadData.videoLinks.size} video links:")
                loadData.videoLinks.forEach {
                    println("  - ${it.name}: ${it.url}")
                }
            } else {
                println("Failed to load data for ${result.title}")
            }
        }
    }
}