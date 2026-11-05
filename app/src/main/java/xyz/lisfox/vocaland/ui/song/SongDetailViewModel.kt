package xyz.lisfox.vocaland.ui.song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.CommentDto
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.RatingSummaryDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.SongRankingStatsDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.SongDetailRepository
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.data.songactions.SongActionsRepository
import xyz.lisfox.vocaland.data.songactions.SongListenStateDto
import xyz.lisfox.vocaland.data.songactions.SongListenStatus
import xyz.lisfox.vocaland.data.songactions.sortSongVideos

data class SongDetailUiState(
    val song: SongDto? = null,
    val tags: List<TagItemDto> = emptyList(),
    val selectedBvid: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val listenStatus: SongListenStatus? = null,
    val listenLitCount: Int = 0,
    val listenLoading: Boolean = false,
    val listenUpdating: Boolean = false,
    val listenError: String? = null,
    val ratingSummary: RatingSummaryDto? = null,
    val ratings: List<RatingDto> = emptyList(),
    val ratingsPage: Int = 1,
    val ratingsTotalPages: Int = 1,
    val ratingsLoading: Boolean = true,
    val ratingsError: String? = null,
    val comments: List<CommentDto> = emptyList(),
    val commentsPage: Int = 1,
    val commentsTotal: Int = 0,
    val commentsTotalPages: Int = 1,
    val commentsLoading: Boolean = false,
    val commentsError: String? = null,
    val rankingBoard: RankingBoard = RankingBoard.Daily,
    val rankingStats: SongRankingStatsDto? = null,
    val rankingHistory: List<RankingDto> = emptyList(),
    val rankingTotal: Int = 0,
    val rankingLoading: Boolean = false,
    val rankingLoaded: Boolean = false,
    val rankingError: String? = null,
)

class SongDetailViewModel(
    private val repository: SongDetailRepository,
    private val session: AuthSession,
    private val songId: Int,
    private val songActions: SongActionsRepository? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(SongDetailUiState())
    val state: StateFlow<SongDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var summaryJob: Job? = null
    private var ratingsJob: Job? = null
    private var commentsJob: Job? = null
    private var rankingJob: Job? = null
    private var litCountJob: Job? = null
    private var listenJob: Job? = null
    private var listenActionJob: Job? = null

    init {
        observeSession()
        loadSong()
        loadRatingSummary()
        loadRatings(1)
        loadLitCount()
    }

    fun selectVideo(bvid: String) {
        if (_state.value.selectedBvid == bvid) return
        _state.update { it.copy(selectedBvid = bvid) }
    }

    fun setListenStatus(status: SongListenStatus) {
        val actions = songActions ?: return
        val current = _state.value
        if (!current.isLoggedIn || current.listenUpdating) return

        listenActionJob?.cancel()
        _state.update { it.copy(listenUpdating = true, listenError = null) }
        listenActionJob = viewModelScope.launch {
            val result = actions.setListenStatus(songId, status)
            if (!isActive) return@launch
            result
                .onSuccess(::applyListenState)
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            listenUpdating = false,
                            listenError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun clearListenStatus() {
        val actions = songActions ?: return
        val current = _state.value
        if (!current.isLoggedIn || current.listenUpdating) return

        listenActionJob?.cancel()
        _state.update { it.copy(listenUpdating = true, listenError = null) }
        listenActionJob = viewModelScope.launch {
            val result = actions.clearListenStatus(songId)
            if (!isActive) return@launch
            result
                .onSuccess(::applyListenState)
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            listenUpdating = false,
                            listenError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun retryListen() {
        loadLitCount()
        if (_state.value.isLoggedIn) {
            loadListenState()
        }
    }

    fun retrySong() {
        loadSong()
    }

    fun retryRatings() {
        if (_state.value.ratingSummary == null) {
            loadRatingSummary()
        }
        loadRatings(_state.value.ratingsPage)
    }

    fun previousRatingsPage() {
        val current = _state.value
        if (current.ratingsPage <= 1 || current.ratingsLoading) return
        loadRatings(current.ratingsPage - 1)
    }

    fun nextRatingsPage() {
        val current = _state.value
        if (current.ratingsPage >= current.ratingsTotalPages || current.ratingsLoading) return
        loadRatings(current.ratingsPage + 1)
    }

    fun retryComments() {
        if (_state.value.isLoggedIn) {
            loadComments(_state.value.commentsPage)
        }
    }

    fun previousCommentsPage() {
        val current = _state.value
        if (current.commentsPage <= 1 || current.commentsLoading) return
        loadComments(current.commentsPage - 1)
    }

    fun nextCommentsPage() {
        val current = _state.value
        if (current.commentsPage >= current.commentsTotalPages || current.commentsLoading) return
        loadComments(current.commentsPage + 1)
    }

    fun selectRankingBoard(board: RankingBoard) {
        if (_state.value.rankingBoard == board) return
        _state.update {
            it.copy(
                rankingBoard = board,
                rankingStats = null,
                rankingHistory = emptyList(),
                rankingTotal = 0,
                rankingLoaded = false,
                rankingLoading = false,
                rankingError = null,
            )
        }
        if (_state.value.isLoggedIn) {
            loadRanking(board)
        }
    }

    fun retryRanking() {
        if (_state.value.isLoggedIn) {
            loadRanking(_state.value.rankingBoard)
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            session.state
                .map { it.isLoggedIn }
                .distinctUntilChanged()
                .collect { loggedIn ->
                    _state.update { it.copy(isLoggedIn = loggedIn) }
                    if (loggedIn) {
                        loadComments(1)
                        loadRanking(_state.value.rankingBoard)
                        loadListenState()
                    } else {
                        commentsJob?.cancel()
                        rankingJob?.cancel()
                        listenJob?.cancel()
                        listenActionJob?.cancel()
                        _state.update {
                            it.copy(
                                comments = emptyList(),
                                commentsPage = 1,
                                commentsTotal = 0,
                                commentsTotalPages = 1,
                                commentsLoading = false,
                                commentsError = null,
                                rankingStats = null,
                                rankingHistory = emptyList(),
                                rankingTotal = 0,
                                rankingLoading = false,
                                rankingLoaded = false,
                                rankingError = null,
                                listenStatus = null,
                                listenLoading = false,
                                listenUpdating = false,
                                listenError = null,
                            )
                        }
                    }
                }
        }
    }

    private fun loadSong() {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }

        loadJob = viewModelScope.launch {
            val result = repository.load(songId)
            if (!isActive) return@launch
            result
                .onSuccess { content ->
                    val videos = sortSongVideos(content.song.videos)
                    val selectedBvid = _state.value.selectedBvid
                        ?.takeIf { bvid -> videos.any { it.bvid == bvid } }
                        ?: videos.firstOrNull { it.bvid.isNotBlank() }?.bvid
                    _state.update {
                        it.copy(
                            song = content.song,
                            tags = content.tags,
                            selectedBvid = selectedBvid,
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

    private fun loadLitCount() {
        val actions = songActions ?: return
        litCountJob?.cancel()
        litCountJob = viewModelScope.launch {
            val result = actions.loadLitCount(songId)
            if (!isActive) return@launch
            result.onSuccess { count ->
                _state.update { it.copy(listenLitCount = count) }
            }
        }
    }

    private fun loadListenState() {
        val actions = songActions ?: return
        if (!_state.value.isLoggedIn) return

        listenJob?.cancel()
        _state.update { it.copy(listenLoading = true, listenError = null) }
        listenJob = viewModelScope.launch {
            val result = actions.loadListenState(songId)
            if (!isActive) return@launch
            result
                .onSuccess(::applyListenState)
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            listenLoading = false,
                            listenError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun applyListenState(stateDto: SongListenStateDto) {
        _state.update {
            it.copy(
                listenStatus = SongListenStatus.fromApiValue(stateDto.status),
                listenLitCount = stateDto.litCount,
                listenLoading = false,
                listenUpdating = false,
                listenError = null,
            )
        }
    }

    private fun loadRatingSummary() {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            val result = repository.loadRatingSummary(songId)
            if (!isActive) return@launch
            result.onSuccess { summary ->
                _state.update { it.copy(ratingSummary = summary) }
            }
        }
    }

    private fun loadRatings(page: Int) {
        ratingsJob?.cancel()
        _state.update { it.copy(ratingsLoading = true, ratingsError = null) }

        ratingsJob = viewModelScope.launch {
            val result = repository.loadRatings(songId, page)
            if (!isActive) return@launch
            result
                .onSuccess { ratingPage ->
                    _state.update {
                        it.copy(
                            ratings = ratingPage.items,
                            ratingsPage = ratingPage.page,
                            ratingsTotalPages = ratingPage.totalPages,
                            ratingsLoading = false,
                            ratingsError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            ratingsLoading = false,
                            ratingsError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun loadComments(page: Int) {
        commentsJob?.cancel()
        _state.update { it.copy(commentsLoading = true, commentsError = null) }

        commentsJob = viewModelScope.launch {
            val result = repository.loadComments(songId, page)
            if (!isActive) return@launch
            result
                .onSuccess { commentPage ->
                    _state.update {
                        it.copy(
                            comments = commentPage.items,
                            commentsPage = commentPage.page,
                            commentsTotal = commentPage.total,
                            commentsTotalPages = commentPage.totalPages,
                            commentsLoading = false,
                            commentsError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            commentsLoading = false,
                            commentsError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun loadRanking(board: RankingBoard) {
        rankingJob?.cancel()
        _state.update {
            it.copy(
                rankingBoard = board,
                rankingLoading = true,
                rankingError = null,
            )
        }

        rankingJob = viewModelScope.launch {
            val result = repository.loadRanking(songId, board)
            if (!isActive) return@launch
            result
                .onSuccess { content ->
                    _state.update {
                        it.copy(
                            rankingStats = content.stats,
                            rankingHistory = content.history,
                            rankingTotal = content.total,
                            rankingLoading = false,
                            rankingLoaded = true,
                            rankingError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            rankingLoading = false,
                            rankingLoaded = true,
                            rankingError = error.toUserMessage(),
                        )
                    }
                }
        }
    }
}
