package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Calls", Icons.Default.GraphicEq)
    object Clients : Screen("clients", "Clients", Icons.Default.Business)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object ActiveRecord : Screen("active_record", "Record", Icons.Default.Mic)
    object CallDetail : Screen("call_detail/{callId}", "Detail", Icons.Default.Info) {
        fun createRoute(callId: Long) = "call_detail/$callId"
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Clients,
        Screen.Analytics,
        Screen.Settings
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = SleekNavBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder.copy(alpha = 0.6f))
                ) {
                    NavigationBar(
                        containerColor = SleekNavBackground,
                        contentColor = SleekOnPrimaryContainer,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(76.dp)
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (selected) SleekOnPrimaryContainer else SleekTextMuted
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                        letterSpacing = (-0.2).sp,
                                        color = if (selected) SleekOnPrimaryContainer else SleekTextMuted
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = SleekPrimaryContainer,
                                    selectedIconColor = SleekOnPrimaryContainer,
                                    unselectedIconColor = SleekTextMuted,
                                    selectedTextColor = SleekOnPrimaryContainer,
                                    unselectedTextColor = SleekTextMuted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToActiveRecord = {
                        navController.navigate(Screen.ActiveRecord.route)
                    },
                    onNavigateToCallDetail = { callId ->
                        navController.navigate(Screen.CallDetail.createRoute(callId))
                    }
                )
            }

            composable(Screen.Clients.route) {
                ClientsScreen(
                    viewModel = viewModel,
                    onStartCallWithClient = { client ->
                        viewModel.startLiveRecording(
                            clientName = client.name,
                            clientCompany = client.company,
                            phone = client.phone
                        )
                        navController.navigate(Screen.ActiveRecord.route)
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }

            composable(Screen.ActiveRecord.route) {
                ActiveRecordingScreen(
                    viewModel = viewModel,
                    onFinishRecording = { savedId ->
                        navController.navigate(Screen.CallDetail.createRoute(savedId)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onDiscard = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.CallDetail.route,
                arguments = listOf(navArgument("callId") { type = NavType.LongType })
            ) { backStackEntry ->
                val callId = backStackEntry.arguments?.getLong("callId") ?: 0L
                viewModel.selectedCallId.value = callId
                CallDetailScreen(
                    callId = callId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
