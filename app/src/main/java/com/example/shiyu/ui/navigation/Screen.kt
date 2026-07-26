package com.example.shiyu.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Articles : Screen("articles", "文章", Icons.AutoMirrored.Filled.MenuBook)
    object ArticleReader : Screen("article_reader", "阅读器", Icons.Default.MenuBook)
    object Vocabulary : Screen("vocabulary", "生词本", Icons.Default.Translate)
    object Sentences : Screen("sentences", "句法本", Icons.Default.Notes)
    object Review : Screen("review", "FSRS复习", Icons.Default.Psychology)
    object Translate : Screen("translate", "AI 翻译", Icons.Default.AutoAwesome)
    object EpubImport : Screen("epub_import", "文本导入", Icons.Default.FileUpload)
    object DataManager : Screen("data_manager", "数据管理", Icons.Default.Storage)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Articles,
    Screen.Vocabulary,
    Screen.Sentences,
    Screen.Review
)
