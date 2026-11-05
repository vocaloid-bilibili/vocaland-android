package xyz.lisfox.vocaland.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.account.AccountAuthRepository
import xyz.lisfox.vocaland.data.model.CaptchaDto
import xyz.lisfox.vocaland.data.remote.toUserMessage

data class RegisterUiState(
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailCode: String = "",
    val showPassword: Boolean = false,
    val captcha: CaptchaDto? = null,
    val captchaAnswer: String = "",
    val captchaLoading: Boolean = false,
    val emailSending: Boolean = false,
    val emailSent: Boolean = false,
    val emailCooldown: Int = 0,
    val loading: Boolean = false,
    val registered: Boolean = false,
    val error: String? = null,
) {
    val canSendEmail: Boolean
        get() = email.contains("@") &&
            captcha != null &&
            captchaAnswer.isNotBlank() &&
            !emailSending &&
            emailCooldown <= 0

    val canSubmit: Boolean
        get() = username.trim().length >= 3 &&
            nickname.isNotBlank() &&
            email.contains("@") &&
            password.length >= 6 &&
            confirmPassword.isNotEmpty() &&
            emailCode.isNotBlank() &&
            emailSent &&
            !loading
}

class RegisterViewModel(
    private val repository: AccountAuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        loadCaptcha(clearError = true)
    }

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, error = null) }
    }

    fun onNicknameChange(value: String) {
        _state.update { it.copy(nickname = value, error = null) }
    }

    fun onEmailChange(value: String) {
        _state.update {
            it.copy(
                email = value,
                emailSent = false,
                emailCode = "",
                error = null,
            )
        }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, error = null) }
    }

    fun onEmailCodeChange(value: String) {
        _state.update { it.copy(emailCode = value, error = null) }
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

    fun sendEmailCode() {
        val current = _state.value
        if (!current.canSendEmail) return
        val captcha = current.captcha ?: return

        _state.update { it.copy(emailSending = true, error = null) }
        viewModelScope.launch {
            repository.sendEmailCode(
                email = current.email,
                purpose = "register",
                codeId = captcha.codeId,
                codeAnswer = current.captchaAnswer,
            ).onSuccess {
                _state.update {
                    it.copy(
                        emailSending = false,
                        emailSent = true,
                        error = null,
                    )
                }
                startCooldown()
                loadCaptcha(clearError = false)
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        emailSending = false,
                        error = error.toUserMessage(),
                    )
                }
                loadCaptcha(clearError = false)
            }
        }
    }

    fun register() {
        val current = _state.value
        if (!current.canSubmit) return

        if (current.password != current.confirmPassword) {
            _state.update { it.copy(error = "两次输入的密码不一致") }
            return
        }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.register(
                username = current.username,
                nickname = current.nickname,
                email = current.email,
                password = current.password,
                emailCode = current.emailCode,
            ).onSuccess {
                _state.update { it.copy(loading = false, registered = true) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.toUserMessage(),
                    )
                }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _state.update { it.copy(emailCooldown = 60) }
            while (_state.value.emailCooldown > 0) {
                delay(1000)
                _state.update { it.copy(emailCooldown = (it.emailCooldown - 1).coerceAtLeast(0)) }
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
