package xyz.lisfox.vocaland.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.update.AppUpdate
import xyz.lisfox.vocaland.data.update.UpdateRepository

data class UpdateUiState(
    val checking: Boolean = false,
    val update: AppUpdate? = null,
    val visible: Boolean = false,
    val message: String? = null,
)

class UpdateViewModel(
    private val repository: UpdateRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var notifyOnComplete = false

    init {
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean) {
        if (_state.value.checking) {
            if (!silent) {
                notifyOnComplete = true
                _state.update { it.copy(message = "正在检查更新…") }
            }
            return
        }

        notifyOnComplete = !silent
        _state.update {
            it.copy(
                checking = true,
                message = if (silent) it.message else "正在检查更新…",
            )
        }
        viewModelScope.launch {
            try {
                val update = repository.checkForUpdate()
                val notify = notifyOnComplete
                notifyOnComplete = false
                _state.update {
                    it.copy(
                        checking = false,
                        update = update,
                        visible = update != null,
                        message = if (update == null && notify) "当前已是最新版本" else null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val notify = notifyOnComplete
                notifyOnComplete = false
                _state.update {
                    it.copy(
                        checking = false,
                        message = if (notify) updateErrorMessage(error) else null,
                    )
                }
            }
        }
    }

    fun dismiss() {
        _state.update { it.copy(visible = false) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun updateErrorMessage(error: Exception): String =
        if (error is IOException) "网络连接失败，请检查网络后重试" else "检查更新失败，请稍后重试"
}
