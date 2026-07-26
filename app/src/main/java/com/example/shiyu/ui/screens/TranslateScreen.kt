package com.example.shiyu.ui.screens

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
import com.example.shiyu.ui.components.MarkdownText
import com.example.shiyu.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var inputText by remember { mutableStateOf("") }
    var promptType by remember { mutableStateOf("complex_sentence") } // "word", "sentence", "complex_sentence"
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 翻译与长难句解析", fontWeight = FontWeight.Bold) }
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
            // Prompt Mode Segmented Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = promptType == "word",
                    onClick = { promptType = "word" },
                    label = { Text("单词查词") },
                    modifier = Modifier.weight(1f).testTag("mode_word")
                )
                FilterChip(
                    selected = promptType == "sentence",
                    onClick = { promptType = "sentence" },
                    label = { Text("标准句子翻译") },
                    modifier = Modifier.weight(1f).testTag("mode_sentence")
                )
                FilterChip(
                    selected = promptType == "complex_sentence",
                    onClick = { promptType = "complex_sentence" },
                    label = { Text("长难句剖析") },
                    modifier = Modifier.weight(1f).testTag("mode_complex")
                )
            }

            // Input Text Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("ai_translate_input"),
                placeholder = { Text("请输入要深度解析的英文单词或长难句...") },
                shape = RoundedCornerShape(12.dp)
            )

            // Execute Button
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        isLoading = true
                        scope.launch {
                            val apiKey = viewModel.getSetting("deepseek_api_key")
                            val model = viewModel.getSetting("deepseek_model") ?: "deepseek-v4-flash"
                            val res = viewModel.deepSeekService.translateOrAnalyze(inputText, promptType, apiKey, model)
                            resultText = res
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("run_ai_translate_button"),
                enabled = !isLoading && inputText.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 正在推演中...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始智能解析", fontWeight = FontWeight.Bold)
                }
            }

            // Output Result Card
            if (resultText.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_result_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("解析结论", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row {
                                IconButton(onClick = { viewModel.speak(inputText) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读原文", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        MarkdownText(markdown = resultText)

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (promptType == "word") {
                                Button(
                                    onClick = { viewModel.addVocabulary(inputText, resultText, null, "AI翻译") },
                                    modifier = Modifier.weight(1f).testTag("save_ai_to_vocab_button")
                                ) {
                                    Text("存入生词本")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.addSentence(inputText, resultText, "AI翻译") },
                                    modifier = Modifier.weight(1f).testTag("save_ai_to_sentences_button")
                                ) {
                                    Text("存入句法本")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
