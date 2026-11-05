package xyz.lisfox.vocaland.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.account.AccountAuthRepository
import xyz.lisfox.vocaland.data.model.CaptchaDto
import xyz.lisfox.vocaland.data.remote.toUserMessage

enum class EmailAction {
    Verify,
    Change,
}

enum class EmailStep {
    Input,
    Code,
}

data class EmailSettingsUiState(
    val action: EmailAction? = null,
    val step: EmailStep = EmailStep.Input,
    val email: String = "",
    val captcha: CaptchaDto? = null,
    val captchaAnswer: String = "",
    val captchaLoading: Boolean = false,
    val code: String = "",
    val sending: Boolean = false,
    val confirming: Boolean = false,
    val cooldown: Int = 0,
    val message: String? = null,
    val error: String? = null,
) {
    val canSendCode: Boolean
        get() = email.contains("@") &&
            captcha != null &&
            captchaAnswer.isNotBlank() &&
            !sending &&
            cooldown <= 0

    val canConfirm: Boolean
        get() = code.isNotBlank() && !confirming
}

sealed interface EmailSettingsEvent {
    data class Updated(val email: String) : EmailSettingsEvent
}

class EmailSettingsViewModel(
    private val repository: AccountAuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(EmailSettingsUiState())
    val state: StateFlow<EmailSettingsUiState> = _state.asStateFlow()

    private val _events = Channel<EmailSettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var cooldownJob: Job? = null

    fun start(action: EmailAction, currentEmail: String?) {
        _state.update {
            it.copy(
                action = action,
                step = EmailStep.Input,
                email = if (action == EmailAction.Verify) currentEmail.orEmpty() else "",
                code = "",
                message = null,
                error = null,
                cooldown = 0,
            )
        }
        loadCaptcha()
    }

    fun cancel() {
        cooldownJob?.cancel()
        _state.update {
            it.copy(
                action = null,
                step = EmailStep.Input,
                code = "",
                cooldown = 0,
                error = null,
            )
        }
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

    fun refreshCaptcha() {
        loadCaptcha()
    }

    fun sendCode() {
        val current = _state.value
        val action = current.action ?: return
        if (!current.canSendCode) return
        val captcha = current.captcha ?: return

        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            repository.sendEmailCode(
                email = current.email,
                purpose = if (action == EmailAction.Verify) "verify" else "change",
                codeId = captcha.codeId,
                codeAnswer = current.captchaAnswer,
            ).onSuccess {
                _state.update {
                    it.copy(
                        sending = false,
                        step = EmailStep.Code,
                        message = "验证码已发送，请查收邮件",
                        error = null,
                    )
                }
                startCooldown()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        sending = false,
                        error = error.toUserMessage(),
                    )
                }
                loadCaptcha()
            }
        }
    }

    fun confirm() {
        val current = _state.value
        val action = current.action ?: return
        if (!current.canConfirm) return

        _state.update { it.copy(confirming = true, error = null, message = null) }
        viewModelScope.launch {
            val result = if (action == EmailAction.Verify) {
                repository.verifyEmail(current.email, current.code)
            } else {
                repository.changeEmail(current.email, current.code)
            }
            result.onSuccess {
                _state.update {
                    it.copy(
                        confirming = false,
                        action = null,
                        step = EmailStep.Input,
                        code = "",
                        message = "邮箱已更新",
                        error = null,
                    )
                }
                _events.send(EmailSettingsEvent.Updated(current.email.trim()))
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        confirming = false,
                        error = error.toUserMessage(),
                    )
                }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _state.update { it.copy(cooldown = 60) }
            while (_state.value.cooldown > 0) {
                delay(1000)
                _state.update { it.copy(cooldown = (it.cooldown - 1).coerceAtLeast(0)) }
            }
        }
    }

    private fun loadCaptcha() {
        if (_state.value.captchaLoading) return
        _state.update { it.copy(captchaLoading = true) }
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
