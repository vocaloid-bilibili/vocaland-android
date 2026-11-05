package xyz.lisfox.vocaland.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.account.AccountAuthRepository
import xyz.lisfox.vocaland.data.account.AccountRepository
import xyz.lisfox.vocaland.data.account.UserPrivateDto
import xyz.lisfox.vocaland.data.notification.NotificationKind
import xyz.lisfox.vocaland.data.notification.NotificationPreferencesDto
import xyz.lisfox.vocaland.data.notification.NotificationPreferencesPatch
import xyz.lisfox.vocaland.data.notification.NotificationRepository
import xyz.lisfox.vocaland.data.notification.forKind
import xyz.lisfox.vocaland.data.notification.with
import xyz.lisfox.vocaland.data.remote.toUserMessage

data class SettingsUiState(
    val me: UserPrivateDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val actionError: String? = null,
    val savingNickname: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val deletingAvatar: Boolean = false,
    val passwordSaving: Boolean = false,
    val passwordMessage: String? = null,
    val passwordError: String? = null,
    val sponsorClaiming: Boolean = false,
    val sponsorMessage: String? = null,
    val sponsorError: String? = null,
    val preferences: NotificationPreferencesDto? = null,
    val preferencesLoading: Boolean = false,
    val preferencesError: String? = null,
    val preferencesSaving: NotificationKind? = null,
)

class SettingsViewModel(
    private val accountRepository: AccountRepository,
    private val accountAuthRepository: AccountAuthRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null, preferencesLoading = true) }
        viewModelScope.launch {
            val meResult = accountRepository.loadMe()
            val prefsResult = notificationRepository.loadPreferences()
            _state.update {
                it.copy(
                    me = meResult.getOrNull() ?: it.me,
                    loading = false,
                    error = meResult.exceptionOrNull()?.toUserMessage(),
                    preferences = prefsResult.getOrNull() ?: it.preferences,
                    preferencesLoading = false,
                    preferencesError = prefsResult.exceptionOrNull()?.toUserMessage(),
                )
            }
        }
    }

    fun reloadMe() {
        viewModelScope.launch {
            accountRepository.loadMe()
                .onSuccess { me -> _state.update { it.copy(me = me, error = null) } }
                .onFailure { error -> _state.update { it.copy(error = error.toUserMessage()) } }
        }
    }

    fun saveNickname(nickname: String) {
        if (_state.value.savingNickname) return
        _state.update {
            it.copy(
                savingNickname = true,
                actionMessage = null,
                actionError = null,
            )
        }
        viewModelScope.launch {
            accountRepository.updateNickname(nickname)
                .onSuccess {
                    _state.update {
                        it.copy(
                            savingNickname = false,
                            actionMessage = "昵称已保存",
                            me = it.me?.copy(nickname = nickname.trim()),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            savingNickname = false,
                            actionError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun uploadAvatar(bytes: ByteArray, fileName: String, mimeType: String) {
        if (_state.value.uploadingAvatar) return
        _state.update {
            it.copy(
                uploadingAvatar = true,
                actionMessage = null,
                actionError = null,
            )
        }
        viewModelScope.launch {
            accountRepository.uploadAvatar(bytes, fileName, mimeType)
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            uploadingAvatar = false,
                            actionMessage = "头像已更新",
                            me = it.me?.copy(avatar = response.avatar),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            uploadingAvatar = false,
                            actionError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun deleteAvatar() {
        if (_state.value.deletingAvatar) return
        _state.update {
            it.copy(
                deletingAvatar = true,
                actionMessage = null,
                actionError = null,
            )
        }
        viewModelScope.launch {
            accountRepository.deleteAvatar()
                .onSuccess {
                    _state.update {
                        it.copy(
                            deletingAvatar = false,
                            actionMessage = "头像已删除",
                            me = it.me?.copy(avatar = null),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            deletingAvatar = false,
                            actionError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        if (_state.value.passwordSaving) return
        _state.update {
            it.copy(
                passwordSaving = true,
                passwordMessage = null,
                passwordError = null,
            )
        }
        viewModelScope.launch {
            accountAuthRepository.changePassword(oldPassword, newPassword)
                .onSuccess {
                    _state.update {
                        it.copy(
                            passwordSaving = false,
                            passwordMessage = "密码修改成功",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            passwordSaving = false,
                            passwordError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun claimSponsor(orderId: String) {
        if (_state.value.sponsorClaiming) return
        _state.update {
            it.copy(
                sponsorClaiming = true,
                sponsorMessage = null,
                sponsorError = null,
            )
        }
        viewModelScope.launch {
            accountRepository.claimSponsorOrder(orderId)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            sponsorClaiming = false,
                            sponsorMessage = "兑换成功，增加 ${result.durationDays} 天赞助时长",
                        )
                    }
                    reloadMe()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            sponsorClaiming = false,
                            sponsorError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun setPreference(kind: NotificationKind, enabled: Boolean) {
        val current = _state.value
        val prefs = current.preferences ?: return
        if (current.preferencesSaving != null) return

        _state.update {
            it.copy(
                preferences = prefs.with(kind, enabled),
                preferencesSaving = kind,
                preferencesError = null,
            )
        }
        viewModelScope.launch {
            notificationRepository.updatePreferences(
                NotificationPreferencesPatch().forKind(kind, enabled),
            ).onSuccess { saved ->
                _state.update {
                    it.copy(
                        preferences = saved,
                        preferencesSaving = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        preferences = prefs,
                        preferencesSaving = null,
                        preferencesError = error.toUserMessage(),
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        _state.update { it.copy(actionMessage = null, actionError = null) }
    }

    fun clearSponsorMessage() {
        _state.update { it.copy(sponsorMessage = null, sponsorError = null) }
    }
}
