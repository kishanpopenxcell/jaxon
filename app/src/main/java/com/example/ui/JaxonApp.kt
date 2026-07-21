package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CustomCommandsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VoiceAssistantOverlay
import com.example.ui.theme.DeepGray
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.SpaceBlack
import com.example.ui.viewmodel.JaxonViewModel

@Composable
fun JaxonApp(
    viewModel: JaxonViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tabs inside the M3 bottom bar
    val navItems = listOf(
        NavItem("home", "Jaxon", Icons.Default.Mic, "nav_tab_home"),
        NavItem("custom_commands", "Routines", Icons.Default.Tune, "nav_tab_routines"),
        NavItem("history", "History", Icons.Default.History, "nav_tab_history"),
        NavItem("settings", "Settings", Icons.Default.Settings, "nav_tab_settings")
    )

    // Hide navigation bar during Splash, Onboarding or Permissions setup
    val showBottomBar = currentRoute in listOf("home", "custom_commands", "history", "settings")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpaceBlack,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DeepGray,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("app_navigation_bar")
                ) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) Color.Black else Color.White.copy(alpha = 0.6f)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = if (selected) GlowCyan else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = GlowCyan
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("splash") {
                    SplashScreen(navController = navController, viewModel = viewModel)
                }
                composable("onboarding") {
                    OnboardingScreen(navController = navController, viewModel = viewModel)
                }
                composable("permissions") {
                    PermissionsScreen(navController = navController, viewModel = viewModel)
                }
                composable("home") {
                    HomeScreen(navController = navController, viewModel = viewModel)
                }
                composable("custom_commands") {
                    CustomCommandsScreen(navController = navController, viewModel = viewModel)
                }
                composable("history") {
                    HistoryScreen(navController = navController, viewModel = viewModel)
                }
                composable("settings") {
                    SettingsScreen(navController = navController, viewModel = viewModel)
                }
            }

            // Layered Speech Listening Wave Overlay (Slides up over any screen when activated)
            VoiceAssistantOverlay(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)
