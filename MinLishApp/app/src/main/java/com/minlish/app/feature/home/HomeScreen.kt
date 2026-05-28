//package com.minlish.app.feature.home
//
//import androidx.compose.animation.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import coil.compose.AsyncImage
//import androidx.compose.ui.graphics.vector.ImageVector
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun HomeScreen(viewModel: HomeViewModel) {
//    LaunchedEffect(Unit) {
//        viewModel.fetchDashboardData()
//    }
//
//    val data = viewModel.dashboardState
//    var isVisible by remember { mutableStateOf(false) }
//
//    LaunchedEffect(Unit) {
//        isVisible = true
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("MinLish", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
//                actions = {
//                    IconButton(onClick = { /* Navigate to Profile */ }) {
//                        AsyncImage(
//                            model = "https://i.pravatar.cc/150?u=${data?.full_name ?: "user"}",
//                            contentDescription = "Avatar",
//                            modifier = Modifier
//                                .size(40.dp)
//                                .clip(CircleShape),
//                            contentScale = ContentScale.Crop
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer
//                )
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(20.dp)
//                .verticalScroll(rememberScrollState()), // ← Cho phép cuộn xuống
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//
//            // Greeting
//            AnimatedVisibility(visible = isVisible, enter = fadeIn() + slideInVertically()) {
//                Text(
//                    text = "Chào mừng trở lại, ${data?.full_name?.split(" ")?.first() ?: "Bạn"} 👋",
//                    fontSize = 26.sp,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//
//            // Streak Card
//            AnimatedStreakCard(currentStreak = data?.current_streak ?: 0)
//
//            // Statistics Row
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                StatCard("📚", "Từ đã học", "${data?.total_words_learned ?: 0}", modifier = Modifier.weight(1f))
//                StatCard("🎯", "Chính xác", "${(data?.accuracy_rate?.times(100) ?: 0).toInt()}%", modifier = Modifier.weight(1f))
//            }
//
//            // Daily Goal
//            DailyGoalCard(dailyGoal = data?.daily_new_words_goal ?: 20)
//
//            // Quick Actions
//            Text(
//                text = "Hành động nhanh",
//                fontSize = 18.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//            QuickActionsRow()
//
//            // Suggested Decks
//            Text(
//                text = "Bộ từ gợi ý hôm nay",
//                fontSize = 18.sp,
//                fontWeight = FontWeight.SemiBold
//            )
//            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                items(sampleSuggestedDecks) { deck ->
//                    SuggestedDeckCard(deck)
//                }
//            }
//
//            Spacer(modifier = Modifier.height(40.dp))
//        }
//    }
//}
//
//// ==================== COMPONENTS ====================
//
//@Composable
//fun AnimatedStreakCard(currentStreak: Int) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
//    ) {
//        Row(
//            modifier = Modifier.padding(20.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text("🔥", fontSize = 48.sp)
//            Spacer(modifier = Modifier.width(16.dp))
//            Column {
//                Text("Chuỗi học hiện tại", fontSize = 16.sp, color = Color(0xFFEF6C00))
//                Text(
//                    "$currentStreak ngày liên tiếp",
//                    fontSize = 28.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Color(0xFFEF6C00)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun StatCard(icon: String, title: String, value: String, modifier: Modifier = Modifier) {
//    Card(
//        modifier = modifier, // Nhận modifier chia tỷ lệ từ bên ngoài ném vào
//        shape = RoundedCornerShape(16.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp).fillMaxWidth(), // Thêm fillMaxWidth để căn giữa đẹp hơn
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(icon, fontSize = 32.sp)
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
//            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//        }
//    }
//}
//
//@Composable
//fun DailyGoalCard(dailyGoal: Int) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp)
//    ) {
//        Column(modifier = Modifier.padding(20.dp)) {
//            Text("Mục tiêu hôm nay", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
//            Spacer(modifier = Modifier.height(12.dp))
//            LinearProgressIndicator(
//                progress = { 0.65f },
//                modifier = Modifier.fillMaxWidth()
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text("13 / $dailyGoal từ mới", color = MaterialTheme.colorScheme.onSurfaceVariant)
//        }
//    }
//}
//
//@Composable
//fun QuickActionsRow() {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        QuickActionButton(
//            icon = Icons.Default.Add,
//            text = "Từ mới",
//            onClick = { /* TODO */ },
//            modifier = Modifier.weight(1f) // Truyền weight ở đây
//        )
//        QuickActionButton(
//            icon = Icons.Default.Replay,
//            text = "Ôn tập",
//            onClick = { /* TODO */ },
//            modifier = Modifier.weight(1f) // Truyền weight ở đây
//        )
//        QuickActionButton(
//            icon = Icons.Default.Edit,
//            text = "Luyện tập",
//            onClick = { /* TODO */ },
//            modifier = Modifier.weight(1f) // Truyền weight ở đây
//        )
//    }
//}
//
//@Composable
//fun QuickActionButton(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
//    OutlinedButton(
//        onClick = onClick,
//        modifier = modifier, // Gán modifier nhận được vào OutlinedButton
//        shape = RoundedCornerShape(12.dp),
//        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp) // Tránh tràn chữ trên màn hình nhỏ
//    ) {
//        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
//        Spacer(modifier = Modifier.width(6.dp))
//        Text(text, fontSize = 14.sp)
//    }
//}
//
//// Suggested Decks
//data class SuggestedDeck(val title: String, val count: Int, val color: Color)
//
//val sampleSuggestedDecks = listOf(
//    SuggestedDeck("IELTS Academic", 245, Color(0xFF4CAF50)),
//    SuggestedDeck("Business English", 178, Color(0xFF2196F3)),
//    SuggestedDeck("Phrasal Verbs", 92, Color(0xFFFF9800)),
//    SuggestedDeck("TOEIC 800+", 156, Color(0xFF9C27B0))
//)
//
//@Composable
//fun SuggestedDeckCard(deck: SuggestedDeck) {
//    Card(
//        modifier = Modifier
//            .width(160.dp)
//            .height(190.dp),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(containerColor = deck.color.copy(alpha = 0.12f))
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            Text("📖", fontSize = 32.sp)
//            Text(deck.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
//            Text("${deck.count} từ", color = MaterialTheme.colorScheme.onSurfaceVariant)
//
//            Button(
//                onClick = { /* TODO */ },
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(12.dp)
//            ) {
//                Text("Học ngay", fontSize = 13.sp)
//            }
//        }
//    }
//}
package com.minlish.app.feature.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.animation.core.tween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    LaunchedEffect(Unit) {
        viewModel.fetchDashboardData()
    }

    val data = viewModel.dashboardState
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val primaryColor = Color(0xFF26A69A)     // Teal theo logo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MinLish", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = { /* Navigate to Profile */ }) {
                        AsyncImage(
                            model = "https://i.pravatar.cc/150?u=${data?.full_name ?: "user"}",
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor.copy(alpha = 0.1f)
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

            // Greeting với animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(animationSpec = tween(600))
            ) {
                Text(
                    text = "Chào mừng trở lại, ${data?.full_name?.split(" ")?.first() ?: "Bạn"} 👋",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            // Streak Card
            AnimatedStreakCard(currentStreak = data?.current_streak ?: 0)

            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("📚", "Từ đã học", "${data?.total_words_learned ?: 0}", modifier = Modifier.weight(1f))
                StatCard("🎯", "Chính xác", "${(data?.accuracy_rate?.times(100) ?: 0).toInt()}%", modifier = Modifier.weight(1f))
            }

            // Daily Goal
            DailyGoalCard(dailyGoal = data?.daily_new_words_goal ?: 20)

            // Quick Actions
            Text("Hành động nhanh", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            QuickActionsRow()

            // Continue Learning
            Text("Tiếp tục học", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            ContinueLearningCard()

            // Suggested Decks
            Text("Bộ từ gợi ý hôm nay", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sampleSuggestedDecks) { deck ->
                    SuggestedDeckCard(deck)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ==================== COMPONENTS ====================

@Composable
fun AnimatedStreakCard(currentStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔥", fontSize = 48.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Chuỗi học hiện tại", fontSize = 16.sp, color = Color(0xFFEF6C00))
                Text(
                    "$currentStreak ngày liên tiếp",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF6C00)
                )
            }
        }
    }
}

@Composable
fun StatCard(icon: String, title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier, // Sửa từ Modifier.weight(1f) thành modifier nhận từ cha
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DailyGoalCard(dailyGoal: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Mục tiêu hôm nay", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.65f },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF26A69A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("13 / $dailyGoal từ mới", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuickActionsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Truyền quyền chia tỷ lệ trực tiếp tại đây
        QuickActionButton(Icons.Default.Add, "Từ mới", modifier = Modifier.weight(1f))
        QuickActionButton(Icons.Default.Replay, "Ôn tập", modifier = Modifier.weight(1f))
        QuickActionButton(Icons.Default.Edit, "Luyện tập", modifier = Modifier.weight(1f))
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    val primaryColor = Color(0xFF26A69A)
    OutlinedButton(
        onClick = { /* TODO */ },
        modifier = modifier, // Gán modifier nhận từ scope Row của cha vào đây
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp) // Chống tràn chữ trên màn hình hẹp
    ) {
        Icon(icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.6.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun ContinueLearningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("📌 Đang học: Business English", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Bạn đã học 12/30 từ trong bộ này", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { /* TODO: Navigate to learning screen */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A))
            ) {
                Text("Tiếp tục học ngay")
            }
        }
    }
}

// Suggested Decks
data class SuggestedDeck(val title: String, val count: Int, val color: Color)

val sampleSuggestedDecks = listOf(
    SuggestedDeck("IELTS Academic", 245, Color(0xFF4CAF50)),
    SuggestedDeck("Business English", 178, Color(0xFF2196F3)),
    SuggestedDeck("Phrasal Verbs", 92, Color(0xFFFF9800)),
    SuggestedDeck("TOEIC 800+", 156, Color(0xFF9C27B0))
)

@Composable
fun SuggestedDeckCard(deck: SuggestedDeck) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(190.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = deck.color.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📖", fontSize = 32.sp)
            Text(deck.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("${deck.count} từ", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A))
            ) {
                Text("Học ngay", fontSize = 13.sp)
            }
        }
    }
}