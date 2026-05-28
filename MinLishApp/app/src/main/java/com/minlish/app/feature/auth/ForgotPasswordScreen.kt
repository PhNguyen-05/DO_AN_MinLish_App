package com.minlish.app.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateToReset: (String) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    val primaryColor = Color(0xFF26A69A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primaryColor)
            }
        }

        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "MinLish Logo",
            modifier = Modifier.size(80.dp),
            tint = primaryColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quên mật khẩu",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (viewModel.forgotError.isNotEmpty()) {
                    Text(viewModel.forgotError, color = MaterialTheme.colorScheme.error)
                }

                if (viewModel.forgotMessage.isNotEmpty()) {
                    Text(viewModel.forgotMessage, color = primaryColor)
                }

                Button(
                    onClick = {
                        val normalizedEmail = email.trim()
                        viewModel.sendForgotPassword(
                            normalizedEmail,
                            onSent = { onNavigateToReset(normalizedEmail) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Gửi mã OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
