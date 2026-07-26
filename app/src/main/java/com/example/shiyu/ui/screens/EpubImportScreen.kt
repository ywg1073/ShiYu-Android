package com.example.shiyu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.shiyu.ui.navigation.Screen
import com.example.shiyu.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubImportScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var textTitle by remember { mutableStateOf("") }
    var textAuthor by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文本导入", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Sample Book Loaders
            Text("经典英语短篇快速导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BookImportCard(
                    title = "小王子经典选段",
                    author = "Saint-Exupéry",
                    modifier = Modifier.weight(1f).testTag("sample_book_little_prince")
                ) {
                    textTitle = "The Little Prince - Selected Passages"
                    textAuthor = "Antoine de Saint-Exupéry"
                    textContent = """It is only with the heart that one can see rightly; what is essential is invisible to the eye.

To me, you are still nothing more than a little boy who is just like a hundred thousand other little boys. And I have no need of you. And you, on your part, have no need of me. To you, I am nothing more than a fox like a hundred thousand other foxes. But if you tame me, then we shall need each other. To me, you will be unique in all the world. To you, I shall be unique in all the world..."""
                }

                BookImportCard(
                    title = "傲慢与偏见",
                    author = "Jane Austen",
                    modifier = Modifier.weight(1f).testTag("sample_book_pride")
                ) {
                    textTitle = "Pride and Prejudice - Chapter 1"
                    textAuthor = "Jane Austen"
                    textContent = """It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.

However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful property of some one or other of their daughters..."""
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Paste Custom Text Area
            Text("自定义文本粘贴导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = textTitle,
                onValueChange = { textTitle = it },
                label = { Text("文章 / 图书标题") },
                modifier = Modifier.fillMaxWidth().testTag("import_title_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = textAuthor,
                onValueChange = { textAuthor = it },
                label = { Text("作者名（可选）") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = textContent,
                onValueChange = { textContent = it },
                label = { Text("粘贴纯英文文章正文内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("import_content_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (textTitle.isNotBlank() && textContent.isNotBlank()) {
                        viewModel.addArticle(textTitle, textContent, textAuthor.ifBlank { null }, "Ebook Import", null)
                        navController.navigate(Screen.Articles.route)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_imported_book_button"),
                enabled = textTitle.isNotBlank() && textContent.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存并加入文章库", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BookImportCard(title: String, author: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("作者: $author", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("快速载入", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
