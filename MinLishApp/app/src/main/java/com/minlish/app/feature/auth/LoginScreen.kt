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
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF26A69A)     // Teal chính theo logo
    val secondaryColor = Color(0xFF4ECDC4)   // Mint Green

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo / Icon
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "MinLish Logo",
            modifier = Modifier.size(90.dp),
            tint = primaryColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Chào mừng trở lại",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
        Text(
            text = "Học tiếng Anh cùng MinLish",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Card chứa form
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                                contentDescription = null,
                                tint = primaryColor
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Error Message
                if (viewModel.loginError.isNotEmpty()) {
                    Text(
                        text = viewModel.loginError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nút Đăng nhập
                Button(
                    onClick = { viewModel.login(email, password, onLoginSuccess) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text(
                        "ĐĂNG NHẬP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Link sang Đăng ký
        TextButton(onClick = onNavigateToRegister) {
            Text(
                "Chưa có tài khoản? Đăng ký ngay",
                color = primaryColor,
                fontWeight = FontWeight.Medium
            )
        }

        // Link quên mật khẩu
        TextButton(onClick = onNavigateToForgot) {
            Text(
                "Quên mật khẩu?",
                color = primaryColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}