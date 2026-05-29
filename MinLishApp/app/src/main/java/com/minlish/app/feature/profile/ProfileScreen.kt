package com.minlish.app.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = Color(0xFF26A69A)
    val secondaryColor = Color(0xFF4ECDC4)
    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    var expanded by remember { mutableStateOf(false) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarError by remember { mutableStateOf("") }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val avatarPayload = context.readProfileAvatarPayload(uri)
            if (avatarPayload == null) {
                avatarError = "Không thể đọc ảnh avatar. Vui lòng chọn ảnh khác."
            } else {
                avatarUri = uri
                avatarError = ""
                viewModel.onAvatarChange(avatarPayload.base64, avatarPayload.mimeType)
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primaryColor)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Hồ sơ người dùng", fontSize = 22.sp, color = primaryColor)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(1.dp, secondaryColor, CircleShape),
                        color = Color(0xFFE8F7F5)
                    ) {
                        when {
                            avatarUri != null -> {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            viewModel.avatarUrl != null -> {
                                AsyncImage(
                                    model = viewModel.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = primaryColor
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { avatarPicker.launch("image/*") },
                            border = BorderStroke(1.dp, primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (viewModel.avatarUrl == null && avatarUri == null) "Tải avatar" else "Đổi avatar")
                        }

                        if (viewModel.avatarUrl != null || avatarUri != null) {
                            TextButton(
                                onClick = {
                                    avatarUri = null
                                    avatarError = ""
                                    viewModel.onAvatarChange(null, null)
                                }
                            ) {
                                Text("Xóa ảnh", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (avatarError.isNotEmpty()) {
                    Text(avatarError, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = viewModel.fullName,
                    onValueChange = { viewModel.onFullNameChange(it) },
                    label = { Text("Họ và tên") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.targetGoal,
                    onValueChange = { viewModel.onTargetGoalChange(it) },
                    label = { Text("Mục tiêu học") },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    OutlinedTextField(
                        value = viewModel.level,
                        onValueChange = { },
                        label = { Text("Level hiện tại") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = primaryColor)
                            }
                        }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        levels.forEach { lv ->
                            DropdownMenuItem(
                                text = { Text(lv) },
                                onClick = {
                                    viewModel.onLevelChange(lv)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (viewModel.error.isNotEmpty()) {
                    Text(viewModel.error, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { viewModel.saveProfile(context = context, onSaved = { avatarUri = null }) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    if (viewModel.loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Lưu", fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Cài đặt nhắc nhở học tập", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = primaryColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thông báo trên thiết bị", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Nhận nhắc nhở trực tiếp từ ứng dụng", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.pushEnabled,
                        onCheckedChange = { viewModel.onPushEnabledChange(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                    )
                }

                HorizontalDivider(color = Color(0xFFE8F0ED))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thông báo qua Email", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Nhận email nhắc nhở học tập hàng ngày", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewModel.emailEnabled,
                        onCheckedChange = { viewModel.onEmailEnabledChange(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                    )
                }

                HorizontalDivider(color = Color(0xFFE8F0ED))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Giờ nhắc nhở hằng ngày", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Chọn thời gian gửi thông báo nhắc học", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    val timePickerDialog = android.app.TimePickerDialog(
                        context,
                        { _, hourOfDay, minuteOfHour ->
                            viewModel.onReminderTimeChange(hourOfDay, minuteOfHour)
                        },
                        viewModel.reminderHour,
                        viewModel.reminderMinute,
                        true
                    )

                    OutlinedButton(
                        onClick = { timePickerDialog.show() },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, primaryColor)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%02d:%02d", viewModel.reminderHour, viewModel.reminderMinute),
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE8F0ED))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.sendTestEmail() },
                        enabled = !viewModel.sendingTestEmail,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, primaryColor)
                    ) {
                        if (viewModel.sendingTestEmail) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đang gửi...")
                        } else {
                            Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gửi thử email nhắc học", color = primaryColor)
                        }
                    }

                    if (viewModel.emailTestResult.isNotEmpty()) {
                        Text(
                            text = viewModel.emailTestResult,
                            fontSize = 13.sp,
                            color = if (viewModel.emailTestResult.startsWith("Lỗi") || viewModel.emailTestResult.startsWith("Không")) Color(0xFFB00020) else Color(0xFF147A70),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        if (viewModel.successMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(viewModel.successMessage, color = primaryColor)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onLogout) {
            Text("Đăng xuất", color = Color(0xFFB00020))
        }
    }
}

private data class ProfileAvatarPayload(
    val base64: String,
    val mimeType: String
)

private const val PROFILE_AVATAR_MAX_SIZE_PX = 256
private const val PROFILE_AVATAR_JPEG_QUALITY = 70

private fun Context.readProfileAvatarPayload(uri: Uri): ProfileAvatarPayload? {
    return runCatching {
        val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return null
        val resizedBitmap = bitmap.resizeProfileAvatarToMax(PROFILE_AVATAR_MAX_SIZE_PX)
        val bytes = ByteArrayOutputStream().use { output ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, PROFILE_AVATAR_JPEG_QUALITY, output)
            output.toByteArray()
        }

        if (resizedBitmap !== bitmap) {
            resizedBitmap.recycle()
        }
        bitmap.recycle()

        ProfileAvatarPayload(
            base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            mimeType = "image/jpeg"
        )
    }.getOrNull()
}

private fun Bitmap.resizeProfileAvatarToMax(maxSizePx: Int): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= maxSizePx) return this

    val scale = maxSizePx.toFloat() / largestSide
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}
