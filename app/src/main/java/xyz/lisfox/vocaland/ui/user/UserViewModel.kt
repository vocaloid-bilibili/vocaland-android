package xyz.lisfox.vocaland.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.PublicUserDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.UserLitStatsDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.UserRepository

data class UserUiState(
    val user: PublicUserDto? = null,
    val litStats: UserLitStatsDto? = null,
    val ratings: List<RatingDto> = emptyList(),
    val ratingsTotal: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
)

class UserViewModel(
    private val repository: UserRepository,
    private val userId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(UserUiState())
    val state: StateFlow<UserUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(loading = it.user == null, error = null) }
        loadJob = viewModelScope.launch {
            val result = repository.load(userId)
            if (!isActive) return@launch
            result
                .onSuccess { content ->
                    _state.update {
                        it.copy(
                            user = content.user,
                            litStats = content.litStats,
                            ratings = content.ratings,
                            ratingsTotal = content.ratingsTotal,
                            loading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = error.toUserMessage(),
                        )
                    }
                }
        }
    }
}
