package xyz.lisfox.vocaland.ui.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.VotingHubData
import xyz.lisfox.vocaland.data.repository.VotingRepository

data class VotingHubUiState(
    val loading: Boolean = true,
    val data: VotingHubData? = null,
    val error: String? = null,
)

class VotingHubViewModel(
    private val repository: VotingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(VotingHubUiState())
    val state: StateFlow<VotingHubUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.loadHub()
                .onSuccess { data ->
                    _state.update {
                        it.copy(loading = false, data = data, error = null)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, error = error.toUserMessage())
                    }
                }
        }
    }
}
