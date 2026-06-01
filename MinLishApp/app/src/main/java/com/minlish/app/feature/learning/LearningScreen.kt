package com.minlish.app.feature.learning

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.VolumeUp
import com.minlish.app.utils.PronounceManager
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.minlish.app.data.model.LearningCard
import com.minlish.app.data.model.LearningPlanResponse
import com.minlish.app.data.remote.RetrofitClient

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    viewModel: LearningViewModel,
    deckId: Long? = null,
    initialMode: String? = null,
    onBack: () -> Unit
) {
    val primaryColor = Color(0xFF26A69A)
    val context = androidx.compose.ui.platform.LocalContext.current
    val pronounceManager = remember { PronounceManager(context) }

    BackHandler {
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            pronounceManager.shutdown()
        }
    }

    LaunchedEffect(deckId, initialMode) {
        viewModel.loadLearning(mode = initialMode ?: viewModel.selectedMode, deckId = deckId)
    }

    val currentCard = viewModel.currentCard
    LaunchedEffect(currentCard) {
        currentCard?.let {
            pronounceManager.pronounce(it.word, it.audio_url)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5FAF8),
        topBar = {
            TopAppBar(
                title = { Text("Học flashcard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadLearning() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Tải lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5FAF8),
                    titleContentColor = Color(0xFF102522)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            viewModel.plan?.let { plan ->
                DailyLearningPlanCard(plan = plan, primaryColor = primaryColor)
            }

            LearningModeSelector(
                selectedMode = viewModel.selectedMode,
                onSelected = viewModel::loadLearning
            )

            StatusMessage(message = viewModel.message, error = viewModel.error, primaryColor = primaryColor)

            when {
                viewModel.loading -> LoadingCard()
                viewModel.isComplete -> CompleteCard(onRestart = { viewModel.loadLearning() }, primaryColor = primaryColor)
                viewModel.currentCard != null -> {
                    Text(
                        text = "${viewModel.currentIndex + 1} / ${viewModel.cards.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    LinearProgressIndicator(
                        progress = { (viewModel.currentIndex + 1).toFloat() / viewModel.cards.size.coerceAtLeast(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = primaryColor,
                        trackColor = Color(0xFFE2EEE9)
                    )
                    Flashcard(
                        card = viewModel.currentCard!!,
                        showingBack = viewModel.showingBack,
                        onFlip = viewModel::flipCard,
                        onPronounce = {
                            viewModel.currentCard?.let { card ->
                                pronounceManager.pronounce(card.word, card.audio_url)
                            }
                        }
                    )
                    ReviewButtons(
                        enabled = !viewModel.submitting && viewModel.showingBack,
                        onReview = viewModel::submitReview
                    )
                }
                else -> EmptyLearningCard(primaryColor = primaryColor)
            }
        }
    }
}

@Composable
private fun DailyLearningPlanCard(plan: LearningPlanResponse, primaryColor: Color) {
    val newProgress by animateFloatAsState(
        targetValue = (plan.words_learned_today.toFloat() / plan.daily_new_words_goal.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "learningNewProgress"
    )
    val reviewProgress by animateFloatAsState(
        targetValue = (plan.words_reviewed_today.toFloat() / plan.daily_review_goal.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "learningReviewProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = primaryColor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color.White, Color(0xFFEAF8F4))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Kế hoạch hôm nay", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color(0xFF102522))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PlanMetric("Từ mới", "${plan.words_learned_today}/${plan.daily_new_words_goal}", primaryColor, newProgress, Modifier.weight(1f))
                PlanMetric("Cần ôn", "${plan.words_reviewed_today}/${plan.daily_review_goal}", Color(0xFF5E7CE2), reviewProgress, Modifier.weight(1f))
            }
            Text(
                text = "${plan.new_words_available} từ mới còn lại, ${plan.due_review_count} thẻ đến hạn ôn.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PlanMetric(title: String, value: String, color: Color, progress: Float, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = color,
                trackColor = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun LearningModeSelector(selectedMode: String, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ModeChip("mixed", "Tất cả", selectedMode, onSelected)
        ModeChip("new", "Từ mới", selectedMode, onSelected)
        ModeChip("review", "Ôn tập", selectedMode, onSelected)
    }
}

@Composable
private fun ModeChip(mode: String, label: String, selectedMode: String, onSelected: (String) -> Unit) {
    val selected = selectedMode == mode
    OutlinedButton(
        onClick = { onSelected(mode) },
        modifier = Modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFFE2F4EF) else Color.White,
            contentColor = if (selected) Color(0xFF147A70) else Color(0xFF4D625D)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFF26A69A) else Color(0xFFD5E4DF)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
    ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
    }
}

@Composable
private fun StatusMessage(message: String, error: String, primaryColor: Color) {
    val text = error.ifBlank { message }
    if (text.isBlank()) return
    val isError = error.isNotBlank()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = if (isError) MaterialTheme.colorScheme.error else primaryColor
        )
    }
}

@Composable
private fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text("Đang tạo phiên học...")
        }
    }
}

@Composable
private fun Flashcard(card: LearningCard, showingBack: Boolean, onFlip: () -> Unit, onPronounce: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (showingBack) 180f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "flashcardFlip"
    )
    val scale by animateFloatAsState(
        targetValue = if (showingBack) 1.015f else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "flashcardScale"
    )
    val density = LocalDensity.current.density
    val showBackContent = rotation > 90f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFF9ECAC1).copy(alpha = 0.16f))
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onFlip),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Color(0xFFDCEBE6))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        if (showBackContent) {
                            listOf(Color(0xFFEAF8F4), Color.White, Color(0xFFF3F6FF))
                        } else {
                            listOf(Color.White, Color(0xFFF2FBF8), Color(0xFFFFF8F1))
                        }
                    )
                )
                .graphicsLayer {
                    if (showBackContent) rotationY = 180f
                }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showBackContent) FlashcardBack(card = card, onPronounce = onPronounce) else FlashcardFront(card = card, onPronounce = onPronounce)
        }
    }
}

@Composable
private fun FlashcardFront(card: LearningCard, onPronounce: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(card.deck_title, color = Color(0xFF26A69A), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(18.dp))
        card.image_url?.takeIf { it.isNotBlank() }?.let { imageUrl ->
            AsyncImage(
                model = RetrofitClient.resolveServerUrl(imageUrl),
                contentDescription = card.word,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(card.word, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onPronounce) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Phát âm",
                    tint = Color(0xFF26A69A),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        card.pronunciation?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Chạm để lật thẻ", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FlashcardBack(card: LearningCard, onPronounce: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(card.word, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF26A69A))
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onPronounce,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Phát âm",
                    tint = Color(0xFF26A69A),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(card.meaning, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        card.description_en?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        card.example?.takeIf { it.isNotBlank() }?.let { LearningDetail("Ví dụ", it) }
        card.collocation?.takeIf { it.isNotBlank() }?.let { LearningDetail("Collocation", it) }
        card.note?.takeIf { it.isNotBlank() }?.let { LearningDetail("Ghi chú", it) }
    }
}

@Composable
private fun LearningDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontSize = 14.sp)
    }
}

@Composable
private fun ReviewButtons(enabled: Boolean, onReview: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ReviewButton("Again", Color(0xFFE53935), enabled, Modifier.weight(1f)) { onReview(0) }
            ReviewButton("Hard", Color(0xFFEF6C00), enabled, Modifier.weight(1f)) { onReview(1) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ReviewButton("Good", Color(0xFF26A69A), enabled, Modifier.weight(1f)) { onReview(2) }
            ReviewButton("Easy", Color(0xFF5E7CE2), enabled, Modifier.weight(1f)) { onReview(3) }
        }
        if (!enabled) {
            Text(
                text = "Lật thẻ để chọn mức độ nhớ.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ReviewButton(text: String, color: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(50.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        4.dp,
                        RoundedCornerShape(12.dp),
                        ambientColor = color.copy(alpha = 0.18f),
                        spotColor = color.copy(alpha = 0.18f)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFF0F5F3),
            disabledContentColor = Color(0xFF91A19D)
        ),
        border = if (enabled) null else BorderStroke(1.dp, Color(0xFFD8E4E0)),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompleteCard(onRestart: () -> Unit, primaryColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = primaryColor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFFE8F6F3), Color.White)))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎉", fontSize = 40.sp)
            Text("Đã hoàn thành phiên học", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Bạn đã xử lý hết các flashcard trong phiên hiện tại.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Tạo phiên mới")
            }
        }
    }
}

@Composable
private fun EmptyLearningCard(primaryColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Chưa có thẻ học", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Hãy import hoặc nạp bộ từ vựng trước khi bắt đầu học.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = {}, enabled = false) {
                Text("Đang chờ dữ liệu", color = primaryColor)
            }
        }
    }
}
