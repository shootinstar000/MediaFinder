package com.example.mediafinder.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mediafinder.api.LoadData

@Composable
fun DetailsScreen(
    url: String,
    onPlayVideoClicked: (String) -> Unit,
    viewModel: DetailsViewModel = viewModel()
) {
    LaunchedEffect(key1 = url) {
        viewModel.loadMovieDetails(url)
    }

    val details: LoadData? by viewModel.movieDetails.collectAsState()
    val isLoading: Boolean by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (details != null) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                AsyncImage(
                    model = details!!.posterUrl,
                    contentDescription = details!!.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(details!!.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(details!!.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                Text(
                    "Video Links",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(details!!.videoLinks) { link ->
                Button(
                    onClick = {
                        viewModel.onVideoLinkClicked(link) { finalVideoUrl ->
                            onPlayVideoClicked(finalVideoUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(link.name)
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load details.")
        }
    }
}