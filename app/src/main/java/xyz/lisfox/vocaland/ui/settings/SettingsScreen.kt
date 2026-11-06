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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import xyz.lisfox.vocaland.R
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
            avatarError = context.getString(R.string.settings_avatar_unsupported_type)
            return@rememberLauncherForActivityResult
        }
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            avatarError = context.getString(R.string.settings_avatar_read_failed)
            return@rememberLauncherForActivityResult
        }
        if (bytes.size > AVATAR_MAX_BYTES) {
            avatarError = context.getString(
                R.string.settings_avatar_too_large_format,
                AVATAR_MAX_BYTES / (1024 * 1024),
            )
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
            LoadingPane(modifier = modifier, text = stringResource(R.string.settings_profile_loading))
        }

        state.me == null -> {
            ScreenMessage(
                title = stringResource(R.string.settings_login_required_title),
                message = state.error ?: stringResource(R.string.settings_login_required_message),
                modifier = modifier,
                actionLabel = if (onLogin != null) stringResource(R.string.action_login) else null,
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
                    SettingsCard(
                        title = stringResource(R.string.settings_notifications_title),
                        description = stringResource(R.string.settings_notifications_desc),
                    ) {
                        NotificationPreferencesSection(
                            state = state,
                            onToggle = viewModel::setPreference,
                        )
                    }
                }

                item {
                    SettingsCard(
                        title = stringResource(R.string.settings_email_title),
                        description = stringResource(R.string.settings_email_desc),
                    ) {
                        EmailSettingsSection(
                            me = me,
                            repository = accountAuthRepository,
                            onUpdated = viewModel::reloadMe,
                        )
                    }
                }

                item {
                    SettingsCard(
                        title = stringResource(R.string.settings_password_title),
                        description = stringResource(R.string.settings_password_desc),
                    ) {
                        PasswordSection(
                            saving = state.passwordSaving,
                            message = state.passwordMessage,
                            error = state.passwordError,
                            onSubmit = viewModel::changePassword,
                        )
                    }
                }

                item {
                    SettingsCard(
                        title = stringResource(R.string.wallet_sponsor_title),
                        description = stringResource(R.string.settings_sponsor_desc),
                    ) {
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
                            Text(stringResource(R.string.profile_logout), color = scheme.error)
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
            title = { Text(stringResource(R.string.settings_nickname_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.register_nickname)) },
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
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { nicknameEditor = null }) {
                    Text(stringResource(R.string.action_cancel))
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
                                contentDescription = stringResource(R.string.settings_change_avatar),
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
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.settings_nickname_dialog_title))
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

            InfoRow(label = stringResource(R.string.login_email), value = me.email ?: stringResource(R.string.settings_value_not_set))
            InfoRow(
                label = stringResource(R.string.settings_email_status),
                value = when {
                    me.email == null -> stringResource(R.string.settings_value_not_set)
                    me.emailVerified -> stringResource(R.string.settings_email_verified)
                    else -> stringResource(R.string.settings_email_unverified)
                },
            )
            InfoRow(label = stringResource(R.string.profile_exp), value = me.totalExp.toString())
            InfoRow(
                label = stringResource(R.string.wallet_sponsor_title),
                value = if (me.sponsorActive) {
                    stringResource(R.string.settings_sponsor_remaining_format, me.sponsorRemainingDays)
                } else {
                    stringResource(R.string.settings_sponsor_inactive)
                },
            )

            if (me.isBanned) {
                Text(
                    text = stringResource(R.string.settings_banned_profile),
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
                    Text(stringResource(R.string.settings_delete_avatar))
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
                text = state.preferencesError ?: stringResource(R.string.settings_notifications_load_failed),
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
    val oldPasswordRequired = stringResource(R.string.settings_old_password_required)
    val passwordTooShort = stringResource(R.string.settings_password_too_short)
    val passwordMismatch = stringResource(R.string.settings_password_mismatch)

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
            label = { Text(stringResource(R.string.settings_old_password)) },
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
            label = { Text(stringResource(R.string.forgot_new_password)) },
            placeholder = { Text(stringResource(R.string.register_password_hint)) },
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
            label = { Text(stringResource(R.string.forgot_confirm_new_password)) },
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
            newPassword.isNotEmpty() && newPassword.length < 6 -> passwordTooShort
            confirmPassword.isNotEmpty() && newPassword != confirmPassword -> passwordMismatch
            else -> null
        }

        Button(
            onClick = {
                when {
                    oldPassword.isBlank() -> localError = oldPasswordRequired
                    newPassword.length < 6 -> localError = passwordTooShort
                    newPassword != confirmPassword -> localError = passwordMismatch
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
                Text(stringResource(R.string.settings_password_title))
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
                stringResource(R.string.settings_sponsor_active_format, me.sponsorRemainingDays)
            } else {
                stringResource(R.string.settings_sponsor_inactive)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (me.sponsorExpiresAt != null) {
            Text(
                text = stringResource(R.string.settings_sponsor_expires_format, me.sponsorExpiresAt),
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
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                label = { Text(stringResource(R.string.wallet_sponsor_order_label)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Button(
                onClick = { onSubmit(orderId) },
                enabled = !claiming && orderId.isNotBlank() && !me.isBanned,
                modifier = Modifier.height(56.dp),
            ) {
                if (claiming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.wallet_sponsor_claim))
                }
            }
        }
        if (me.isBanned) {
            Text(
                text = stringResource(R.string.settings_banned_sponsor),
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
