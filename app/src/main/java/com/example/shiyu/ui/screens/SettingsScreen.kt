package com.example.shiyu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.example.shiyu.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("deepseek-v4-flash") }
    var fontDefault by remember { mutableStateOf("medium") }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf<Boolean?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val savedKey = viewModel.getSetting("deepseek_api_key")
        if (!savedKey.isNullOrBlank()) {
            apiKey = savedKey
        }
        val savedModel = viewModel.getSetting("deepseek_model")
        if (!savedModel.isNullOrBlank()) {
            modelName = savedModel
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用设置", fontWeight = FontWeight.Bold) }
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
            // DeepSeek AI API Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DeepSeek 大模型 API 配置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("配置 DeepSeek API Key 及模型（支持 deepseek-v4-flash、deepseek-v4-pro），已保存在数据库中的配置离线永久生效。", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("DeepSeek API Key") },
                        modifier = Modifier.fillMaxWidth().testTag("deepseek_api_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("DeepSeek 模型名称") },
                        placeholder = { Text("deepseek-v4-flash") },
                        modifier = Modifier.fillMaxWidth().testTag("deepseek_model_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = modelName == "deepseek-v4-flash",
                            onClick = { modelName = "deepseek-v4-flash" },
                            label = { Text("deepseek-v4-flash", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = modelName == "deepseek-v4-pro",
                            onClick = { modelName = "deepseek-v4-pro" },
                            label = { Text("deepseek-v4-pro", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (apiKey.isBlank()) {
                                    viewModel.showToast("请先输入 DeepSeek API Key")
                                    return@OutlinedButton
                                }
                                val cleanModel = modelName.trim().ifBlank { "deepseek-v4-flash" }
                                modelName = cleanModel
                                isTestingConnection = true
                                testResultText = null
                                isTestSuccess = null
                                scope.launch {
                                    val res = viewModel.testDeepSeekConnection(apiKey, cleanModel)
                                    isTestingConnection = false
                                    res.onSuccess { msg ->
                                        isTestSuccess = true
                                        testResultText = msg
                                        viewModel.showToast("DeepSeek API 连接正常！")
                                    }.onFailure { err ->
                                        isTestSuccess = false
                                        testResultText = "❌ 连接测试失败: ${err.message ?: "未知网络异常"}"
                                        viewModel.showToast("连接测试失败，请检查密钥与网络")
                                    }
                                }
                            },
                            enabled = !isTestingConnection,
                            modifier = Modifier.testTag("test_connection_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("正在测试...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("测试连接", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val cleanModel = modelName.trim().ifBlank { "deepseek-v4-flash" }
                                modelName = cleanModel
                                viewModel.saveSetting("deepseek_api_key", apiKey.trim())
                                viewModel.saveSetting("deepseek_model", cleanModel)
                                viewModel.showToast("AI 配置已保存 ($cleanModel)")
                            },
                            modifier = Modifier.testTag("save_api_key_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("保存 AI 配置")
                        }
                    }

                    AnimatedVisibility(
                        visible = testResultText != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            color = if (isTestSuccess == true) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (isTestSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isTestSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = testResultText ?: "",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isTestSuccess == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Reader Defaults
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("阅读器字号预设", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = fontDefault == "small",
                            onClick = { fontDefault = "small"; viewModel.setReaderFontSize("small") },
                            label = { Text("小字号") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = fontDefault == "medium",
                            onClick = { fontDefault = "medium"; viewModel.setReaderFontSize("medium") },
                            label = { Text("标准") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = fontDefault == "large",
                            onClick = { fontDefault = "large"; viewModel.setReaderFontSize("large") },
                            label = { Text("大字号") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // App Version Announcement Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("关于 ShiYu (相遇)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("当前版本: v0.2.3 (代号: 相遇)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("许可与版权: MIT License © 2026 amluckydave", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("ShiYu 是一款专为外语阅读与词汇积累设计的离线助手，集成 FSRS 间隔重复记忆算法、句法本、阅读词表提取与智能 AI 句子解析。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
