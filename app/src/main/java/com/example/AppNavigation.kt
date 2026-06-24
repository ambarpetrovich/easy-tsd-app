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

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Filled.Home)
    object Scan : Screen("scan?sessionId={sessionId}", R.string.nav_scan, Icons.Filled.QrCodeScanner) {
        fun createRoute(sessionId: String? = null): String {
            return if (sessionId != null) "scan?sessionId=$sessionId" else "scan"
        }
    }
    object Files : Screen("files", R.string.nav_files, Icons.Filled.Folder)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings)
    object History : Screen("history", R.string.nav_history, Icons.Filled.Settings)
    object Products : Screen("products?action={action}&gtin={gtin}", R.string.nav_products, Icons.Filled.Settings) {
        fun createRoute(action: String? = null, gtin: String? = null): String {
            if (action == null && gtin == null) return "products"
            return "products?action=${action ?: ""}&gtin=${gtin ?: ""}"
        }
    }
    object PdfSessions : Screen("pdf_sessions", R.string.nav_pdf_sessions, Icons.Filled.PictureAsPdf)
    object CreatePdfSession : Screen("create_pdf_session", R.string.nav_create_pdf_session, Icons.Filled.PictureAsPdf)
    object PdfSessionDetails : Screen("pdf_session_details/{sessionId}", R.string.nav_pdf_session_details, Icons.Filled.PictureAsPdf) {
        fun createRoute(sessionId: String) = "pdf_session_details/$sessionId"
    }
    object ImportSessions : Screen("import_sessions", R.string.nav_import_sessions, Icons.Filled.Description)
    object CreateImportSession : Screen("create_import_session", R.string.nav_create_import_session, Icons.Filled.Description)
    object ImportSessionDetails : Screen("import_session_details/{sessionId}", R.string.nav_import_session_details, Icons.Filled.Description) {
        fun createRoute(sessionId: String) = "import_session_details/$sessionId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val pdfSessionViewModel: PdfSessionViewModel = viewModel()
    val importSessionViewModel: ImportSessionViewModel = viewModel()
    val accountingViewModel: AccountingViewModel = viewModel()
    val productsViewModel: ProductsViewModel = viewModel()
    val scanSessionViewModel: ScanSessionViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    
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
                        icon = { Icon(screen.icon, contentDescription = androidx.compose.ui.res.stringResource(screen.titleRes)) },
                        label = { Text(androidx.compose.ui.res.stringResource(screen.titleRes)) },
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
                    pdfSessionViewModel = pdfSessionViewModel,
                    importSessionViewModel = importSessionViewModel,
                    accountingViewModel = accountingViewModel,
                    scanSessionViewModel = scanSessionViewModel,
                    onNavigateToScan = { sessionId -> navController.navigate(Screen.Scan.createRoute(sessionId)) },
                    onNavigateToPdfSession = { sessionId -> navController.navigate(Screen.PdfSessionDetails.createRoute(sessionId)) },
                    onNavigateToImportSession = { sessionId -> navController.navigate(Screen.ImportSessionDetails.createRoute(sessionId)) }
                ) 
            }
            composable(
                route = Screen.Scan.route,
                arguments = listOf(androidx.navigation.navArgument("sessionId") { type = androidx.navigation.NavType.StringType; nullable = true })
            ) { backStackEntry -> 
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                ScanScreen(
                    scanViewModel = scanSessionViewModel,
                    accountingViewModel = accountingViewModel,
                    productsViewModel = productsViewModel,
                    settingsViewModel = settingsViewModel,
                    initialSessionId = sessionId
                ) 
            }
            composable(Screen.Files.route) { FilesScreen(
                onNavigateToPdfSessions = { navController.navigate(Screen.PdfSessions.route) },
                onNavigateToImportSessions = { navController.navigate(Screen.ImportSessions.route) }
            ) }
            composable(Screen.Settings.route) { SettingsScreen(
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToProducts = { navController.navigate(Screen.Products.createRoute()) },
                settingsViewModel = settingsViewModel
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
                    initialGtin = gtin,
                    viewModel = productsViewModel
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
                PdfSessionDetailsScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                    onNavigateToProduct = { gtin, action ->
                        navController.navigate(Screen.Products.createRoute(action, gtin))
                    },
                    viewModel = pdfSessionViewModel,
                    productsViewModel = productsViewModel,
                    accountingViewModel = accountingViewModel,
                    settingsViewModel = settingsViewModel
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
                ImportSessionDetailsScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                    onNavigateToProduct = { gtin, action ->
                        navController.navigate(Screen.Products.createRoute(action, gtin))
                    },
                    viewModel = importSessionViewModel,
                    productsViewModel = productsViewModel,
                    accountingViewModel = accountingViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
