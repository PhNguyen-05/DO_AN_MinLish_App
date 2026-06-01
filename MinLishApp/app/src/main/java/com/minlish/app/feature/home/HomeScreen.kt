package com.minlish.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.minlish.app.data.model.LearningDeckSummary
import com.minlish.app.data.model.NotificationSummaryResponse
import com.minlish.app.data.model.ProgressResponse
import com.minlish.app.data.remote.RetrofitClient
import com.minlish.app.feature.notification.ReminderPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProfileClick: () -> Unit = {},
    onImportExportClick: () -> Unit = {},
    onLearningClick: (Long?, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchDashboardData()
    }

    val data = viewModel.dashboardState
    val learningPlan = viewModel.learningPlanState
    val learningDecks = viewModel.learningDecksState
    val progress = viewModel.progressState
    val notificationSummary = viewModel.notificationSummaryState

    LaunchedEffect(learningPlan) {
        learningPlan?.let { plan ->
            ReminderPreferences.updateStudyStats(
                context,
                wordsLearnedToday = plan.words_learned_today,
                wordsReviewedToday = plan.words_reviewed_today,
                dueReviewCount = plan.due_review_count
            )
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isVisible by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchDashboardData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        containerColor = Color(0xFFF5FAF8),
        topBar = {
            TopAppBar(
                title = { Text("MinLish", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        AsyncImage(
                            model = RetrofitClient.resolveServerUrl(data?.avatar_url)
                                ?: "https://i.pravatar.cc/150?u=${data?.full_name ?: "user"}",
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5FAF8),
                    titleContentColor = Color(0xFF102522)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(animationSpec = tween(600))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Chào mừng trở lại, ${data?.full_name?.split(" ")?.first() ?: "Bạn"}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF102522)
                    )
                    Text(
                        text = "Một phiên học nhỏ hôm nay là đủ giữ nhịp.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedStreakCard(currentStreak = data?.current_streak ?: 0)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("📚", "Từ đã học", "${data?.total_words_learned ?: 0}", modifier = Modifier.weight(1f))
                StatCard("🎯", "Chính xác", "${(data?.accuracy_rate?.times(100) ?: 0f).toInt()}%", modifier = Modifier.weight(1f))
            }

            DailyGoalCard(
                dailyGoal = learningPlan?.daily_new_words_goal ?: data?.daily_new_words_goal ?: 20,
                learnedToday = learningPlan?.words_learned_today ?: 0,
                dailyReviewGoal = learningPlan?.daily_review_goal ?: 50,
                reviewedToday = learningPlan?.words_reviewed_today ?: 0,
                dueReviewCount = learningPlan?.due_review_count ?: 0
            )

            ProgressOverviewSection(progress = progress)

            StudyReminderSummaryCard(
                summary = notificationSummary,
                fallbackDueReviewCount = learningPlan?.due_review_count ?: 0
            )

            Text("Hành động nhanh", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            QuickActionsRow(
                onImportExportClick = onImportExportClick,
                onLearningClick = { mode -> onLearningClick(null, mode) }
            )

            Text("Tiếp tục học", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            ContinueLearningCard(
                deck = learningDecks.continue_deck,
                onLearningClick = { deckId -> onLearningClick(deckId, "mixed") }
            )

            SuggestedDeckSection(
                decks = learningDecks.decks,
                onLearningClick = { deckId, mode -> onLearningClick(deckId, mode) }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AnimatedStreakCard(currentStreak: Int) {
    val animatedStreak by animateIntAsState(
        targetValue = currentStreak,
        animationSpec = tween(durationMillis = 700),
        label = "streakCounter"
    )
    val shine by rememberInfiniteTransition(label = "streakShine").animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "streakShineOffset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFFFF8A65).copy(alpha = 0.18f),
                spotColor = Color(0xFFFF8A65).copy(alpha = 0.18f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFF1D8),
                            Color(0xFFFFE3D7),
                            Color(0xFFE4F8F2)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { translationX = 360f * shine }
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0f),
                                Color.White.copy(alpha = 0.32f),
                                Color.White.copy(alpha = 0f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.72f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🔥", fontSize = 34.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Chuỗi học hiện tại", fontSize = 15.sp, color = Color(0xFF8B4B00))
                    Text(
                        "$animatedStreak ngày liên tiếp",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF552C00)
                    )
                    Text(
                        "Giữ nhịp đều để từ vựng ở lại lâu hơn.",
                        fontSize = 13.sp,
                        color = Color(0xFF7C5B41)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(icon: String, title: String, value: String, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400),
        label = "statScale"
    )

    Card(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE7ECEA))
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F6F3)) {
                Text(icon, fontSize = 26.sp, modifier = Modifier.padding(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DailyGoalCard(
    dailyGoal: Int,
    learnedToday: Int,
    dailyReviewGoal: Int,
    reviewedToday: Int,
    dueReviewCount: Int
) {
    val newProgress = (learnedToday.toFloat() / dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val reviewProgress = (reviewedToday.toFloat() / dailyReviewGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedNewProgress by animateFloatAsState(
        targetValue = newProgress,
        animationSpec = tween(durationMillis = 650),
        label = "newWordsProgress"
    )
    val animatedReviewProgress by animateFloatAsState(
        targetValue = reviewProgress,
        animationSpec = tween(durationMillis = 650),
        label = "reviewProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE7ECEA))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mục tiêu hôm nay", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFE8F6F3)) {
                    Text(
                        "${((newProgress + reviewProgress) / 2f * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color(0xFF147A70),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Từ mới", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedNewProgress },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF26A69A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("$learnedToday / $dailyGoal từ mới", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Ôn tập", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animatedReviewProgress },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF5E7CE2)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$reviewedToday / $dailyReviewGoal thẻ ôn, $dueReviewCount thẻ đang đến hạn",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProgressOverviewSection(progress: ProgressResponse?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Theo dõi tiến độ", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LevelEstimationCard(progress = progress, modifier = Modifier.weight(1f))
            RetentionRateCard(rate = progress?.retention_rate ?: 0f, modifier = Modifier.weight(1f))
        }

        DailyActivityChart(progress = progress)
    }
}

@Composable
fun LevelEstimationCard(progress: ProgressResponse?, modifier: Modifier = Modifier) {
    val level = progress?.estimated_level ?: "Beginner"
    val color = when (level) {
        "Advanced" -> Color(0xFF7E57C2)
        "Intermediate" -> Color(0xFF5E7CE2)
        else -> Color(0xFF26A69A)
    }

    Card(
        modifier = modifier.height(146.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
                Text("Level", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Text(level, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Color(0xFF102522))
            Text(
                progress?.level_reason ?: "Bắt đầu học để hệ thống ước lượng level chính xác hơn.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RetentionRateCard(rate: Float, modifier: Modifier = Modifier) {
    val percent = (rate * 100).toInt().coerceIn(0, 100)
    val animatedRate by animateFloatAsState(
        targetValue = rate.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "retentionRate"
    )

    Card(
        modifier = modifier.height(146.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDDEBE7)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ghi nhớ", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("$percent%", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF26A69A))
            LinearProgressIndicator(
                progress = { animatedRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = Color(0xFF26A69A),
                trackColor = Color(0xFFE4EEEA)
            )
            Text("Dựa trên kết quả ôn tập 30 ngày gần nhất.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DailyActivityChart(progress: ProgressResponse?) {
    val items = progress?.daily_activity.orEmpty()
    val maxTotal = items.maxOfOrNull { it.words_learned + it.words_reviewed }?.coerceAtLeast(1) ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE7ECEA)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hoạt động hằng ngày", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                ChartLegend(Color(0xFF26A69A), "Mới")
                Spacer(modifier = Modifier.width(8.dp))
                ChartLegend(Color(0xFF5E7CE2), "Ôn")
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(132.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có dữ liệu học tập.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(146.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    items.forEach { item ->
                        ActivityBar(
                            learned = item.words_learned,
                            reviewed = item.words_reviewed,
                            maxTotal = maxTotal,
                            label = item.date.takeLast(2),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartLegend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ActivityBar(learned: Int, reviewed: Int, maxTotal: Int, label: String, modifier: Modifier = Modifier) {
    val learnedHeight = ((learned.toFloat() / maxTotal) * 104).coerceIn(2f, 104f)
    val reviewedHeight = ((reviewed.toFloat() / maxTotal) * 104).coerceIn(2f, 104f)

    Column(modifier = modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (reviewed > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(reviewedHeight.dp)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(Color(0xFF5E7CE2))
                )
            }
            if (learned > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(learnedHeight.dp)
                        .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                        .background(Color(0xFF26A69A))
                )
            }
            if (learned == 0 && reviewed == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFE4EEEA))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StudyReminderSummaryCard(summary: NotificationSummaryResponse?, fallbackDueReviewCount: Int) {
    val dueCount = summary?.due_review_count ?: fallbackDueReviewCount
    val newWords = summary?.new_words_available ?: 0
    val body = summary?.push_body ?: when {
        dueCount > 0 -> "Bạn có $dueCount thẻ cần ôn hôm nay."
        newWords > 0 -> "Bạn còn $newWords từ mới có thể học hôm nay."
        else -> "Mở MinLish để giữ nhịp học hằng ngày."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBFF)),
        border = BorderStroke(1.dp, Color(0xFFD6E3F8)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F0FF)) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color(0xFF5E7CE2),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(summary?.push_title ?: "MinLish nhắc học", fontWeight = FontWeight.SemiBold)
                Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// NotificationReminderCard was moved to Profile & Settings screen.

@Composable
fun QuickActionsRow(onImportExportClick: () -> Unit, onLearningClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(Icons.Default.UploadFile, "Import", onClick = onImportExportClick, modifier = Modifier.weight(1f))
        QuickActionButton(Icons.Default.Add, "Từ mới", onClick = { onLearningClick("new") }, modifier = Modifier.weight(1f))
        QuickActionButton(Icons.Default.Replay, "Ôn tập", onClick = { onLearningClick("review") }, modifier = Modifier.weight(1f))
        QuickActionButton(Icons.Default.Edit, "Luyện tập", onClick = { onLearningClick("mixed") }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = Color(0xFF9ECAC1).copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD6E5E0)),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = Color(0xFF26A69A), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ContinueLearningCard(deck: LearningDeckSummary?, onLearningClick: (Long?) -> Unit) {
    val progress = if (deck == null) 0f else {
        (deck.learned_words.toFloat() / deck.total_words.coerceAtLeast(1)).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 650),
        label = "continueDeckProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFDDEBE7))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (deck == null) {
                Text("Không có bộ đang học dở", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bạn đã hoàn thành các bộ đang học. Chọn một bộ khác bên dưới để bắt đầu.", fontSize = 14.sp)
            } else {
                Text("Đang học: ${deck.title}", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bạn đã học ${deck.learned_words}/${deck.total_words} từ trong bộ này.", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Còn ${(deck.total_words - deck.learned_words).coerceAtLeast(0)} từ mới.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF26A69A),
                    trackColor = Color(0xFFE4EEEA)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onLearningClick(deck?.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A), contentColor = Color.White)
            ) {
                Text(if (deck == null) "Học bộ khác" else "Tiếp tục học ngay")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestedDeckSection(decks: List<LearningDeckSummary>, onLearningClick: (Long, String) -> Unit) {
    val pageSize = 2
    val sortedDecks = remember(decks) {
        decks.sortedWith(
            compareBy<LearningDeckSummary> { it.is_completed }
                .thenByDescending { it.is_in_progress }
                .thenByDescending { it.due_review_count }
                .thenBy { it.id }
        )
    }
    val pageCount = ((sortedDecks.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(decks.size, pageCount) {
        if (pagerState.currentPage > pageCount - 1) {
            pagerState.scrollToPage(pageCount - 1)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Bộ từ gợi ý hôm nay",
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Trang trước")
            }
            Text(
                "${pagerState.currentPage + 1}/$pageCount",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                enabled = pagerState.currentPage < pageCount - 1,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Trang sau")
            }
        }

        if (sortedDecks.isEmpty()) {
            EmptySuggestedDeckCard()
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 12.dp
            ) { page ->
                val pageDecks = sortedDecks.drop(page * pageSize).take(pageSize)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pageDecks.forEach { deck ->
                        val mode = if (deck.due_review_count > 0) "review" else "new"
                        SuggestedDeckCard(
                            deck = deck,
                            onLearningClick = { onLearningClick(deck.id, mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(pageSize - pageDecks.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySuggestedDeckCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE7ECEA)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có bộ từ nào để gợi ý.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
fun SuggestedDeckCard(
    deck: LearningDeckSummary,
    onLearningClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = deckAccentColor(deck.id)
    val remaining = (deck.total_words - deck.learned_words).coerceAtLeast(0)
    val progress = (deck.learned_words.toFloat() / deck.total_words.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "deckProgress"
    )
    val actionText = when {
        deck.due_review_count > 0 -> "Ôn ngay"
        deck.is_completed -> "Đã xong"
        else -> "Học ngay"
    }

    Card(
        modifier = modifier
            .height(218.dp)
            .shadow(5.dp, RoundedCornerShape(14.dp), ambientColor = accent.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.12f)) {
                    Text("📖", modifier = Modifier.padding(7.dp), fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (deck.is_in_progress && !deck.is_completed) {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFE8F6F3)) {
                        Text(
                            "Đang học",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF147A70),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = deck.title,
                modifier = Modifier.height(38.dp),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("${deck.total_words} từ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (deck.is_completed) "Hoàn thành" else "Còn $remaining từ",
                color = if (deck.is_completed) Color(0xFF26A69A) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onLearningClick,
                enabled = !deck.is_completed || deck.due_review_count > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF26A69A),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE7ECEA),
                    disabledContentColor = Color(0xFF60706C)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(actionText, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

private fun deckAccentColor(deckId: Long): Color {
    val colors = listOf(
        Color(0xFF26A69A),
        Color(0xFF5E7CE2),
        Color(0xFF7E57C2),
        Color(0xFFEF6C00),
        Color(0xFF43A047)
    )
    return colors[(deckId % colors.size).toInt()]
}
