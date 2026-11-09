package com.pisheblok.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuthException
import com.pisheblok.app.AppContainer
import com.pisheblok.app.R
import kotlinx.coroutines.launch

private enum class AuthMode { LOGIN, REGISTER }

@Composable
fun AuthScreen(container: AppContainer) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var mode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var password2 by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf("") }
    var infoText by rememberSaveable { mutableStateOf("") }

    val error = errorText.takeIf { it.isNotEmpty() }
    val info = infoText.takeIf { it.isNotEmpty() }
    val isLogin = mode == AuthMode.LOGIN

    fun fail(e: Throwable) {
        loading = false
        errorText = authErrorText(e, context)
        infoText = ""
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🥘", fontSize = 42.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.auth_welcome),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(if (isLogin) R.string.auth_title_login else R.string.auth_title_register),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (error != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                        }
                    }
                    if (info != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(info, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                        }
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorText = ""; infoText = "" },
                        label = { Text(stringResource(R.string.auth_email)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorText = ""; infoText = "" },
                        label = { Text(stringResource(R.string.auth_password)) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!isLogin) {
                        OutlinedTextField(
                            value = password2,
                            onValueChange = { password2 = it; errorText = "" },
                            label = { Text(stringResource(R.string.auth_password_repeat)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Button(
                        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                        onClick = {
                            errorText = ""
                            infoText = ""
                            if (!isLogin && password != password2) {
                                errorText = context.getString(R.string.auth_error_password_match)
                                return@Button
                            }
                            loading = true
                            scope.launch {
                                val result = if (isLogin) {
                                    container.auth.login(email, password)
                                } else {
                                    container.auth.register(email, password)
                                }
                                result.fold(
                                    onSuccess = { loading = false },
                                    onFailure = { fail(it) },
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                stringResource(if (isLogin) R.string.auth_login else R.string.auth_register),
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 16.sp,
                            )
                        }
                    }
                    if (isLogin) {
                        TextButton(
                            onClick = {
                                errorText = ""
                                infoText = ""
                                if (email.isBlank()) {
                                    errorText = context.getString(R.string.auth_error_fill_fields)
                                    return@TextButton
                                }
                                scope.launch {
                                    container.auth.sendPasswordReset(email).fold(
                                        onSuccess = {
                                            infoText = context.getString(R.string.auth_reset_sent)
                                        },
                                        onFailure = { fail(it) },
                                    )
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(stringResource(R.string.auth_forgot))
                        }
                    }
                    TextButton(
                        onClick = {
                            errorText = ""
                            infoText = ""
                            mode = if (isLogin) AuthMode.REGISTER else AuthMode.LOGIN
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(if (isLogin) R.string.auth_no_account else R.string.auth_have_account))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun authErrorText(e: Throwable, context: android.content.Context): String {
    val code = (e as? FirebaseAuthException)?.errorCode
    val res = when (code) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.auth_error_email_used
        "ERROR_INVALID_EMAIL" -> R.string.auth_error_bad_email
        "ERROR_WEAK_PASSWORD" -> R.string.auth_error_weak_password
        "ERROR_USER_NOT_FOUND", "ERROR_EMAIL_NOT_FOUND" -> R.string.auth_error_no_user
        "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_INVALID_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
            R.string.auth_error_invalid_credentials
        "ERROR_WRONG_PASSWORD" -> R.string.auth_error_wrong_password
        "ERROR_NETWORK_REQUEST_FAILED" -> R.string.auth_error_network
        "ERROR_TOO_MANY_REQUESTS" -> R.string.auth_error_too_many
        else -> null
    }
    return res?.let { context.getString(it) }
        ?: context.getString(R.string.auth_error_generic, e.message ?: "")
}
