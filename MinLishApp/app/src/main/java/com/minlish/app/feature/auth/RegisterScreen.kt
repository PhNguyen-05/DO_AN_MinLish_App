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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

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
    }

    AuthPage(
        title = "Tạo tài khoản",
        subtitle = "Thiết lập hồ sơ học tập để bắt đầu với MinLish",
        footer = {
            AuthFooterLink(
                text = "Đã có tài khoản?",
                actionText = "Đăng nhập",
                onClick = onNavigateToLogin
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(1.dp, AuthBorder, CircleShape),
                color = ColorPalette.avatarBackground
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = AuthPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    border = BorderStroke(1.dp, AuthPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AuthPrimaryDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (avatarUri == null) "Chọn avatar" else "Đổi avatar",
                        color = AuthPrimaryDark
                    )
                }

                if (avatarUri != null) {
                    TextButton(onClick = { avatarUri = null }) {
                        Text("Xóa ảnh", color = AuthTextMuted)
                    }
                }
            }
        }

        AuthMessage(text = avatarError, isError = true)

        AuthTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Họ và tên",
            leadingIcon = Icons.Default.Person
        )

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            leadingIcon = Icons.Default.Email
        )

        AuthPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Mật khẩu",
            visible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            leadingIcon = Icons.Default.Lock,
            isError = showPasswordError
        )
        AuthMessage(text = if (showPasswordError) passwordError.orEmpty() else "", isError = true)

        AuthPasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Xác nhận mật khẩu",
            visible = confirmPasswordVisible,
            onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible },
            leadingIcon = Icons.Default.Lock,
            isError = showConfirmError
        )
        AuthMessage(text = if (showConfirmError) confirmError.orEmpty() else "", isError = true)

        AuthTextField(
            value = targetGoal,
            onValueChange = { targetGoal = it },
            label = "Mục tiêu học tập",
            leadingIcon = Icons.Default.Flag
        )

        AuthMessage(text = viewModel.registerMessage, isError = false)
        AuthMessage(text = viewModel.registerError, isError = true)

        AuthPrimaryButton(
            text = "Đăng ký",
            loading = viewModel.registerLoading,
            onClick = {
                formSubmitted = true
                viewModel.clearRegisterState()

                if (passwordError != null || confirmError != null) {
                    return@AuthPrimaryButton
                }

                val avatarPayload = avatarUri?.let { context.readAvatarPayload(it) }
                if (avatarUri != null && avatarPayload == null) {
                    avatarError = "Không thể đọc ảnh avatar. Vui lòng chọn ảnh khác."
                    return@AuthPrimaryButton
                }

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
        )
    }
}

private object ColorPalette {
    val avatarBackground = androidx.compose.ui.graphics.Color(0xFFE8F6F3)
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
