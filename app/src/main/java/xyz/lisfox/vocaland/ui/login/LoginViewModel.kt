package xyz.lisfox.vocaland.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.CaptchaDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.AuthRepository

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val captcha: CaptchaDto? = null,
    val captchaAnswer: String = "",
    val captchaLoading: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() &&
            password.isNotEmpty() &&
            captcha != null &&
            captchaAnswer.isNotBlank() &&
            !loading
}

sealed interface LoginEvent {
    data object Success : LoginEvent
}

class LoginViewModel(
    private val repository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadCaptcha(clearError = true)
    }

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onCaptchaAnswerChange(value: String) {
        _state.update { it.copy(captchaAnswer = value, error = null) }
    }

    fun togglePasswordVisible() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }

    fun refreshCaptcha() {
        loadCaptcha(clearError = true)
    }

    fun login() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            repository.login(
                username = current.username,
                password = current.password,
                codeId = current.captcha?.codeId ?: 0,
                codeAnswer = current.captchaAnswer,
            ).onSuccess {
                _state.update { it.copy(loading = false) }
                _events.send(LoginEvent.Success)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.toUserMessage(),
                    )
                }
                loadCaptcha(clearError = false)
            }
        }
    }

    private fun loadCaptcha(clearError: Boolean) {
        if (_state.value.captchaLoading) return

        _state.update {
            it.copy(
                captchaLoading = true,
                error = if (clearError) null else it.error,
            )
        }

        viewModelScope.launch {
            repository.getCaptcha()
                .onSuccess { captcha ->
                    _state.update {
                        it.copy(
                            captcha = captcha,
                            captchaAnswer = "",
                            captchaLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            captchaLoading = false,
                            error = it.error ?: error.toUserMessage(),
                        )
                    }
                }
        }
    }
}
