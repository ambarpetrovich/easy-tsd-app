package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Начало", Icons.Filled.Home)
    object Scan : Screen("scan", "Скан", Icons.Filled.QrCodeScanner)
    object Files : Screen("files", "Файлы", Icons.Filled.Folder)
    object Settings : Screen("settings", "Настройки", Icons.Filled.Settings)
    object History : Screen("history", "История версий", Icons.Filled.Settings)
    object Products : Screen("products?action={action}&gtin={gtin}", "Товары", Icons.Filled.Settings) {
        fun createRoute(action: String? = null, gtin: String? = null): String {
            if (action == null && gtin == null) return "products"
            return "products?action=${action ?: ""}&gtin=${gtin ?: ""}"
        }
    }
    object PdfSessions : Screen("pdf_sessions", "Распознавание PDF", Icons.Filled.PictureAsPdf)
    object CreatePdfSession : Screen("create_pdf_session", "Новая сессия", Icons.Filled.PictureAsPdf)
    object PdfSessionDetails : Screen("pdf_session_details/{sessionId}", "Детали сессии", Icons.Filled.PictureAsPdf) {
        fun createRoute(sessionId: String) = "pdf_session_details/$sessionId"
    }
    object ImportSessions : Screen("import_sessions", "Импорт данных", Icons.Filled.Description)
    object CreateImportSession : Screen("create_import_session", "Новая сессия импорта", Icons.Filled.Description)
    object ImportSessionDetails : Screen("import_session_details/{sessionId}", "Детали импорта", Icons.Filled.Description) {
        fun createRoute(sessionId: String) = "import_session_details/$sessionId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val pdfSessionViewModel: PdfSessionViewModel = viewModel()
    val importSessionViewModel: ImportSessionViewModel = viewModel()
    
    val bottomBarItems = listOf(
        Screen.Home,
        Screen.Scan,
        Screen.Files,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomBarItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateToPdfSession = { sessionId -> navController.navigate(Screen.PdfSessionDetails.createRoute(sessionId)) },
                    onNavigateToImportSession = { sessionId -> navController.navigate(Screen.ImportSessionDetails.createRoute(sessionId)) }
                ) 
            }
            composable(Screen.Scan.route) { ScanScreen() }
            composable(Screen.Files.route) { FilesScreen(
                onNavigateToPdfSessions = { navController.navigate(Screen.PdfSessions.route) },
                onNavigateToImportSessions = { navController.navigate(Screen.ImportSessions.route) }
            ) }
            composable(Screen.Settings.route) { SettingsScreen(
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProducts = { navController.navigate(Screen.Products.createRoute()) }
            ) }
            composable(Screen.History.route) { HistoryScreen(
                onBack = { navController.popBackStack() }
            ) }
            composable(
                route = Screen.Products.route,
                arguments = listOf(
                    androidx.navigation.navArgument("action") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null },
                    androidx.navigation.navArgument("gtin") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry -> 
                val action = backStackEntry.arguments?.getString("action")?.takeIf { it.isNotBlank() }
                val gtin = backStackEntry.arguments?.getString("gtin")?.takeIf { it.isNotBlank() }
                ProductsScreen(
                    onBack = { navController.popBackStack() },
                    initialAction = action,
                    initialGtin = gtin
                ) 
            }
            composable(Screen.PdfSessions.route) { PdfRecognitionScreen(
                onBack = { navController.popBackStack() },
                onCreateSession = { navController.navigate(Screen.CreatePdfSession.route) },
                onSessionClick = { sessionId -> navController.navigate(Screen.PdfSessionDetails.createRoute(sessionId)) },
                viewModel = pdfSessionViewModel
            ) }
            composable(Screen.CreatePdfSession.route) { CreatePdfSessionScreen(
                onBack = { navController.popBackStack() },
                viewModel = pdfSessionViewModel
            ) }
            composable(Screen.PdfSessionDetails.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                val productsViewModel: ProductsViewModel = viewModel()
                PdfSessionDetailsScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                    onNavigateToProduct = { gtin, action ->
                        navController.navigate(Screen.Products.createRoute(action, gtin))
                    },
                    viewModel = pdfSessionViewModel,
                    productsViewModel = productsViewModel
                )
            }
            composable(Screen.ImportSessions.route) { ImportRecognitionScreen(
                onBack = { navController.popBackStack() },
                onCreateSession = { navController.navigate(Screen.CreateImportSession.route) },
                onSessionClick = { sessionId -> navController.navigate(Screen.ImportSessionDetails.createRoute(sessionId)) },
                viewModel = importSessionViewModel
            ) }
            composable(Screen.CreateImportSession.route) { CreateImportSessionScreen(
                onBack = { navController.popBackStack() },
                viewModel = importSessionViewModel
            ) }
            composable(Screen.ImportSessionDetails.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                val productsViewModel: ProductsViewModel = viewModel()
                ImportSessionDetailsScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                    onNavigateToProduct = { gtin, action ->
                        navController.navigate(Screen.Products.createRoute(action, gtin))
                    },
                    viewModel = importSessionViewModel,
                    productsViewModel = productsViewModel
                )
            }
        }
    }
}
