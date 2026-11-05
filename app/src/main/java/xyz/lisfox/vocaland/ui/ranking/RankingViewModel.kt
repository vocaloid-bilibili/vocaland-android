package xyz.lisfox.vocaland.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.discovery.ArtistRankingItemDto
import xyz.lisfox.vocaland.data.discovery.ArtistRankingPage
import xyz.lisfox.vocaland.data.discovery.ArtistRankingSongsPage
import xyz.lisfox.vocaland.data.discovery.MilestonePage
import xyz.lisfox.vocaland.data.discovery.roleAtLeast
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.RankingOrder
import xyz.lisfox.vocaland.data.model.RankingPage
import xyz.lisfox.vocaland.data.model.RankingTab
import xyz.lisfox.vocaland.data.model.availableRankingTabs
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.RankingRepository

enum class RankingViewMode {
    List,
    Grid,
}

data class RankingUiState(
    val board: RankingBoard = RankingBoard.Weekly,
    val tab: RankingTab = RankingTab.Main,
    val order: RankingOrder = RankingOrder.Point,
    val viewMode: RankingViewMode = RankingViewMode.List,
    val issue: Int? = null,
    val latestIssue: Int? = null,
    val oldestIssue: Int? = null,
    val page: Int = 1,
    val data: RankingPage? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val issueNotice: String? = null,
    val role: String = "guest",
    val artistPage: Int = 1,
    val artistData: ArtistRankingPage? = null,
    val selectedArtist: ArtistRankingItemDto? = null,
    val artistSongsPage: Int = 1,
    val artistSongs: ArtistRankingSongsPage? = null,
    val milestonePage: Int = 1,
    val milestoneData: MilestonePage? = null,
    val tabLoading: Boolean = false,
    val tabError: String? = null,
    val permissionHint: String? = null,
) {
    val tabs: List<RankingTab>
        get() = availableRankingTabs(board)

    val part: String
        get() = if (tab == RankingTab.New && board.supportsNewPart) "new" else "main"

    val isSponsor: Boolean
        get() = roleAtLeast(role, "sponsor")

    val isUser: Boolean
        get() = roleAtLeast(role, "user")

    val displayIssue: Int?
        get() = issue ?: data?.issue ?: artistData?.issue ?: latestIssue
}

class RankingViewModel(
    private val repository: RankingRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RankingUiState())
    val state: StateFlow<RankingUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var tabJob: Job? = null

    init {
        loadCurrent()
    }

    fun setRole(role: String) {
        if (_state.value.role == role) return
        _state.update { it.copy(role = role) }
        loadCurrent()
    }

    fun selectBoard(board: RankingBoard) {
        val current = _state.value
        if (current.board == board) return

        val nextTab = if (availableRankingTabs(board).contains(current.tab)) {
            current.tab
        } else {
            RankingTab.Main
        }

        _state.update {
            it.copy(
                board = board,
                tab = nextTab,
                issue = null,
                page = 1,
                oldestIssue = null,
                data = null,
                error = null,
                issueNotice = null,
                artistPage = 1,
                artistData = null,
                selectedArtist = null,
                artistSongs = null,
                artistSongsPage = 1,
                milestonePage = 1,
                milestoneData = null,
                tabLoading = false,
                tabError = null,
                permissionHint = null,
            )
        }
        loadCurrent()
    }

    fun selectTab(tab: RankingTab) {
        val current = _state.value
        if (current.tab == tab) return
        if (tab !in current.tabs) return

        _state.update {
            it.copy(
                tab = tab,
                page = 1,
                artistPage = 1,
                artistData = null,
                selectedArtist = null,
                artistSongs = null,
                artistSongsPage = 1,
                milestonePage = 1,
                milestoneData = null,
                data = null,
                error = null,
                tabError = null,
                permissionHint = null,
                issueNotice = null,
            )
        }
        loadCurrent()
    }

    fun setViewMode(mode: RankingViewMode) {
        if (_state.value.viewMode == mode) return
        _state.update { it.copy(viewMode = mode) }
    }

    fun selectOrder(order: RankingOrder) {
        if (_state.value.order == order) return
        _state.update {
            it.copy(order = order, page = 1, data = null, error = null, issueNotice = null)
        }
        loadCurrent()
    }

    fun previousPage() {
        when (_state.value.tab) {
            RankingTab.Main, RankingTab.New -> {
                val data = _state.value.data ?: return
                if (data.page <= 1) return
                _state.update { it.copy(page = data.page - 1) }
                loadCurrent()
            }

            RankingTab.Vocalist, RankingTab.Producer -> {
                val data = _state.value.artistData ?: return
                if (_state.value.artistPage <= 1) return
                _state.update {
                    it.copy(
                        artistPage = _state.value.artistPage - 1,
                        selectedArtist = null,
                        artistSongs = null,
                        artistSongsPage = 1,
                    )
                }
                loadArtistTab()
            }

            RankingTab.Milestone -> {
                if (_state.value.milestonePage <= 1) return
                _state.update { it.copy(milestonePage = _state.value.milestonePage - 1) }
                loadMilestoneTab()
            }
        }
    }

    fun nextPage() {
        when (_state.value.tab) {
            RankingTab.Main, RankingTab.New -> {
                val data = _state.value.data ?: return
                if (data.page >= data.totalPages) return
                _state.update { it.copy(page = data.page + 1) }
                loadCurrent()
            }

            RankingTab.Vocalist, RankingTab.Producer -> {
                val data = _state.value.artistData ?: return
                if (_state.value.artistPage >= data.totalPages) return
                _state.update {
                    it.copy(
                        artistPage = _state.value.artistPage + 1,
                        selectedArtist = null,
                        artistSongs = null,
                        artistSongsPage = 1,
                    )
                }
                loadArtistTab()
            }

            RankingTab.Milestone -> {
                val data = _state.value.milestoneData ?: return
                if (_state.value.milestonePage >= data.totalPages) return
                _state.update { it.copy(milestonePage = _state.value.milestonePage + 1) }
                loadMilestoneTab()
            }
        }
    }

    fun previousIssue() {
        val currentIssue = currentIssue() ?: return
        val oldest = _state.value.oldestIssue ?: 1
        if (currentIssue <= oldest) return

        _state.update {
            it.copy(
                issue = currentIssue - 1,
                page = 1,
                artistPage = 1,
                milestonePage = 1,
                data = null,
                artistData = null,
                selectedArtist = null,
                artistSongs = null,
                milestoneData = null,
                error = null,
                tabError = null,
                issueNotice = null,
            )
        }
        loadCurrent()
    }

    fun nextIssue() {
        val currentIssue = currentIssue() ?: return
        val latest = _state.value.latestIssue ?: _state.value.data?.latestIssue ?: return
        if (currentIssue >= latest) return

        _state.update {
            it.copy(
                issue = currentIssue + 1,
                page = 1,
                artistPage = 1,
                milestonePage = 1,
                data = null,
                artistData = null,
                selectedArtist = null,
                artistSongs = null,
                milestoneData = null,
                error = null,
                tabError = null,
                issueNotice = null,
            )
        }
        loadCurrent()
    }

    fun goToLatestIssue() {
        val latest = _state.value.latestIssue ?: return
        if (currentIssue() == latest) return

        _state.update {
            it.copy(
                issue = latest,
                page = 1,
                artistPage = 1,
                milestonePage = 1,
                data = null,
                artistData = null,
                selectedArtist = null,
                artistSongs = null,
                milestoneData = null,
                error = null,
                tabError = null,
                issueNotice = null,
            )
        }
        loadCurrent()
    }

    fun retry() {
        loadCurrent()
    }

    fun selectArtist(item: ArtistRankingItemDto) {
        val current = _state.value
        if (current.selectedArtist?.artist?.id == item.artist.id) {
            _state.update {
                it.copy(
                    selectedArtist = null,
                    artistSongs = null,
                    artistSongsPage = 1,
                )
            }
            return
        }

        _state.update {
            it.copy(
                selectedArtist = item,
                artistSongs = null,
                artistSongsPage = 1,
            )
        }
        loadArtistSongs(1)
    }

    fun retryArtistSongs() {
        loadArtistSongs(_state.value.artistSongsPage)
    }

    fun artistSongsPreviousPage() {
        if (_state.value.artistSongsPage <= 1) return
        loadArtistSongs(_state.value.artistSongsPage - 1)
    }

    fun artistSongsNextPage() {
        val data = _state.value.artistSongs ?: return
        if (_state.value.artistSongsPage >= data.totalPages) return
        loadArtistSongs(_state.value.artistSongsPage + 1)
    }

    private fun currentIssue(): Int? =
        _state.value.issue ?: _state.value.data?.issue ?: _state.value.artistData?.issue
            ?: _state.value.latestIssue

    private fun loadCurrent() {
        when (_state.value.tab) {
            RankingTab.Main, RankingTab.New -> loadSongs()
            RankingTab.Vocalist, RankingTab.Producer -> loadArtistTab()
            RankingTab.Milestone -> loadMilestoneTab()
        }
    }

    private fun loadSongs() {
        val snapshot = _state.value
        loadJob?.cancel()
        tabJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }

        loadJob = viewModelScope.launch {
            repository.loadRanking(
                board = snapshot.board,
                part = snapshot.part,
                order = snapshot.order,
                issue = snapshot.issue,
                page = snapshot.page,
                role = snapshot.role,
            ).onSuccess { pageData ->
                val knownLatest = pageData.latestIssue ?: _state.value.latestIssue
                val notice = when {
                    pageData.requestedIssue != null &&
                        pageData.requestedIssue > pageData.issue ->
                        "该期尚未发布，已切换到第 ${pageData.issue} 期"

                    pageData.requestedIssue != null &&
                        pageData.requestedIssue < pageData.issue &&
                        pageData.issue == knownLatest ->
                        "当前未登录或权限不足，仅可查看最新一期"

                    pageData.requestedIssue != null &&
                        pageData.requestedIssue < pageData.issue ->
                        "该期不在当前账号可查看范围内，已切换到第 ${pageData.issue} 期"

                    pageData.clamped && pageData.page != pageData.requestedPage ->
                        "该页超出可查看范围，已回到第 ${pageData.page} 页"

                    else -> null
                }

                _state.update {
                    it.copy(
                        data = pageData,
                        issue = pageData.issue,
                        latestIssue = pageData.latestIssue ?: it.latestIssue,
                        oldestIssue = pageData.oldestIssue,
                        page = pageData.page,
                        loading = false,
                        error = null,
                        issueNotice = notice,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, error = error.toUserMessage())
                }
            }
        }
    }

    private fun loadArtistTab() {
        val snapshot = _state.value
        val artistType = when (snapshot.tab) {
            RankingTab.Vocalist -> "vocalist"
            RankingTab.Producer -> "producer"
            else -> return
        }

        if (!snapshot.isSponsor) {
            loadJob?.cancel()
            tabJob?.cancel()
            _state.update {
                it.copy(
                    loading = false,
                    tabLoading = false,
                    tabError = null,
                    artistData = null,
                    permissionHint = if (snapshot.isUser) {
                        "歌手 / P主排行是赞助会员专属功能"
                    } else {
                        "登录并开通赞助后可查看歌手 / P主排行"
                    },
                )
            }
            return
        }

        loadJob?.cancel()
        tabJob?.cancel()
        _state.update { it.copy(tabLoading = true, tabError = null, permissionHint = null) }

        tabJob = viewModelScope.launch {
            repository.loadArtistRanking(
                board = snapshot.board,
                issue = snapshot.issue ?: snapshot.latestIssue,
                artistType = artistType,
                page = snapshot.artistPage,
            ).onSuccess { pageData ->
                _state.update {
                    it.copy(
                        artistData = pageData,
                        issue = pageData.issue.takeIf { issue -> issue > 0 } ?: it.issue,
                        artistPage = snapshot.artistPage,
                        tabLoading = false,
                        tabError = null,
                        permissionHint = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        tabLoading = false,
                        tabError = error.toUserMessage(),
                    )
                }
            }
        }
    }

    private fun loadMilestoneTab() {
        val snapshot = _state.value
        if (!snapshot.isUser) {
            loadJob?.cancel()
            tabJob?.cancel()
            _state.update {
                it.copy(
                    loading = false,
                    tabLoading = false,
                    tabError = null,
                    milestoneData = null,
                    permissionHint = "登录后可查看本期里程碑达成",
                )
            }
            return
        }

        val issue = snapshot.issue ?: snapshot.latestIssue ?: return

        loadJob?.cancel()
        tabJob?.cancel()
        _state.update { it.copy(tabLoading = true, tabError = null, permissionHint = null) }

        tabJob = viewModelScope.launch {
            repository.loadMilestones(
                board = snapshot.board,
                issue = issue,
                page = snapshot.milestonePage,
            ).onSuccess { pageData ->
                _state.update {
                    it.copy(
                        milestoneData = pageData,
                        issue = it.issue ?: issue,
                        milestonePage = snapshot.milestonePage,
                        tabLoading = false,
                        tabError = null,
                        permissionHint = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        tabLoading = false,
                        tabError = error.toUserMessage(),
                    )
                }
            }
        }
    }

    private fun loadArtistSongs(page: Int) {
        val snapshot = _state.value
        val artist = snapshot.selectedArtist ?: return
        val artistType = when (snapshot.tab) {
            RankingTab.Vocalist -> "vocalist"
            RankingTab.Producer -> "producer"
            else -> return
        }
        val issue = snapshot.issue ?: snapshot.artistData?.issue ?: snapshot.latestIssue ?: return

        tabJob?.cancel()
        _state.update { it.copy(tabLoading = true, tabError = null) }

        tabJob = viewModelScope.launch {
            repository.loadArtistRankingSongs(
                board = snapshot.board,
                issue = issue,
                artistType = artistType,
                artistId = artist.artist.id,
                page = page,
            ).onSuccess { pageData ->
                _state.update {
                    it.copy(
                        artistSongs = pageData,
                        artistSongsPage = page,
                        tabLoading = false,
                        tabError = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        tabLoading = false,
                        tabError = error.toUserMessage(),
                    )
                }
            }
        }
    }
}
