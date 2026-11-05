package xyz.lisfox.vocaland.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.account.AccountAuthRepository
import xyz.lisfox.vocaland.data.account.UserPrivateDto
import xyz.lisfox.vocaland.ui.login.CaptchaField

@Composable
fun EmailSettingsSection(
    me: UserPrivateDto?,
    repository: AccountAuthRepository,
    onUpdated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: EmailSettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { EmailSettingsViewModel(repository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is EmailSettingsEvent.Updated) {
                onUpdated()
            }
        }
    }

    if (me == null) {
        Text(
            text = "正在加载账号资料",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = me.email ?: "尚未设置邮箱",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    me.email == null -> "未设置"
                    me.emailVerified -> "已验证"
                    else -> "未验证"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (me.emailVerified) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }

        if (state.action == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (me.email != null && !me.emailVerified) {
                    OutlinedButton(
                        onClick = { viewModel.start(EmailAction.Verify, me.email) },
                        enabled = !me.isBanned,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("验证邮箱")
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.start(EmailAction.Change, me.email) },
                    enabled = !me.isBanned,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (me.email == null) "设置邮箱" else "更换邮箱")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (state.action) {
                        EmailAction.Verify -> "验证邮箱"
                        EmailAction.Change -> if (me.email == null) "设置邮箱" else "更换邮箱"
                        null -> ""
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::cancel) {
                    Text("取消")
                }
            }

            if (state.step == EmailStep.Input) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("新邮箱") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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
                    if (state.sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("发送验证码")
                }
            } else {
                Text(
                    text = "验证码已发送到 ${state.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("邮箱验证码") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = viewModel::confirm,
                        enabled = state.canConfirm,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.confirming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("确认")
                    }
                    TextButton(
                        onClick = viewModel::sendCode,
                        enabled = state.canSendCode,
                    ) {
                        Text(if (state.cooldown > 0) "${state.cooldown}s" else "重新发送")
                    }
                }
            }
        }

        if (!state.message.isNullOrBlank()) {
            Text(
                text = state.message!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (!state.error.isNullOrBlank()) {
            Text(
                text = state.error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
