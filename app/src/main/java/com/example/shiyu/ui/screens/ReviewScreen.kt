package com.example.shiyu.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.shiyu.fsrs.FsrsRating
import com.example.shiyu.fsrs.FsrsScheduler
import com.example.shiyu.ui.navigation.Screen
import com.example.shiyu.ui.components.MarkdownText
import com.example.shiyu.ui.theme.PrimaryIndigo
import com.example.shiyu.ui.viewmodel.MainViewModel
import com.example.shiyu.ui.viewmodel.ReviewItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val reviewQueue by viewModel.reviewQueue.collectAsState()
    var isFlipped by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadReviewQueue()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FSRS 间隔重复复习", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(
                        text = "剩余 ${reviewQueue.size} 张",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (reviewQueue.isEmpty()) {
                // Queue Completed Empty State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_completed_container")
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("太棒了！今日需复习卡片已全部完成", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("FSRS 算法已根据记忆曲线更新卡片到期时间。", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Home.route) },
                        modifier = Modifier.testTag("back_home_after_review_button")
                    ) {
                        Text("返回首页")
                    }
                }
            } else {
                val currentItem = reviewQueue.first()
                val previews = remember(currentItem, isFlipped) {
                    FsrsScheduler.previewRatings(currentItem.fsrsCard)
                }

                // Card Rotation Animation
                val rotation by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 400),
                    label = "cardFlipAnimation"
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Flashcard Area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12 * density
                            }
                            .clickable { isFlipped = !isFlipped }
                            .testTag("review_flashcard"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rotation <= 90f) {
                                // Front Side
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (currentItem.type == com.example.shiyu.ui.viewmodel.ReviewItemType.VOCABULARY) "生词" else "句法",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = currentItem.front,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 34.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    IconButton(
                                        onClick = { viewModel.speak(currentItem.front) },
                                        modifier = Modifier.testTag("review_speak_button")
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = PrimaryIndigo, modifier = Modifier.size(32.dp))
                                    }
                                    currentItem.context?.let { ctx ->
                                        if (ctx.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "\"$ctx\"",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("💡 点击卡片翻转显示答案", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            } else {
                                // Back Side (Flipped)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                                ) {
                                    Text(
                                        text = "【释义 / 详细解析】",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigo,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    MarkdownText(
                                        markdown = currentItem.back,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                                    )
                                    currentItem.articlePath?.let { path ->
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("来源: $path", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Rating Buttons
                    if (!isFlipped) {
                        Button(
                            onClick = { isFlipped = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("flip_card_button"),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text("显示答案", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // 4 Rating Buttons (Again, Hard, Good, Easy)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RatingButton("忘了", previews[FsrsRating.AGAIN] ?: "", Color(0xFFEF4444), Modifier.weight(1f).testTag("rate_again_button")) {
                                viewModel.gradeReviewItem(currentItem, FsrsRating.AGAIN)
                                isFlipped = false
                            }
                            RatingButton("困难", previews[FsrsRating.HARD] ?: "", Color(0xFFF59E0B), Modifier.weight(1f).testTag("rate_hard_button")) {
                                viewModel.gradeReviewItem(currentItem, FsrsRating.HARD)
                                isFlipped = false
                            }
                            RatingButton("记住了", previews[FsrsRating.GOOD] ?: "", Color(0xFF10B981), Modifier.weight(1f).testTag("rate_good_button")) {
                                viewModel.gradeReviewItem(currentItem, FsrsRating.GOOD)
                                isFlipped = false
                            }
                            RatingButton("简单", previews[FsrsRating.EASY] ?: "", Color(0xFF3B82F6), Modifier.weight(1f).testTag("rate_easy_button")) {
                                viewModel.gradeReviewItem(currentItem, FsrsRating.EASY)
                                isFlipped = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingButton(label: String, interval: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(interval, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}
