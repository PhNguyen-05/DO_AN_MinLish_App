package com.minlish.app.feature.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val AuthPrimary = Color(0xFF22A99A)
internal val AuthPrimaryDark = Color(0xFF147A70)
internal val AuthBackground = Color(0xFFF6FAF8)
internal val AuthSurface = Color.White
internal val AuthBorder = Color(0xFFDDE7E3)
internal val AuthTextMuted = Color(0xFF61706B)
internal val AuthErrorContainer = Color(0xFFFFECEC)
internal val AuthSuccessContainer = Color(0xFFE7F7EF)

@Composable
internal fun AuthPage(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "authContentAlpha"
    )
    val cardOffset by animateDpAsState(
        targetValue = if (entered) 0.dp else 18.dp,
        animationSpec = tween(durationMillis = 520),
        label = "authCardOffset"
    )
    val logoPulse by rememberInfiniteTransition(label = "authLogoPulse").animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "authLogoScale"
    )

    LaunchedEffect(Unit) {
        entered = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AuthBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AuthMotionBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .graphicsLayer(alpha = contentAlpha)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = AuthPrimaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = AuthPrimary.copy(alpha = 0.18f),
                            spotColor = AuthPrimary.copy(alpha = 0.18f)
                        )
                        .graphicsLayer(
                            scaleX = logoPulse,
                            scaleY = logoPulse,
                            alpha = contentAlpha
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFE5F5F1)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "MinLish",
                            tint = AuthPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    color = Color(0xFF0F2724),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                    modifier = Modifier.graphicsLayer(alpha = contentAlpha)
                )
                Text(
                    text = subtitle,
                    color = AuthTextMuted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .graphicsLayer(alpha = contentAlpha)
                )

                Spacer(modifier = Modifier.height(26.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = cardOffset)
                        .graphicsLayer(alpha = contentAlpha)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color(0xFF9ECAC1).copy(alpha = 0.16f),
                            spotColor = Color(0xFF9ECAC1).copy(alpha = 0.16f)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AuthSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content
                    )
                }

                if (footer != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(modifier = Modifier.graphicsLayer(alpha = contentAlpha)) {
                        footer()
                    }
                }
            }
        }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = AuthPrimaryDark)
        },
        isError = isError,
        singleLine = singleLine,
        shape = RoundedCornerShape(8.dp),
        colors = authTextFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityChange: () -> Unit,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = AuthPrimaryDark)
        },
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                    tint = AuthTextMuted
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = isError,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = authTextFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (loading) 0.98f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "authButtonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AuthPrimary),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun AuthMessage(text: String, isError: Boolean) {
    if (text.isBlank()) return

    Surface(
        color = if (isError) AuthErrorContainer else AuthSuccessContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF16824C),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF166A42),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AuthMotionBackdrop() {
    val shift by rememberInfiniteTransition(label = "authBackdrop").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "authBackdropShift"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF4FBF8),
                        Color(0xFFF9FCFB),
                        Color(0xFFF2F7FF)
                    )
                )
            )
    ) {
        val corner = 34.dp.toPx()
        val bandHeight = 82.dp.toPx()
        val wideBand = Size(width = size.width * 1.45f, height = bandHeight)
        val narrowBand = Size(width = size.width * 0.95f, height = 52.dp.toPx())

        withTransform({
            translate(left = -size.width * 0.28f + shift * 72f, top = size.height * 0.10f)
            rotate(degrees = -9f)
        }) {
            drawRoundRect(
                color = Color(0xFF8FE4D3).copy(alpha = 0.22f),
                size = wideBand,
                cornerRadius = CornerRadius(corner, corner)
            )
        }

        withTransform({
            translate(left = size.width * 0.10f - shift * 54f, top = size.height * 0.76f)
            rotate(degrees = -12f)
        }) {
            drawRoundRect(
                color = Color(0xFFAFC7FF).copy(alpha = 0.16f),
                size = wideBand,
                cornerRadius = CornerRadius(corner, corner)
            )
        }

        withTransform({
            translate(left = size.width * 0.18f + shift * 36f, top = size.height * 0.25f)
            rotate(degrees = 12f)
        }) {
            drawRoundRect(
                color = Color(0xFFFFB7A6).copy(alpha = 0.14f),
                size = narrowBand,
                cornerRadius = CornerRadius(corner, corner)
            )
        }
    }
}

@Composable
internal fun AuthFooterLink(text: String, actionText: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, color = AuthTextMuted, fontSize = 14.sp)
        TextButton(onClick = onClick) {
            Text(actionText, color = AuthPrimaryDark, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AuthPrimary,
    unfocusedBorderColor = AuthBorder,
    focusedLabelColor = AuthPrimaryDark,
    cursorColor = AuthPrimaryDark
)
