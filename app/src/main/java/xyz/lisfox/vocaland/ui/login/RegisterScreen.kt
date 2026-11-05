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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.account.AccountAuthRepository

@Composable
fun RegisterScreen(
    repository: AccountAuthRepository,
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: RegisterViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RegisterViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    AuthScaffold(
        title = if (state.registered) "注册成功" else "创建账号",
        subtitle = if (state.registered) "账号已创建，请登录后使用" else "注册后可同步收藏、评分与投票",
        modifier = modifier,
    ) {
        if (state.registered) {
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
                        text = "欢迎加入 VocaLand",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onRegistered) {
                        Text("前往登录")
                    }
                }
            }
            TextButton(onClick = onBack) {
                Text("返回")
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
                RegisterForm(state = state, viewModel = viewModel)
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text("已有账号？返回登录")
        }
    }
}

@Composable
private fun RegisterForm(
    state: RegisterUiState,
    viewModel: RegisterViewModel,
) {
    val scheme = MaterialTheme.colorScheme

    OutlinedTextField(
        value = state.username,
        onValueChange = viewModel::onUsernameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("用户名") },
        placeholder = { Text("至少 3 位") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )

    OutlinedTextField(
        value = state.nickname,
        onValueChange = viewModel::onNicknameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("昵称") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )

    OutlinedTextField(
        value = state.password,
        onValueChange = viewModel::onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("密码") },
        placeholder = { Text("至少 6 位") },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = viewModel::togglePasswordVisible) {
                Icon(
                    imageVector = if (state.showPassword) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (state.showPassword) "隐藏密码" else "显示密码",
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
        label = { Text("确认密码") },
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
            imeAction = ImeAction.Next,
        ),
    )

    OutlinedTextField(
        value = state.email,
        onValueChange = viewModel::onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("邮箱") },
        placeholder = { Text("用于接收验证码") },
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
        onClick = viewModel::sendEmailCode,
        enabled = state.canSendEmail,
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
                state.emailCooldown > 0 -> "${state.emailCooldown}s 后重发"
                state.emailSent -> "重新发送验证码"
                else -> "发送邮箱验证码"
            },
        )
    }

    if (state.emailSent) {
        OutlinedTextField(
            value = state.emailCode,
            onValueChange = viewModel::onEmailCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("邮箱验证码") },
            placeholder = { Text("请输入邮件中的验证码") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
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

    Button(
        onClick = viewModel::register,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = state.canSubmit,
        shape = RoundedCornerShape(16.dp),
    ) {
        if (state.loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = scheme.onPrimary,
            )
        } else {
            Text("注册")
        }
    }
}
