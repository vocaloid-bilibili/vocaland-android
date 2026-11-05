package xyz.lisfox.vocaland.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.LevelInfoDto
import xyz.lisfox.vocaland.data.model.PublicProfileResponseDto
import xyz.lisfox.vocaland.data.model.PublicUserDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.UserLitStatsDto
import xyz.lisfox.vocaland.data.model.WalletDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.UserRepository
import xyz.lisfox.vocaland.data.session.AuthSession

data class UserUiState(
    val user: PublicUserDto? = null,
    val litStats: UserLitStatsDto? = null,
    val profile: PublicProfileResponseDto? = null,
    val ratings: List<RatingDto> = emptyList(),
    val ratingsPage: Int = 1,
    val ratingsTotalPages: Int = 1,
    val ratingsTotal: Int = 0,
    val ratingsLoadingMore: Boolean = false,
    val ratingsLoadMoreError: String? = null,
    val isLoggedIn: Boolean = false,
    val isOwner: Boolean = false,
    val wallet: WalletDto? = null,
    val level: LevelInfoDto? = null,
    val ownerLoading: Boolean = false,
    val ownerError: String? = null,
    val ownerNotice: String? = null,
    val checkinBusy: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)

class UserViewModel(
    private val repository: UserRepository,
    private val userId: Int,
    private val session: AuthSession? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(UserUiState())
    val state: StateFlow<UserUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var profileJob: Job? = null
    private var ownerJob: Job? = null
    private var ratingsJob: Job? = null

    init {
        observeSession()
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
                            ratingsPage = 1,
                            ratingsTotalPages = content.ratingsTotalPages,
                            ratingsTotal = content.ratingsTotal,
                            ratingsLoadingMore = false,
                            ratingsLoadMoreError = null,
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

    fun loadMoreRatings() {
        val current = _state.value
        if (current.ratingsLoadingMore || current.ratingsPage >= current.ratingsTotalPages) return

        _state.update { it.copy(ratingsLoadingMore = true, ratingsLoadMoreError = null) }
        ratingsJob?.cancel()
        ratingsJob = viewModelScope.launch {
            val result = repository.loadRatings(userId, current.ratingsPage + 1)
            if (!isActive) return@launch
            result
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            ratings = it.ratings + page.items,
                            ratingsPage = page.page,
                            ratingsTotalPages = page.totalPages,
                            ratingsTotal = page.total,
                            ratingsLoadingMore = false,
                            ratingsLoadMoreError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            ratingsLoadingMore = false,
                            ratingsLoadMoreError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun checkin() {
        if (_state.value.checkinBusy || !_state.value.isOwner) return

        _state.update {
            it.copy(checkinBusy = true, ownerError = null, ownerNotice = null)
        }
        viewModelScope.launch {
            repository.checkin()
                .onSuccess { result ->
                    val message = when {
                        result.already -> "今天已经签过啦"
                        result.ticketGranted -> "签到成功，获得补签卡 ×1"
                        else -> "签到成功 +${result.expGained} 经验"
                    }
                    _state.update {
                        it.copy(
                            checkinBusy = false,
                            ownerNotice = message,
                            ownerError = null,
                        )
                    }
                    loadOwnerData()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            checkinBusy = false,
                            ownerError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun observeSession() {
        val currentSession = session
        if (currentSession == null) return

        viewModelScope.launch {
            currentSession.state
                .map { it.user?.id to it.isLoggedIn }
                .distinctUntilChanged()
                .collect { (viewerId, loggedIn) ->
                    val owner = loggedIn && viewerId == userId
                    val wasLoggedIn = _state.value.isLoggedIn
                    _state.update {
                        it.copy(
                            isLoggedIn = loggedIn,
                            isOwner = owner,
                            profile = if (loggedIn) it.profile else null,
                            wallet = if (owner) it.wallet else null,
                            level = if (owner) it.level else null,
                            ownerError = if (owner) it.ownerError else null,
                            ownerNotice = if (owner) it.ownerNotice else null,
                        )
                    }

                    if (loggedIn && (!wasLoggedIn || _state.value.profile == null)) {
                        loadProfile()
                    }
                    if (owner) {
                        loadOwnerData()
                    }
                }
        }
    }

    private fun loadProfile() {
        if (!_state.value.isLoggedIn) return
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            val result = repository.loadProfile(userId)
            if (!isActive) return@launch
            result.onSuccess { profile ->
                if (profile != null) {
                    _state.update { it.copy(profile = profile) }
                }
            }
        }
    }

    private fun loadOwnerData() {
        if (!_state.value.isOwner || repository.community == null) return
        ownerJob?.cancel()
        _state.update { it.copy(ownerLoading = true, ownerError = null) }

        ownerJob = viewModelScope.launch {
            val data = coroutineScope {
                val wallet = async { repository.loadWallet() }
                val level = async { repository.loadLevel() }
                wallet.await() to level.await()
            }
            if (!isActive) return@launch

            val (walletResult, levelResult) = data
            val wallet = walletResult.getOrNull()
            val level = levelResult.getOrNull()
            val error = if (wallet == null && level == null) {
                (levelResult.exceptionOrNull() ?: walletResult.exceptionOrNull())
                    ?.toUserMessage()
            } else {
                null
            }

            _state.update {
                it.copy(
                    wallet = wallet ?: it.wallet,
                    level = level ?: it.level,
                    ownerLoading = false,
                    ownerError = error,
                )
            }
        }
    }
}
