package com.minlish.app.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val passwordError = viewModel.validatePassword(password)
    val confirmError = viewModel.validateConfirmPassword(password, confirmPassword)

    val primaryColor = Color(0xFF26A69A)     // Teal chính theo logo
    val secondaryColor = Color(0xFF4ECDC4)   // Mint Green

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    isError = passwordError != null && password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (passwordError != null) {
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
                    isError = confirmError != null && confirmPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (confirmError != null) {
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

                Spacer(modifier = Modifier.height(8.dp))

                // Hiển thị thông báo từ ViewModel (thành công / lỗi kết nối)
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