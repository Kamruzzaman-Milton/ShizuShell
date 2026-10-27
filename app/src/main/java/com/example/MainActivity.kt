package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PackageManagerScreen
import com.example.ui.screens.PermissionGrantScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SnippetsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.ShizuShellTheme
import com.example.ui.viewmodel.ShellViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Terminal : Screen("terminal", "Shell", Icons.Default.Terminal)
    object History : Screen("history", "History", Icons.Default.History)
    object Snippets : Screen("snippets", "Snippets", Icons.Default.Bookmark)
    object Packages : Screen("packages", "Packages", Icons.Default.Apps)
    object Permissions : Screen("permissions", "Perms", Icons.Default.Security)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val shellViewModel: ShellViewModel = viewModel()
            val preferences by shellViewModel.preferences.collectAsState()

            ShizuShellTheme(themeMode = preferences.themeMode) {
                MainAppContent(viewModel = shellViewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: ShellViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val bottomNavItems = listOf(
        Screen.Terminal,
        Screen.History,
        Screen.Snippets,
        Screen.Packages,
        Screen.Permissions,
        Screen.Settings
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Adaptive wide layout: NavigationRail on side
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationRailItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavigationHost(
                            navController = navController,
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState
                        )
                    }
                }
            }
        } else {
            // Standard handheld layout: NavigationBar at bottom
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    AppNavigationHost(
                        navController = navController,
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavigationHost(
    navController: androidx.navigation.NavHostController,
    viewModel: ShellViewModel,
    snackbarHostState: SnackbarHostState
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Terminal.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Terminal.route) {
            TerminalScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onNavigateToTerminal = {
                    navController.navigate(Screen.Terminal.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Snippets.route) {
            SnippetsScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onNavigateToTerminal = {
                    navController.navigate(Screen.Terminal.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Packages.route) {
            PackageManagerScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onNavigateToTerminal = {
                    navController.navigate(Screen.Terminal.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Permissions.route) {
            PermissionGrantScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onNavigateToTerminal = {
                    navController.navigate(Screen.Terminal.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
