package com.example.mediafinder.providers

import com.example.mediafinder.api.Provider
import com.example.mediafinder.api.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProviderManager {
    private val allProviders: List<Provider> = listOf(
        VegaMoviesProvider()
    )

    // This is the new, simplified search function.
    suspend fun search(query: String): List<SearchResult> {
        // We still run everything on a background thread.
        return withContext(Dispatchers.IO) {
            val allResults = mutableListOf<SearchResult>()

            // Instead of complex parallel execution, we now simply
            // loop through each provider one by one.
            for (provider in allProviders) {
                try {
                    // Directly call the provider's search function.
                    val resultsFromProvider = provider.search(query)

                    // --- NEW DEBUG LINE ---
                    println("PROVIDER_MANAGER_DEBUG: Provider '${provider.name}' returned ${resultsFromProvider.size} items.")

                    // Add the results to our master list.
                    allResults.addAll(resultsFromProvider)

                } catch (e: Exception) {
                    println("PROVIDER_MANAGER_DEBUG: Provider '${provider.name}' failed with an exception.")
                    e.printStackTrace()
                }
            }

            println("PROVIDER_MANAGER_DEBUG: Total results from all providers: ${allResults.size}.")
            allResults
        }
    }

    fun getProvider(name: String): Provider? {
        return allProviders.find { it.name.equals(name, ignoreCase = true) }
    }
}