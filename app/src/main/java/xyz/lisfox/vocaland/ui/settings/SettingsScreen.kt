package xyz.lisfox.vocaland.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.data.account.AccountAuthRepository
import xyz.lisfox.vocaland.data.account.AccountRepository
import xyz.lisfox.vocaland.data.account.UserPrivateDto
import xyz.lisfox.vocaland.data.notification.NotificationKind
import xyz.lisfox.vocaland.data.notification.NotificationRepository
import xyz.lisfox.vocaland.data.notification.isEnabled
import xyz.lisfox.vocaland.ui.account.roleLabel
import xyz.lisfox.vocaland.ui.components.LoadingPane
import xyz.lisfox.vocaland.ui.components.NetworkImage
import xyz.lisfox.vocaland.ui.components.ScreenMessage
import xyz.lisfox.vocaland.ui.components.SectionHeader
import xyz.lisfox.vocaland.ui.components.StatusPill
import xyz.lisfox.vocaland.ui.notifications.notificationIcon

private val AVATAR_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
private const val AVATAR_MAX_BYTES = 10 * 1024 * 1024

@Composable
fun SettingsScreen(
    accountRepository: AccountRepository,
    accountAuthRepository: AccountAuthRepository,
    notificationRepository: NotificationRepository,
    onLogin: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    accountRepository = accountRepository,
                    accountAuthRepository = accountAuthRepository,
                    notificationRepository = notificationRepository,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    var nicknameEditor by remember { mutableStateOf<String?>(null) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        if (mime !in AVATAR_TYPES) {
            avatarError = "仅支持 JPG / PNG / WebP / GIF"
            return@rememberLauncherForActivityResult
        }
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            avatarError = "读取图片失败"
            return@rememberLauncherForActivityResult
        }
        if (bytes.size > AVATAR_MAX_BYTES) {
            avatarError = "文件不能超过 10MB"
            return@rememberLauncherForActivityResult
        }
        avatarError = null
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        viewModel.uploadAvatar(bytes, "avatar.$extension", mime)
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    when {
        state.me == null && state.loading -> {
            LoadingPane(modifier = modifier, text = "正在读取账号资料")
        }

        state.me == null -> {
            ScreenMessage(
                title = "登录后管理账号",
                message = state.error ?: "账号资料需要登录后才能查看与修改。",
                modifier = modifier,
                actionLabel = if (onLogin != null) "去登录" else null,
                onAction = onLogin,
            )
        }

        else -> {
            val me = state.me!!
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ProfileSection(
                        me = me,
                        uploading = state.uploadingAvatar,
                        deleting = state.deletingAvatar,
                        savingNickname = state.savingNickname,
                        message = state.actionMessage,
                        error = avatarError ?: state.actionError,
                        onPickAvatar = { avatarPicker.launch("image/*") },
                        onDeleteAvatar = viewModel::deleteAvatar,
                        onEditNickname = { nicknameEditor = me.nickname },
                    )
                }

                item {
                    SettingsCard(title = "通知设置", description = "控制站内消息中心接收哪些类型") {
                        NotificationPreferencesSection(
                            state = state,
                            onToggle = viewModel::setPreference,
                        )
                    }
                }

                item {
                    SettingsCard(title = "邮箱管理", description = "验证、设置或更换绑定邮箱") {
                        EmailSettingsSection(
                            me = me,
                            repository = accountAuthRepository,
                            onUpdated = viewModel::reloadMe,
                        )
                    }
                }

                item {
                    SettingsCard(title = "修改密码", description = "更换账号登录密码") {
                        PasswordSection(
                            saving = state.passwordSaving,
                            message = state.passwordMessage,
                            error = state.passwordError,
                            onSubmit = viewModel::changePassword,
                        )
                    }
                }

                item {
                    SettingsCard(title = "赞助", description = "赞助状态与爱发电订单兑换") {
                        SponsorSection(
                            me = me,
                            claiming = state.sponsorClaiming,
                            message = state.sponsorMessage,
                            error = state.sponsorError,
                            onSubmit = viewModel::claimSponsor,
                        )
                    }
                }

                if (onLogout != null) {
                    item {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("退出登录", color = scheme.error)
                        }
                    }
                }
            }
        }
    }

    nicknameEditor?.let { initial ->
        var value by remember(initial) { mutableStateOf(initial) }
        AlertDialog(
            onDismissRequest = { nicknameEditor = null },
            title = { Text("修改昵称") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveNickname(value)
                        nicknameEditor = null
                    },
                    enabled = value.isNotBlank() && !state.savingNickname,
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { nicknameEditor = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun ProfileSection(
    me: UserPrivateDto,
    uploading: Boolean,
    deleting: Boolean,
    savingNickname: Boolean,
    message: String?,
    error: String?,
    onPickAvatar: () -> Unit,
    onDeleteAvatar: () -> Unit,
    onEditNickname: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !me.isBanned && !uploading, onClick = onPickAvatar),
                    contentAlignment = Alignment.Center,
                ) {
                    NetworkImage(
                        url = me.avatar,
                        contentDescription = me.nickname,
                        modifier = Modifier.size(74.dp),
                    )
                    if (uploading) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .background(scheme.scrim.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = scheme.onPrimary,
                            )
                        }
                    } else if (!me.isBanned) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .background(scheme.scrim.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "更换头像",
                                tint = scheme.onPrimary,
                            )
                        }
                    }
                }

                Spacer(Modifier.size(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = me.nickname.ifBlank { me.username },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = onEditNickname,
                            enabled = !me.isBanned && !savingNickname,
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "修改昵称")
                        }
                    }
                    Text(
                        text = "@${me.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(text = roleLabel(me.role))
                        StatusPill(text = "Lv.${me.level}")
                    }
                }
            }

            HorizontalDivider(color = scheme.outlineVariant)

            InfoRow(label = "邮箱", value = me.email ?: "未设置")
            InfoRow(
                label = "邮箱状态",
                value = when {
                    me.email == null -> "未设置"
                    me.emailVerified -> "已验证"
                    else -> "未验证"
                },
            )
            InfoRow(label = "经验值", value = me.totalExp.toString())
            InfoRow(
                label = "赞助",
                value = if (me.sponsorActive) {
                    "有效，剩余 ${me.sponsorRemainingDays} 天"
                } else {
                    "非赞助用户"
                },
            )

            if (me.isBanned) {
                Text(
                    text = "账号已被封禁，无法修改昵称、头像和邮箱。",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error,
                )
            }

            if (me.avatar != null && !me.isBanned) {
                TextButton(
                    onClick = onDeleteAvatar,
                    enabled = !deleting,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("删除头像，恢复默认")
                }
            }

            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.primary,
                )
            }
            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun NotificationPreferencesSection(
    state: SettingsUiState,
    onToggle: (NotificationKind, Boolean) -> Unit,
) {
    val prefs = state.preferences
    when {
        prefs == null && state.preferencesLoading -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        prefs == null -> {
            Text(
                text = state.preferencesError ?: "通知偏好读取失败",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        else -> {
            Column {
                NotificationKind.ordered.forEachIndexed { index, kind ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = notificationIcon(kind),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kind.prefLabel, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = kind.prefDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = prefs.isEnabled(kind),
                            onCheckedChange = { onToggle(kind, it) },
                            enabled = state.preferencesSaving == null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordSection(
    saving: Boolean,
    message: String?,
    error: String?,
    onSubmit: (String, String) -> Unit,
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            oldPassword = ""
            newPassword = ""
            confirmPassword = ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = oldPassword,
            onValueChange = { oldPassword = it; localError = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("原密码") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it; localError = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("新密码") },
            placeholder = { Text("至少 6 位") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; localError = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("确认新密码") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        )

        val validationError = localError ?: when {
            newPassword.isNotEmpty() && newPassword.length < 6 -> "新密码至少 6 位"
            confirmPassword.isNotEmpty() && newPassword != confirmPassword -> "两次输入的密码不一致"
            else -> null
        }

        Button(
            onClick = {
                when {
                    oldPassword.isBlank() -> localError = "请输入原密码"
                    newPassword.length < 6 -> localError = "新密码至少 6 位"
                    newPassword != confirmPassword -> localError = "两次输入的密码不一致"
                    else -> onSubmit(oldPassword, newPassword)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("修改密码")
            }
        }

        if (!validationError.isNullOrBlank()) {
            Text(
                text = validationError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SponsorSection(
    me: UserPrivateDto,
    claiming: Boolean,
    message: String?,
    error: String?,
    onSubmit: (String) -> Unit,
) {
    var orderId by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) orderId = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (me.sponsorActive) {
                "赞助有效，剩余 ${me.sponsorRemainingDays} 天"
            } else {
                "非赞助用户"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (me.sponsorExpiresAt != null) {
            Text(
                text = "到期时间：${me.sponsorExpiresAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = orderId,
                onValueChange = { orderId = it },
                modifier = Modifier.weight(1f),
                label = { Text("爱发电订单号") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Button(
                onClick = { onSubmit(orderId) },
                enabled = !claiming && orderId.isNotBlank() && !me.isBanned,
            ) {
                if (claiming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("兑换")
                }
            }
        }
        if (me.isBanned) {
            Text(
                text = "账号已被封禁，无法兑换",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column {
        SectionHeader(title = title, subtitle = description)
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}
