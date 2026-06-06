package com.minlish.app.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.minlish.app.data.model.LearningCard
import com.minlish.app.data.model.LearningDeckSummary
import com.minlish.app.data.remote.RetrofitClient
import com.minlish.app.utils.PronounceManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onBack: () -> Unit
) {
    val primaryColor = Color(0xFF26A69A)
    val context = LocalContext.current
    val pronounceManager = remember { PronounceManager(context) }

    LaunchedEffect(Unit) {
        viewModel.loadDecks()
    }

    LaunchedEffect(viewModel.currentListenCard, viewModel.selectedMode) {
        if (viewModel.selectedMode == PracticeMode.LISTENING) {
            viewModel.currentListenCard?.let { card ->
                pronounceManager.pronounce(card.word, card.audio_url)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { pronounceManager.shutdown() }
    }

    fun navigateBack() {
        when {
            viewModel.selectedMode != null -> viewModel.clearMode()
            viewModel.selectedDeck != null -> viewModel.clearDeckSelection()
            else -> onBack()
        }
    }

    BackHandler { navigateBack() }

    Scaffold(
        containerColor = Color(0xFFF5FAF8),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.selectedMode?.title
                            ?: viewModel.selectedDeck?.title
                            ?: "Luyện tập",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val deck = viewModel.selectedDeck
                            if (deck == null) viewModel.loadDecks() else viewModel.selectDeck(deck)
                        }
                    ) {
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
            PracticeStatus(
                message = viewModel.message,
                error = viewModel.error,
                primaryColor = primaryColor
            )

            when {
                viewModel.loading -> PracticeLoadingCard()
                viewModel.selectedDeck == null -> PracticeDeckPicker(
                    decks = viewModel.decks,
                    onDeckSelected = viewModel::selectDeck
                )
                viewModel.selectedMode == null -> PracticeModePicker(
                    deck = viewModel.selectedDeck!!,
                    cards = viewModel.cards,
                    onModeSelected = viewModel::startMode
                )
                viewModel.selectedMode == PracticeMode.QUIZ -> QuizPractice(
                    viewModel = viewModel,
                    primaryColor = primaryColor
                )
                viewModel.selectedMode == PracticeMode.MATCHING -> MatchingPractice(
                    viewModel = viewModel,
                    primaryColor = primaryColor
                )
                viewModel.selectedMode == PracticeMode.LISTENING -> ListeningPractice(
                    viewModel = viewModel,
                    primaryColor = primaryColor,
                    onPronounce = {
                        viewModel.currentListenCard?.let { card ->
                            pronounceManager.pronounce(card.word, card.audio_url)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PracticeDeckPicker(
    decks: List<LearningDeckSummary>,
    onDeckSelected: (LearningDeckSummary) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Chọn chủ đề đã học", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF102522))
        Text(
            "Các chủ đề xuất hiện ở đây khi bạn đã học ít nhất một từ trong chủ đề đó.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        if (decks.isEmpty()) {
            EmptyPracticeCard(
                title = "Chưa có chủ đề đã học",
                body = "Hãy học một vài flashcard trước, sau đó quay lại luyện tập."
            )
        } else {
            decks.forEach { deck ->
                PracticeDeckCard(deck = deck, onClick = { onDeckSelected(deck) })
            }
        }
    }
}

@Composable
private fun PracticeDeckCard(deck: LearningDeckSummary, onClick: () -> Unit) {
    val accent = practiceAccentColor(deck.id)
    val progress = (deck.learned_words.toFloat() / deck.total_words.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "practiceDeckProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = accent.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.12f)) {
                    Text("Topic", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(deck.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${deck.learned_words}/${deck.total_words} từ đã học",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = accent,
                trackColor = accent.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun PracticeModePicker(
    deck: LearningDeckSummary,
    cards: List<LearningCard>,
    onModeSelected: (PracticeMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDDEBE7))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(deck.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF102522))
                Text("${cards.size} từ đã học sẵn sàng luyện tập", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text("Chọn hình thức luyện tập", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

        PracticeModeCard(
            icon = Icons.Default.CheckCircle,
            mode = PracticeMode.QUIZ,
            enabled = cards.size >= 4,
            disabledReason = "Cần ít nhất 4 từ đã học.",
            onClick = { onModeSelected(PracticeMode.QUIZ) }
        )
        PracticeModeCard(
            icon = Icons.Default.Link,
            mode = PracticeMode.MATCHING,
            enabled = cards.size >= 2,
            disabledReason = "Cần ít nhất 2 từ đã học.",
            onClick = { onModeSelected(PracticeMode.MATCHING) }
        )
        PracticeModeCard(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            mode = PracticeMode.LISTENING,
            enabled = cards.isNotEmpty(),
            disabledReason = "Chưa có từ đã học.",
            onClick = { onModeSelected(PracticeMode.LISTENING) }
        )
    }
}

@Composable
private fun PracticeModeCard(
    icon: ImageVector,
    mode: PracticeMode,
    enabled: Boolean,
    disabledReason: String,
    onClick: () -> Unit
) {
    val borderColor = if (enabled) Color(0xFFBFE4DC) else Color(0xFFE1E8E5)
    val iconColor = if (enabled) Color(0xFF26A69A) else Color(0xFF9BA9A5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = iconColor.copy(alpha = 0.12f)) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(10.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(mode.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF102522))
                Text(
                    if (enabled) mode.description else disabledReason,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun QuizPractice(viewModel: PracticeViewModel, primaryColor: Color) {
    val question = viewModel.currentQuizQuestion
    if (question == null) {
        EmptyPracticeCard("Chưa có câu hỏi", "Hãy chọn lại hình thức luyện tập hoặc tải lại chủ đề.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PracticeProgressHeader(
            current = viewModel.quizIndex + 1,
            total = viewModel.quizQuestions.size,
            score = viewModel.quizScore,
            color = primaryColor
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDDEBE7))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(999.dp), color = primaryColor.copy(alpha = 0.12f)) {
                    Text(
                        "Điền từ phù hợp nhất",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryColor
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEAF8F4),
                    border = BorderStroke(1.dp, Color(0xFFCFE8E1))
                ) {
                    Text(
                        question.prompt,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 23.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF102522)
                    )
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF7FBFA)) {
                    Text(
                        "Nghĩa: ${question.card.meaning}",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        question.options.forEach { option ->
            QuizOptionButton(
                text = option,
                isSelected = viewModel.selectedQuizAnswer == option,
                isCorrectAnswer = question.answer == option,
                answered = viewModel.selectedQuizAnswer != null,
                onClick = { viewModel.submitQuizAnswer(option) }
            )
        }

        if (viewModel.selectedQuizAnswer != null) {
            Button(
                onClick = viewModel::nextQuizQuestion,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(if (viewModel.quizIndex == viewModel.quizQuestions.lastIndex) "Hoàn thành" else "Câu tiếp theo")
            }
        }
    }
}

@Composable
private fun QuizOptionButton(
    text: String,
    isSelected: Boolean,
    isCorrectAnswer: Boolean,
    answered: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        answered && isCorrectAnswer -> Color(0xFFE4F7EC)
        answered && isSelected -> Color(0xFFFFEBEE)
        else -> Color.White
    }
    val border = when {
        answered && isCorrectAnswer -> Color(0xFF43A047)
        answered && isSelected -> Color(0xFFE53935)
        else -> Color(0xFFDDEBE7)
    }
    val content = when {
        answered && isCorrectAnswer -> Color(0xFF2E7D32)
        answered && isSelected -> Color(0xFFC62828)
        else -> Color(0xFF263A36)
    }

    OutlinedButton(
        onClick = onClick,
        enabled = !answered,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = background,
            contentColor = content,
            disabledContainerColor = background,
            disabledContentColor = content
        ),
        border = BorderStroke(1.dp, border),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MatchingPractice(viewModel: PracticeViewModel, primaryColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PracticeProgressHeader(
            current = viewModel.matchedIds.size,
            total = viewModel.matchWords.size,
            score = (viewModel.matchAttempts - viewModel.matchMistakes).coerceAtLeast(0),
            color = primaryColor
        )

        Text("Chạm một từ bên trái rồi chạm nghĩa đúng bên phải.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Từ", fontWeight = FontWeight.SemiBold, color = Color(0xFF102522))
                viewModel.matchWords
                    .filterNot { it.id in viewModel.matchedIds }
                    .forEach { tile ->
                        MatchingTile(
                            tile = tile,
                            selected = viewModel.selectedMatchWordId == tile.id,
                            correct = tile.id in viewModel.correctMatchIds,
                            wrong = tile.id in viewModel.wrongMatchIds,
                            onClick = { viewModel.selectMatchWord(tile.id) }
                        )
                    }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nghĩa", fontWeight = FontWeight.SemiBold, color = Color(0xFF102522))
                viewModel.matchMeanings
                    .filterNot { it.id in viewModel.matchedIds }
                    .forEach { tile ->
                        MatchingTile(
                            tile = tile,
                            selected = false,
                            correct = tile.id in viewModel.correctMatchIds,
                            wrong = tile.id in viewModel.wrongMatchIds,
                            onClick = { viewModel.selectMatchMeaning(tile.id) }
                        )
                    }
            }
        }

        if (viewModel.matchedIds.size == viewModel.matchWords.size && viewModel.matchWords.isNotEmpty()) {
            Button(
                onClick = viewModel::finishMatchingPractice,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Hoàn thành")
            }
            Button(
                onClick = { viewModel.startMode(PracticeMode.MATCHING) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Chơi lại")
            }
        }
    }
}

@Composable
private fun MatchingTile(
    tile: MatchTile,
    selected: Boolean,
    correct: Boolean,
    wrong: Boolean,
    onClick: () -> Unit
) {
    val shake = remember { Animatable(0f) }
    val background = when {
        correct -> Color(0xFFE4F7EC)
        wrong -> Color(0xFFFFEBEE)
        selected -> Color(0xFFE2F4EF)
        else -> Color.White
    }
    val border = when {
        correct -> Color(0xFF43A047)
        wrong -> Color(0xFFE53935)
        selected -> Color(0xFF26A69A)
        else -> Color(0xFFDDEBE7)
    }

    LaunchedEffect(wrong) {
        if (wrong) {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    0f at 0
                    -1f at 55
                    1f at 110
                    -1f at 165
                    1f at 220
                    -0.65f at 285
                    0.35f at 350
                    0f at 420
                }
            )
        } else {
            shake.snapTo(0f)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = (shake.value * 8).dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Box(modifier = Modifier.heightIn(min = 64.dp).padding(10.dp), contentAlignment = Alignment.Center) {
            Text(
                tile.text,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (tile.text.length > 22) 14.sp else 16.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun ListeningPractice(
    viewModel: PracticeViewModel,
    primaryColor: Color,
    onPronounce: () -> Unit
) {
    val card = viewModel.currentListenCard
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(card?.id, viewModel.listenAnswered) {
        if (card != null && !viewModel.listenAnswered) {
            delay(260)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    if (card == null) {
        EmptyPracticeCard("Chưa có câu nghe", "Hãy chọn lại hình thức luyện tập hoặc tải lại chủ đề.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PracticeProgressHeader(
            current = viewModel.listenIndex + 1,
            total = viewModel.listenCards.size,
            score = viewModel.listenScore,
            color = primaryColor
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDDEBE7))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                card.image_url?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    AsyncImage(
                        model = RetrofitClient.resolveServerUrl(imageUrl),
                        contentDescription = card.word,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                IconButton(
                    onClick = onPronounce,
                    modifier = Modifier
                        .size(66.dp)
                        .background(Color(0xFFE2F4EF), RoundedCornerShape(18.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Phát lại", tint = primaryColor, modifier = Modifier.size(34.dp))
                }
                ListeningAnswerInput(
                    word = card.word,
                    value = viewModel.listenInput,
                    enabled = !viewModel.listenAnswered,
                    focusRequester = focusRequester,
                    primaryColor = primaryColor,
                    onValueChange = viewModel::updateListenInput,
                    onDone = {
                        viewModel.checkListenAnswer()
                        keyboardController?.hide()
                    }
                )
                Text("Nghe kỹ rồi nhập từ bạn vừa nghe.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }

        if (false) OutlinedTextField(
            value = viewModel.listenInput,
            onValueChange = viewModel::updateListenInput,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Từ vừa nghe") },
            singleLine = true,
            enabled = !viewModel.listenAnswered
        )

        val hint = viewModel.listenFeedback.ifBlank { viewModel.listenHint }
        if (hint.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (viewModel.listenAnswered) Color(0xFFE4F7EC) else Color(0xFFFFF4E5)
            ) {
                Text(
                    hint,
                    modifier = Modifier.padding(12.dp),
                    color = if (viewModel.listenAnswered) Color(0xFF2E7D32) else Color(0xFF8B5A00)
                )
            }
        }

        if (false) OnScreenKeyboard(
            enabled = !viewModel.listenAnswered,
            onKey = viewModel::appendListenInput,
            onBackspace = viewModel::deleteListenInput,
            onClear = viewModel::clearListenInput
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = viewModel::checkListenAnswer,
                enabled = !viewModel.listenAnswered,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, primaryColor)
            ) {
                Text("Kiểm tra", color = primaryColor)
            }
            Button(
                onClick = viewModel::nextListenQuestion,
                enabled = viewModel.listenAnswered,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(if (viewModel.listenIndex == viewModel.listenCards.lastIndex) "Hoàn thành" else "Câu tiếp")
            }
        }
    }
}

@Composable
private fun ListeningAnswerInput(
    word: String,
    value: String,
    enabled: Boolean,
    focusRequester: FocusRequester,
    primaryColor: Color,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = { if (enabled) onValueChange(it) },
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (enabled) Color.White else Color(0xFFEAF8F4),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(1.dp)) {
                        innerTextField()
                    }
                    Text(
                        text = listeningAnswerDisplay(word, value),
                        color = if (value.isBlank()) Color(0xFF102522) else primaryColor,
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable
private fun OnScreenKeyboard(
    enabled: Boolean,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM").forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { char ->
                    KeyboardLetterButton(
                        text = char.toString(),
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onKey(char.lowercaseChar().toString()) }
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyboardActionButton("Space", enabled, Modifier.weight(1.4f)) { onKey(" ") }
            OutlinedButton(
                onClick = onBackspace,
                enabled = enabled,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Xóa", modifier = Modifier.size(18.dp))
            }
            KeyboardActionButton("Clear", enabled, Modifier.weight(1f), onClear)
        }
    }
}

@Composable
private fun KeyboardLetterButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KeyboardActionButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Text(text, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun PracticeProgressHeader(current: Int, total: Int, score: Int, color: Color) {
    val progress = (current.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$current / $total", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Color(0xFF102522))
            Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
                Text(
                    "Điểm $score",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = Color(0xFFE2EEE9)
        )
    }
}

@Composable
private fun PracticeStatus(message: String, error: String, primaryColor: Color) {
    val text = error.ifBlank { message }
    if (text.isBlank()) return

    val isError = error.isNotBlank()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else primaryColor.copy(alpha = 0.1f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            color = if (isError) MaterialTheme.colorScheme.error else primaryColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PracticeLoadingCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text("Đang chuẩn bị dữ liệu luyện tập...")
        }
    }
}

@Composable
private fun EmptyPracticeCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDDEBE7))
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color.White, Color(0xFFEAF8F4))))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF102522), textAlign = TextAlign.Center)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

private fun blankAnswer(word: String): String {
    return word.trim()
        .replace("_", " ")
        .map { char -> if (char.isWhitespace()) "  " else "_" }
        .joinToString(" ")
}

private fun listeningAnswerDisplay(word: String, value: String): String {
    val expected = word.trim().replace("_", " ")
    val typed = value.replace("_", " ")
    val chars = expected.mapIndexed { index, expectedChar ->
        when {
            expectedChar.isWhitespace() -> " "
            index < typed.length && !typed[index].isWhitespace() -> typed[index].toString()
            else -> "_"
        }
    }.toMutableList()

    if (typed.length > expected.length) {
        chars.add(" ")
        chars.add(typed.drop(expected.length))
    }

    return chars.joinToString(" ")
}

private fun practiceAccentColor(deckId: Long): Color {
    val colors = listOf(
        Color(0xFF26A69A),
        Color(0xFF5E7CE2),
        Color(0xFF7E57C2),
        Color(0xFFEF6C00),
        Color(0xFF43A047)
    )
    return colors[Math.floorMod(deckId, colors.size.toLong()).toInt()]
}
