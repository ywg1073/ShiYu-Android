package com.example.shiyu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.shiyu.ui.navigation.Screen
import com.example.shiyu.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val articles by viewModel.articles.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = remember(articles) {
        listOf("全部") + articles.mapNotNull { it.category }.distinct()
    }

    val filteredArticles = remember(articles, searchQuery, selectedCategory) {
        articles.filter { article ->
            val matchesQuery = searchQuery.isBlank() ||
                    article.title.contains(searchQuery, ignoreCase = true) ||
                    article.content.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "全部" || article.category == selectedCategory
            matchesQuery && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文章库", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_article_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建文章")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_article_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建文章")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("articles_search_input"),
                placeholder = { Text("搜索文章标题或正文...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("category_chip_$cat")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("未查找到匹配文章", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredArticles, key = { it.id }) { article ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrentArticle(article)
                                    navController.navigate(Screen.ArticleReader.route)
                                }
                                .testTag("article_card_${article.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = article.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteArticle(article.id) },
                                        modifier = Modifier.testTag("delete_article_${article.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除文章", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = article.content,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("字数: ${article.wordCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    article.category?.let { cat ->
                                        Text("分类: $cat", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddArticleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, author, category, description ->
                viewModel.addArticle(title, content, author, category, description)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddArticleDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, author: String?, category: String?, description: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Essay") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建阅读文章") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("文章标题") },
                    modifier = Modifier.fillMaxWidth().testTag("add_article_title_input")
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("作者（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("文章正文（英文）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("add_article_content_input"),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onConfirm(title, content, author.ifBlank { null }, category.ifBlank { null }, null)
                    }
                },
                modifier = Modifier.testTag("confirm_add_article_button")
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
