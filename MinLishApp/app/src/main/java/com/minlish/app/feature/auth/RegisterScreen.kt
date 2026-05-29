package com.minlish.app.feature.auth
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var targetGoal by remember { mutableStateOf("TOEIC 700") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var formSubmitted by remember { mutableStateOf(false) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarError by remember { mutableStateOf("") }

    val context = LocalContext.current
    val passwordError = viewModel.validatePassword(password)
    val confirmError = viewModel.validateConfirmPassword(password, confirmPassword)
    val showPasswordError = formSubmitted && passwordError != null
    val showConfirmError = formSubmitted && confirmError != null

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        avatarUri = uri
        avatarError = ""
    }

    LaunchedEffect(Unit) {
        viewModel.clearRegisterState()
        formSubmitted = false
        avatarError = ""
    }

    LaunchedEffect(viewModel.registerError) {
        if (viewModel.registerError.isNotEmpty()) {
            Toast.makeText(context, viewModel.registerError, Toast.LENGTH_SHORT).show()
        }
    }

    val primaryColor = Color(0xFF26A69A)
    val secondaryColor = Color(0xFF4ECDC4)


    val passwordError = viewModel.validatePassword(password)
    val confirmError = viewModel.validateConfirmPassword(password, confirmPassword)

    val primaryColor = Color(0xFF26A69A)     // Teal chính theo logo
    val secondaryColor = Color(0xFF4ECDC4)   // Mint Green


    Column(
        modifier = Modifier
            .fillMaxSize()

            .verticalScroll(rememberScrollState())

            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo/Icon

        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "MinLish",
            modifier = Modifier.size(90.dp),
            tint = primaryColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tạo tài khoản MinLish",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
        Text(
            text = "Học tiếng Anh thông minh hơn mỗi ngày",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(1.dp, secondaryColor, CircleShape),
                        color = Color(0xFFE8F7F5)
                    ) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp),
                                    tint = primaryColor
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            border = BorderStroke(1.dp, primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (avatarUri == null) "Tải avatar" else "Đổi avatar")
                        }

                        if (avatarUri != null) {
                            TextButton(onClick = { avatarUri = null }) {
                                Text("Xóa ảnh", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (avatarError.isNotEmpty()) {
                    Text(avatarError, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Họ và tên

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Email

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Mật khẩu

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryColor) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                tint = primaryColor
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),

                    isError = showPasswordError,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showPasswordError) {
                    Text(passwordError.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }


                    isError = passwordError != null && password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (passwordError != null && password.isNotEmpty()) {
                    Text(passwordError, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                // Xác nhận mật khẩu

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Xác nhận mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryColor) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                tint = primaryColor
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),

                    isError = showConfirmError,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showConfirmError) {
                    Text(confirmError.orEmpty(), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                    isError = confirmError != null && confirmPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (confirmError != null && confirmPassword.isNotEmpty()) {
                    Text(confirmError, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                // Mục tiêu học

                OutlinedTextField(
                    value = targetGoal,
                    onValueChange = { targetGoal = it },
                    label = { Text("Mục tiêu học tập") },
                    leadingIcon = { Icon(Icons.Default.Flag, null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (viewModel.registerMessage.isNotEmpty()) {
                    Text(
                        viewModel.registerMessage,
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp
                    )
                }

                if (viewModel.registerError.isNotEmpty()) {
                    Text(viewModel.registerError, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        formSubmitted = true
                        viewModel.clearRegisterState()

                        if (passwordError == null && confirmError == null) {
                            val avatarPayload = avatarUri?.let { context.readAvatarPayload(it) }
                            if (avatarUri != null && avatarPayload == null) {
                                avatarError = "Không thể đọc ảnh avatar. Vui lòng chọn ảnh khác."
                            } else {
                                avatarError = ""
                                viewModel.registerWithAvatar(
                                    email = email,
                                    password = password,
                                    fullName = fullName,
                                    targetGoal = targetGoal,
                                    avatarBase64 = avatarPayload?.base64,
                                    avatarMimeType = avatarPayload?.mimeType,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Đăng ký thành công. Vui lòng đăng nhập.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onNavigateToLogin()
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (passwordError == null && confirmError == null) {
                            viewModel.register(email, password, fullName, targetGoal)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),

                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("ĐĂNG KÝ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Đã có tài khoản? Đăng nhập ngay", color = primaryColor)
        }
    }
}

private data class AvatarPayload(
    val base64: String,
    val mimeType: String?
)

private const val AVATAR_MAX_SIZE_PX = 256
private const val AVATAR_JPEG_QUALITY = 70

private fun Context.readAvatarPayload(uri: Uri): AvatarPayload? {
    return runCatching {
        val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return null
        val resizedBitmap = bitmap.resizeToMax(AVATAR_MAX_SIZE_PX)
        val bytes = ByteArrayOutputStream().use { output ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, output)
            output.toByteArray()
        }

        if (resizedBitmap !== bitmap) {
            resizedBitmap.recycle()
        }
        bitmap.recycle()

        AvatarPayload(
            base64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
            mimeType = "image/jpeg"
        )
    }.getOrNull()
}

private fun Bitmap.resizeToMax(maxSizePx: Int): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= maxSizePx) return this

    val scale = maxSizePx.toFloat() / largestSide
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}
}

