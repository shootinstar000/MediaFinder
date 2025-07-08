package com.example.mediafinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mediafinder.ui.details.DetailsScreen
import com.example.mediafinder.ui.player.PlayerScreen
import com.example.mediafinder.ui.search.SearchScreen
import com.example.mediafinder.ui.theme.MediaFinderTheme
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaFinderTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "search") {
        // Search Screen Route
        composable("search") {
            SearchScreen(
                onResultClicked = { url ->
                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                    navController.navigate("details/$encodedUrl")
                }
            )
        }

        // Details Screen Route
        composable(
            route = "details/{movieUrl}",
            arguments = listOf(navArgument("movieUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("movieUrl") ?: ""
            val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
            DetailsScreen(
                url = decodedUrl,
                onPlayVideoClicked = { videoUrl ->
                    val encodedVideoUrl = URLEncoder.encode(videoUrl, "UTF-8")
                    navController.navigate("player/$encodedVideoUrl")
                }
            )
        }

        // Player Screen Route
        composable(
            route = "player/{videoUrl}",
            arguments = listOf(navArgument("videoUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
            val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
            PlayerScreen(videoUrl = decodedUrl)
        }
    }
}