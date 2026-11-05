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

enum class ResetStep {
    SendCode,
    Reset,
}

data class ForgotPasswordUiState(
    val email: String = "",
    val captcha: CaptchaDto? = null,
    val captchaAnswer: String = "",
    val captchaLoading: Boolean = false,
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val step: ResetStep = ResetStep.SendCode,
    val emailSending: Boolean = false,
    val emailCooldown: Int = 0,
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
) {
    val canSendCode: Boolean
        get() = email.contains("@") &&
            captcha != null &&
            captchaAnswer.isNotBlank() &&
            !emailSending &&
            emailCooldown <= 0

    val canReset: Boolean
        get() = code.isNotBlank() &&
            newPassword.length >= 6 &&
            confirmPassword.isNotEmpty() &&
            !loading
}

class ForgotPasswordViewModel(
    private val repository: AccountAuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        loadCaptcha(clearError = true)
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun onCaptchaAnswerChange(value: String) {
        _state.update { it.copy(captchaAnswer = value, error = null) }
    }

    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value, error = null) }
    }

    fun onNewPasswordChange(value: String) {
        _state.update { it.copy(newPassword = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, error = null) }
    }

    fun togglePasswordVisible() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }

    fun refreshCaptcha() {
        loadCaptcha(clearError = true)
    }

    fun sendCode() {
        val current = _state.value
        if (!current.canSendCode) return
        val captcha = current.captcha ?: return

        _state.update { it.copy(emailSending = true, error = null) }
        viewModelScope.launch {
            repository.sendEmailCode(
                email = current.email,
                purpose = "reset",
                codeId = captcha.codeId,
                codeAnswer = current.captchaAnswer,
            ).onSuccess {
                _state.update {
                    it.copy(
                        emailSending = false,
                        step = ResetStep.Reset,
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

    fun resetPassword() {
        val current = _state.value
        if (!current.canReset) return
        if (current.newPassword != current.confirmPassword) {
            _state.update { it.copy(error = "两次输入的新密码不一致") }
            return
        }

        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.resetPasswordByEmail(
                email = current.email,
                code = current.code,
                newPassword = current.newPassword,
            ).onSuccess {
                _state.update { it.copy(loading = false, success = true) }
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
