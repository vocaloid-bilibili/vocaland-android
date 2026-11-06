package xyz.lisfox.vocaland.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.R
import xyz.lisfox.vocaland.data.account.AccountAuthRepository

@Composable
fun ForgotPasswordScreen(
    repository: AccountAuthRepository,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ForgotPasswordViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ForgotPasswordViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    AuthScaffold(
        title = if (state.success) stringResource(R.string.forgot_success_title) else stringResource(R.string.page_forgot_password),
        subtitle = if (state.success) stringResource(R.string.forgot_success_subtitle) else stringResource(R.string.forgot_subtitle),
        modifier = modifier,
    ) {
        if (state.success) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = scheme.surfaceContainerLow.copy(alpha = 0.96f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.forgot_success_message),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onFinished) {
                        Text(stringResource(R.string.register_go_login))
                    }
                }
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.action_back))
            }
            return@AuthScaffold
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = scheme.surfaceContainerLow.copy(alpha = 0.96f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.forgot_email)) },
                    leadingIcon = { Icon(Icons.Filled.Mail, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                CaptchaField(
                    answer = state.captchaAnswer,
                    onAnswerChange = viewModel::onCaptchaAnswerChange,
                    captcha = state.captcha,
                    loading = state.captchaLoading,
                    onRefresh = viewModel::refreshCaptcha,
                )

                OutlinedButton(
                    onClick = viewModel::sendCode,
                    enabled = state.canSendCode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.emailSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Mail,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            state.emailCooldown > 0 -> stringResource(R.string.register_resend_countdown, state.emailCooldown)
                            state.step == ResetStep.Reset -> stringResource(R.string.register_resend)
                            else -> stringResource(R.string.forgot_send_code)
                        },
                    )
                }

                if (state.step == ResetStep.Reset) {
                    OutlinedTextField(
                        value = state.code,
                        onValueChange = viewModel::onCodeChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.register_email_code)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = viewModel::onNewPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.forgot_new_password)) },
                        placeholder = { Text(stringResource(R.string.register_password_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePasswordVisible) {
                                Icon(
                                    imageVector = if (state.showPassword) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (state.showPassword) stringResource(R.string.login_hide_password) else stringResource(R.string.login_show_password),
                                )
                            }
                        },
                        visualTransformation = if (state.showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.forgot_confirm_new_password)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = if (state.showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }

                if (!state.error.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.errorContainer,
                        contentColor = scheme.onErrorContainer,
                    ) {
                        Text(
                            text = state.error!!,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (state.step == ResetStep.Reset) {
                    Button(
                        onClick = viewModel::resetPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = state.canReset,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = scheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.forgot_reset))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.forgot_back_login))
        }
    }
}
