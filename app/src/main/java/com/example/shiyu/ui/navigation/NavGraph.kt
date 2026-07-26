package com.example.shiyu.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shiyu.ui.screens.*
import com.example.shiyu.ui.theme.PrimaryGreen
import com.example.shiyu.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun NavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val toastMessage by viewModel.toastMessage.collectAsState()
    var activeToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            activeToastMessage = msg
            delay(2400)
            activeToastMessage = null
            viewModel.clearToast()
        }
    }

    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        bottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
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
                                label = { Text(screen.title) },
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .background(Color.White),
                enterTransition = {
                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(initialOffsetX = { 120 }, animationSpec = tween(280, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(targetOffsetX = { -120 }, animationSpec = tween(280, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(initialOffsetX = { -120 }, animationSpec = tween(280, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(targetOffsetX = { 120 }, animationSpec = tween(280, easing = FastOutSlowInEasing))
                }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Articles.route) {
                    ArticlesScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.ArticleReader.route) {
                    ArticleReaderScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Vocabulary.route) {
                    VocabularyScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Sentences.route) {
                    SentencesScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Review.route) {
                    ReviewScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Translate.route) {
                    TranslateScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.EpubImport.route) {
                    EpubImportScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.DataManager.route) {
                    DataManagerScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController = navController, viewModel = viewModel)
                }
            }
        }

        // Fresh Green App Toast Overlay Banner
        AnimatedVisibility(
            visible = activeToastMessage != null,
            enter = slideInVertically(initialOffsetY = { -120 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -120 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = activeToastMessage ?: "",
                        color = Color(0xFF14532D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
