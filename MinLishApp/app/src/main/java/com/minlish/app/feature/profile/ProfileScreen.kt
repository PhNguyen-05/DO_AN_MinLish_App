package com.minlish.app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Đã cập nhật import mới
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val primaryColor = Color(0xFF26A69A)
    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                // Đã sửa sang Icons.AutoMirrored.Filled.ArrowBack để hết cảnh báo deprecated
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
                OutlinedTextField(
                    value = viewModel.fullName,
                    onValueChange = { viewModel.onFullNameChange(it) }, // Đã sửa từ setFullName
                    label = { Text("Họ và tên") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.targetGoal,
                    onValueChange = { viewModel.onTargetGoalChange(it) }, // Đã sửa từ setTargetGoal
                    label = { Text("Mục tiêu học") },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Level selector
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
                                    viewModel.onLevelChange(lv) // Đã sửa từ setLevel
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
                    onClick = { viewModel.saveProfile() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
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

        if (viewModel.successMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(viewModel.successMessage, color = primaryColor)
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Nút đăng xuất
        TextButton(onClick = onLogout) {
            Text("Đăng xuất", color = Color(0xFFB00020))
        }
    }
}