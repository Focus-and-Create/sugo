package com.seogoapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seogoapp.ui.drawer.DrawerScreen
import com.seogoapp.ui.library.LibraryScreen
import com.seogoapp.ui.viewer.ViewerScreen

private object Routes {
    const val LIBRARY = "library"
    const val DRAWER  = "drawer/{folderId}"
    const val VIEWER  = "viewer/{sceneId}"

    fun drawer(folderId: Long) = "drawer/$folderId"
    fun viewer(sceneId: String) = "viewer/$sceneId"
}

@Composable
fun SeogoNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY
    ) {
        // ① 서고 (홈)
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onFolderClick = { folderId ->
                    navController.navigate(Routes.drawer(folderId))
                },
                onSceneClick = { sceneId ->
                    navController.navigate(Routes.viewer(sceneId))
                }
            )
        }

        // ② 서랍
        composable(
            route = Routes.DRAWER,
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType }
            )
        ) { backStack ->
            val folderId = backStack.arguments!!.getLong("folderId")
            DrawerScreen(
                folderId = folderId,
                onBack = { navController.popBackStack() },
                onSceneClick = { sceneId ->
                    navController.navigate(Routes.viewer(sceneId))
                }
            )
        }

        // ③ 씬 뷰어
        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("sceneId") { type = NavType.StringType }
            )
        ) { backStack ->
            val sceneId = backStack.arguments!!.getString("sceneId")!!
            ViewerScreen(
                sceneId = sceneId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
