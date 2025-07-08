package com.example.mediafinder.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediafinder.api.LoadData
import com.example.mediafinder.api.VideoLink
import com.example.mediafinder.extractors.ExtractorManager
import com.example.mediafinder.providers.ProviderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel : ViewModel() {
    private val _movieDetails = MutableStateFlow<LoadData?>(null)
    val movieDetails = _movieDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadMovieDetails(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val provider = ProviderManager.getProvider("VegaMovies")
            _movieDetails.value = provider?.load(url)
            _isLoading.value = false
        }
    }

    fun onVideoLinkClicked(link: VideoLink, onUrlReady: (String) -> Unit) {
        viewModelScope.launch {
            println("DETAILS_DEBUG: Clicked on link: ${link.name} -> ${link.url}")
            val finalFiles = ExtractorManager.extract(link.url)

            if (finalFiles.isNotEmpty()) {
                val finalUrl = finalFiles.first().url
                println("DETAILS_DEBUG: SUCCESS! Found final video file: $finalUrl")
                onUrlReady(finalUrl)
            } else {
                println("DETAILS_DEBUG: FAILURE. Extractor found no video files.")
            }
        }
    }
}