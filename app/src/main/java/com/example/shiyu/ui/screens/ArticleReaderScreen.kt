package com.example.shiyu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.shiyu.ui.navigation.Screen
import com.example.shiyu.ui.theme.BorderLight
import com.example.shiyu.ui.theme.EyeCareGreenBg
import com.example.shiyu.ui.theme.EyeCareGreenCard
import com.example.shiyu.ui.theme.EyeCareGreenText
import com.example.shiyu.ui.theme.EyeCareYellowBg
import com.example.shiyu.ui.theme.EyeCareYellowCard
import com.example.shiyu.ui.theme.EyeCareYellowSecondary
import com.example.shiyu.ui.theme.EyeCareYellowText
import com.example.shiyu.ui.theme.OnPrimaryContainerDark
import com.example.shiyu.ui.theme.PrimaryContainerLight
import com.example.shiyu.ui.theme.PrimaryGreen
import com.example.shiyu.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ArticleReaderScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val article by viewModel.currentArticle.collectAsState()
    val fontSizeState by viewModel.readerFontSize.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var selectedWordMeaning by remember { mutableStateOf("") }
    var selectedWordContext by remember { mutableStateOf("") }
    var isAnalyzingWord by remember { mutableStateOf(false) }
    var isGeneratingMindMap by remember { mutableStateOf(false) }
    var showMindMapDetail by remember { mutableStateOf(false) }
    var isFullScreenReader by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    val scope = rememberCoroutineScope()

    val textSizeSp = when (fontSizeState) {
        "small" -> 14.sp
        "large" -> 19.sp
        else -> 16.sp
    }
    val textLineHeight = textSizeSp * 1.6f
    val textLetterSpacing = 0.3.sp
    val textFontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif

    if (article == null) {
        Scaffold { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("请选择要阅读的文章")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { navController.navigate(Screen.Articles.route) }) {
                        Text("返回文章列表")
                    }
                }
            }
        }
        return
    }

    val currentArticle = article!!

    // Extract unique words for Article Word List Tab
    val extractedWords = remember(currentArticle.content) {
        currentArticle.content
            .split("[\\s,.:;!\"'()?-]+".toRegex())
            .filter { it.length > 2 && it.all { c -> c.isLetter() } }
            .map { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
            .distinct()
            .sorted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentArticle.title,
                        maxLines = 1,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val next = when (fontSizeState) {
                            "small" -> "medium"
                            "medium" -> "large"
                            else -> "small"
                        }
                        viewModel.setReaderFontSize(next)
                    }) {
                        Text("A", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Reader Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("正文阅读") },
                    modifier = Modifier.testTag("reader_tab_text")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("文章词表 (${extractedWords.size})") },
                    modifier = Modifier.testTag("reader_tab_words")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("思维导图") },
                    modifier = Modifier.testTag("reader_tab_mindmap")
                )
            }

            when (selectedTab) {
                0 -> {
                    // Article Content Reader with Eye-Care Cream Beige Background and Full Screen Reading Button
                    val allParagraphs = remember(currentArticle.content) {
                        currentArticle.content.split("\n\n").filter { it.isNotBlank() }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(EyeCareYellowBg)
                    ) {
                        // Header Bar with Word Count and Full Screen Reading Action
                        Surface(
                            color = EyeCareYellowCard,
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Row(
                                    modifier = Modifier
                                        .widthIn(max = 660.dp)
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📖 ", fontSize = 13.sp)
                                        Text(
                                            text = "正文阅读 · 共 ${allParagraphs.size} 段 (约 ${currentArticle.wordCount} 词)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EyeCareYellowSecondary
                                        )
                                    }

                                    Button(
                                        onClick = { isFullScreenReader = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("full_screen_reader_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Fullscreen,
                                            contentDescription = "全屏阅读",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("全屏查看", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Article Body Column with Smooth Vertical Scroll
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 660.dp)
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                ) {
                                    allParagraphs.forEach { para ->
                                        InteractiveParagraphView(
                                            para = para,
                                            textSizeSp = textSizeSp,
                                            textLineHeight = textLineHeight,
                                            textLetterSpacing = textLetterSpacing,
                                            textFontFamily = textFontFamily,
                                            textColor = EyeCareYellowText,
                                            onWordClick = { cleanWord ->
                                                if (cleanWord.isNotBlank()) {
                                                    selectedWord = cleanWord
                                                    selectedWordContext = para
                                                    isAnalyzingWord = true
                                                    scope.launch {
                                                        val res = viewModel.lookupWord(cleanWord)
                                                        selectedWordMeaning = res
                                                        isAnalyzingWord = false
                                                    }
                                                }
                                            },
                                            onSaveSentence = { targetSentence ->
                                                viewModel.analyzeAndAddSentence(targetSentence, currentArticle.title)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    Spacer(modifier = Modifier.height(40.dp))
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Article Word List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(extractedWords) { word ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.speak(word)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(word, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    }
                                    IconButton(onClick = {
                                        viewModel.addVocabulary(word, "提取自文章: ${currentArticle.title}", null, currentArticle.title)
                                    }) {
                                        Icon(Icons.Default.BookmarkAdd, contentDescription = "加入生词本", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Article Mind Map View with DeepSeek AI Generation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("思维导图大纲", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("基于文章语义逻辑的结构拆解大纲", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (!isGeneratingMindMap) {
                                        isGeneratingMindMap = true
                                        scope.launch {
                                            viewModel.generateArticleMindMap(currentArticle)
                                            isGeneratingMindMap = false
                                        }
                                    }
                                },
                                enabled = !isGeneratingMindMap,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isGeneratingMindMap) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("生成中...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (currentArticle.mindmapMarkdown.isNullOrBlank()) "DeepSeek 一键生成" else "重新生成", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isGeneratingMindMap) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = PrimaryGreen,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("🌿 DeepSeek AI 大模型推演中...", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryGreen)
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = PrimaryGreen,
                                        trackColor = PrimaryGreen.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("正在实时剖析文章段落主干、因果关联与中文逻辑批注，请稍候...", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        } else if (currentArticle.mindmapMarkdown.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderLight)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("🌿", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "暂无思维导图",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "点击上方【DeepSeek 一键生成】或下方按钮，AI 将为你精准提取段落主干、因果关系与中文逻辑批注。",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = {
                                            isGeneratingMindMap = true
                                            scope.launch {
                                                viewModel.generateArticleMindMap(currentArticle)
                                                isGeneratingMindMap = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("generate_mindmap_empty_btn")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("一键生成 AI 深度思维导图", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            val mindmapText = currentArticle.mindmapMarkdown!!
                            val treeNodes = remember(mindmapText) { parseMarkdownToMindMapTree(mindmapText) }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMindMapDetail = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderLight)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = PrimaryContainerLight,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("🌿 AI 推演完成 (${treeNodes.size} 个主题)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainerDark)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Fullscreen, contentDescription = "全屏查看", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("全屏护眼模式 🔍", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    androidx.compose.foundation.text.selection.SelectionContainer {
                                        Column {
                                            treeNodes.forEach { rootNode ->
                                                RenderMindMapTreeNode(node = rootNode, isEyeCareMode = false)
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isFullScreenReader) {
        BackHandler(enabled = true) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                isFullScreenReader = false
            } else {
                lastBackPressTime = currentTime
                viewModel.showToast("连按两次返回键退出全屏")
            }
        }

        Dialog(
            onDismissRequest = {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    isFullScreenReader = false
                } else {
                    lastBackPressTime = currentTime
                    viewModel.showToast("连按两次返回键退出全屏")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = EyeCareYellowBg
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Fullscreen Header Bar
                    Surface(
                        color = EyeCareYellowCard,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentArticle.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EyeCareYellowText,
                                    maxLines = 1
                                )
                                Text(
                                    text = "💡 提示：连按两次返回键或点击右侧按钮退出全屏",
                                    fontSize = 11.sp,
                                    color = EyeCareYellowSecondary
                                )
                            }

                            IconButton(
                                onClick = { isFullScreenReader = false },
                                modifier = Modifier.testTag("exit_full_screen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "退出全屏",
                                    tint = EyeCareYellowText
                                )
                            }
                        }
                    }

                    // Fullscreen Reading Scroll Area
                    val allParagraphs = remember(currentArticle.content) {
                        currentArticle.content.split("\n\n").filter { it.isNotBlank() }
                    }
                    val fullScrollState = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(fullScrollState),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            allParagraphs.forEach { para ->
                                InteractiveParagraphView(
                                    para = para,
                                    textSizeSp = textSizeSp,
                                    textLineHeight = textLineHeight,
                                    textLetterSpacing = textLetterSpacing,
                                    textFontFamily = textFontFamily,
                                    textColor = EyeCareYellowText,
                                    onWordClick = { cleanWord ->
                                        if (cleanWord.isNotBlank()) {
                                            selectedWord = cleanWord
                                            selectedWordContext = para
                                            isAnalyzingWord = true
                                            scope.launch {
                                                val res = viewModel.lookupWord(cleanWord)
                                                selectedWordMeaning = res
                                                isAnalyzingWord = false
                                            }
                                        }
                                    },
                                    onSaveSentence = { targetSentence ->
                                        viewModel.analyzeAndAddSentence(targetSentence, currentArticle.title)
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
            }
        }
    }

    if (showMindMapDetail) {
        val mindmapText = article?.mindmapMarkdown?.takeIf { it.isNotBlank() } ?: """
            # 📌 ${article?.title ?: "文章思维导图"}
            ## 1️⃣ 暂未生成思维导图
            - 点击右上角【DeepSeek 一键生成】即可获取 AI 精细提取的段落逻辑与中文批注。
        """.trimIndent()

        MindMapDetailDialog(
            title = article?.title ?: "思维导图",
            markdownText = mindmapText,
            onDismiss = { showMindMapDetail = false }
        )
    }

    // Word Lookup Bottom Sheet / Modal
    selectedWord?.let { word ->
        AlertDialog(
            onDismissRequest = { selectedWord = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(word, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = { viewModel.speak(word) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAnalyzingWord) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = PrimaryGreen)
                        Text("智能查询中...", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally), color = PrimaryGreen)
                    } else {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = selectedWordMeaning,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addVocabulary(word, selectedWordMeaning, selectedWordContext, currentArticle.title)
                        selectedWord = null
                    },
                    modifier = Modifier.testTag("add_word_to_vocab_dialog_button")
                ) {
                    Text("存入生词本")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedWord = null }) {
                    Text("关闭")
                }
            }
        )
    }
}

data class MindMapNodeItem(
    val id: String,
    val level: Int,
    val title: String,
    val children: MutableList<MindMapNodeItem> = mutableListOf()
)

fun parseMarkdownToMindMapTree(markdown: String): List<MindMapNodeItem> {
    val roots = mutableListOf<MindMapNodeItem>()
    val lines = markdown.lines().map { it.trim() }.filter { it.isNotBlank() }

    var currentLevel1: MindMapNodeItem? = null
    var currentLevel2: MindMapNodeItem? = null
    var idCounter = 0

    for (line in lines) {
        idCounter++
        val cleanText = line
            .replace(Regex("^[#\\-*•\\d\\.\\s]+"), "")
            .replace("**", "")
            .trim()

        if (cleanText.isBlank()) continue

        when {
            line.startsWith("# ") -> {
                val node = MindMapNodeItem("node_$idCounter", 1, cleanText)
                roots.add(node)
                currentLevel1 = node
                currentLevel2 = null
            }
            line.startsWith("## ") -> {
                val node = MindMapNodeItem("node_$idCounter", 2, cleanText)
                if (currentLevel1 != null) {
                    currentLevel1.children.add(node)
                } else {
                    roots.add(node)
                }
                currentLevel2 = node
            }
            line.startsWith("### ") || line.startsWith("- ") || line.startsWith("• ") || line.matches(Regex("^\\d+\\..*")) -> {
                val node = MindMapNodeItem("node_$idCounter", 3, cleanText)
                if (currentLevel2 != null) {
                    currentLevel2.children.add(node)
                } else if (currentLevel1 != null) {
                    currentLevel1.children.add(node)
                } else {
                    roots.add(node)
                }
            }
            else -> {
                val node = MindMapNodeItem("node_$idCounter", 3, cleanText)
                if (currentLevel2 != null) {
                    currentLevel2.children.add(node)
                } else if (currentLevel1 != null) {
                    currentLevel1.children.add(node)
                } else {
                    roots.add(node)
                }
            }
        }
    }
    return roots
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapDetailDialog(
    title: String,
    markdownText: String,
    onDismiss: () -> Unit
) {
    val treeNodes = remember(markdownText) { parseMarkdownToMindMapTree(markdownText) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EyeCareGreenBg
        ) {
            Scaffold(
                containerColor = EyeCareGreenBg,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("🌿 全屏思维导图 · 护眼模式", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EyeCareGreenText)
                                Text(title, fontSize = 12.sp, color = EyeCareGreenText.copy(alpha = 0.7f), maxLines = 1)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = EyeCareGreenText)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = EyeCareGreenBg)
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = EyeCareGreenCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("当前处于护眼绿微距渲染视角，点击节点分支可进行无缝展开/收起。", fontSize = 12.sp, color = EyeCareGreenText)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (treeNodes.isEmpty()) {
                        Text("无可用思维导图数据", color = EyeCareGreenText, modifier = Modifier.padding(16.dp))
                    } else {
                        treeNodes.forEach { rootNode ->
                            RenderMindMapTreeNode(node = rootNode, isEyeCareMode = true)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun RenderMindMapTreeNode(
    node: MindMapNodeItem,
    isEyeCareMode: Boolean = false,
    indent: Int = 0
) {
    var isExpanded by remember { mutableStateOf(true) }

    val bgColor = if (isEyeCareMode) EyeCareGreenCard else MaterialTheme.colorScheme.surface
    val textColor = if (isEyeCareMode) EyeCareGreenText else MaterialTheme.colorScheme.onSurface
    val strokeColor = if (isEyeCareMode) BorderLight else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 10).dp)
    ) {
        when (node.level) {
            1 -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isEyeCareMode) PrimaryContainerLight else MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryGreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📌", fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = node.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OnPrimaryContainerDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            2 -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, strokeColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = node.children.isNotEmpty()) {
                            isExpanded = !isExpanded
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🌿", fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = node.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )

                            if (node.children.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryGreen.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = "${node.children.size}项",
                                        fontSize = 10.sp,
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                val isAnnotationNode = node.title.contains("💡") || node.title.contains("批注") || node.title.contains("解析") || node.title.contains("注解")
                if (isAnnotationNode) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEyeCareMode) EyeCareGreenCard else EyeCareYellowCard
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isEyeCareMode) PrimaryGreen.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color(0xFFF59E0B).copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = node.title.replace("💡", "").trim(),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isEyeCareMode) EyeCareGreenText else EyeCareYellowSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                        )
                        Text(
                            text = node.title,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (node.children.isNotEmpty()) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 6.dp)) {
                    node.children.forEach { child ->
                        RenderMindMapTreeNode(node = child, isEyeCareMode = isEyeCareMode, indent = indent + 1)
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveParagraphView(
    para: String,
    textSizeSp: androidx.compose.ui.unit.TextUnit,
    textLineHeight: androidx.compose.ui.unit.TextUnit,
    textLetterSpacing: androidx.compose.ui.unit.TextUnit,
    textFontFamily: androidx.compose.ui.text.font.FontFamily,
    textColor: Color,
    onWordClick: (String) -> Unit,
    onSaveSentence: (String) -> Unit
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showSentenceDialog by remember { mutableStateOf(false) }
    var initialSelectedSentence by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = para,
            fontSize = textSizeSp,
            lineHeight = textLineHeight,
            letterSpacing = textLetterSpacing,
            fontFamily = textFontFamily,
            color = textColor,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(para) {
                    detectTapGestures(
                        onTap = { offset ->
                            textLayoutResult?.let { layout ->
                                val position = layout.getOffsetForPosition(offset)
                                val wordRange = extractWordAtOffset(para, position)
                                if (wordRange.isNotBlank()) {
                                    onWordClick(wordRange)
                                }
                            }
                        }
                    )
                }
                .padding(vertical = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {
                    initialSelectedSentence = ""
                    showSentenceDialog = true
                },
                label = { Text("📌 选句存入句法本", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = PrimaryGreen)
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                colors = AssistChipDefaults.assistChipColors(containerColor = PrimaryGreen.copy(alpha = 0.08f))
            )
        }
    }

    if (showSentenceDialog) {
        SentenceSelectionDialog(
            paragraph = para,
            initialSelected = initialSelectedSentence,
            onDismiss = { showSentenceDialog = false },
            onConfirmSave = { chosenSentence ->
                showSentenceDialog = false
                if (chosenSentence.isNotBlank()) {
                    onSaveSentence(chosenSentence)
                }
            }
        )
    }
}

@Composable
fun SentenceSelectionDialog(
    paragraph: String,
    initialSelected: String,
    onDismiss: () -> Unit,
    onConfirmSave: (String) -> Unit
) {
    val sentences = remember(paragraph) {
        paragraph.split(Regex("(?<=[.!?！?。])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    var selectedText by remember {
        mutableStateOf(if (initialSelected.isNotBlank()) initialSelected else (sentences.firstOrNull() ?: paragraph))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("挑选长难句存入句法本", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "轻触选择段落中的单句，或在下方直接自定义编辑：",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (sentences.isNotEmpty()) {
                    Text("拆分单句列表（轻触切换）：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    sentences.forEachIndexed { _, sentenceText ->
                        val isSelected = (selectedText == sentenceText)
                        Surface(
                            onClick = { selectedText = sentenceText },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PrimaryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) PrimaryGreen else Color.LightGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedText = sentenceText },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sentenceText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("所选长难句/自定义文本：", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = selectedText,
                    onValueChange = { selectedText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 160.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                    placeholder = { Text("要存入句法本的内容...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSave(selectedText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(10.dp),
                enabled = selectedText.isNotBlank()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("存入句法本并 AI 剖析", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.outline)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

fun extractWordAtOffset(text: String, offset: Int): String {
    if (text.isEmpty() || offset < 0 || offset >= text.length) return ""
    var start = offset
    var end = offset
    while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '\'')) {
        start--
    }
    while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '\'')) {
        end++
    }
    if (start < end) {
        return text.substring(start, end).trim()
    }
    return ""
}

fun extractSentenceAtOffset(text: String, offset: Int): String {
    if (text.isEmpty() || offset < 0 || offset >= text.length) return text
    var start = offset
    while (start > 0) {
        val ch = text[start - 1]
        if (ch == '.' || ch == '!' || ch == '?' || ch == '。' || ch == '！' || ch == '？' || ch == '\n') {
            break
        }
        start--
    }
    var end = offset
    while (end < text.length) {
        val ch = text[end]
        if (ch == '.' || ch == '!' || ch == '?' || ch == '。' || ch == '！' || ch == '？' || ch == '\n') {
            end++
            break
        }
        end++
    }
    val result = text.substring(start, end).trim()
    return if (result.isNotBlank()) result else text
}
