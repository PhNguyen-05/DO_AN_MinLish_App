package com.minlish.app.feature.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.minlish.app.BuildConfig
import kotlinx.coroutines.launch

private const val TAG = "GoogleSignIn"

@Composable
internal fun rememberGoogleSignInHandler(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    return remember(context, credentialManager, onIdToken, onError) {
        {
            val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
            if (serverClientId.isBlank()) {
                onError("Chưa cấu hình GOOGLE_WEB_CLIENT_ID cho đăng nhập Google.")
            } else {
                scope.launch {
                    try {
                        val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleOption)
                            .build()

                        val result = credentialManager.getCredential(
                            context = context.findActivity() ?: context,
                            request = request
                        )
                        val credential = result.credential

                        if (
                            credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            onIdToken(googleCredential.idToken)
                        } else {
                            onError("Không nhận được tài khoản Google hợp lệ. Vui lòng thử lại.")
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Invalid Google ID token response", e)
                        onError("Mã xác thực Google không hợp lệ. Vui lòng thử lại.")
                    } catch (e: NoCredentialException) {
                        Log.e(TAG, "No Google account is available", e)
                        onError("Không tìm thấy tài khoản Google khả dụng.")
                    } catch (e: GetCredentialException) {
                        Log.e(TAG, "Google credential request failed", e)
                        onError("Không thể mở đăng nhập Google. Vui lòng thử lại.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Google sign-in failed", e)
                        onError("Đăng nhập Google thất bại. Vui lòng thử lại.")
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
