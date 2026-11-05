package xyz.lisfox.vocaland.ui.customranking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.customranking.CustomRankingCsv
import xyz.lisfox.vocaland.data.customranking.CustomRankingFormula
import xyz.lisfox.vocaland.data.customranking.CustomRankingPage
import xyz.lisfox.vocaland.data.customranking.CustomRankingQuery
import xyz.lisfox.vocaland.data.customranking.CustomRankingRepository
import xyz.lisfox.vocaland.data.customranking.CustomRankingSource
import xyz.lisfox.vocaland.data.customranking.CustomRankingSourceType
import xyz.lisfox.vocaland.data.customranking.MAX_CUSTOM_RANKING_LIMIT
import xyz.lisfox.vocaland.data.model.PublicPlaylistDto
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.data.share.ShareFileStore
import xyz.lisfox.vocaland.data.stat.StatFilterState
import xyz.lisfox.vocaland.data.stat.VocaRole
import xyz.lisfox.vocaland.data.stat.hasRole

data class CustomRankingUiState(
    val role: VocaRole = VocaRole.Guest,
    val draft: CustomRankingQuery = CustomRankingQuery(),
    val applied: CustomRankingQuery? = null,
    val loading: Boolean = false,
    val exporting: Boolean = false,
    val error: String? = null,
    val page: CustomRankingPage? = null,
    val dirty: Boolean = false,
    val playlists: List<PublicPlaylistDto> = emptyList(),
    val playlistsLoading: Boolean = false,
    val message: String? = null,
) {
    val canUse: Boolean get() = hasRole(role, VocaRole.Sponsor)
    val canIncludeAll: Boolean get() = hasRole(role, VocaRole.Worker)
    val hasResult: Boolean get() = page != null
}

class CustomRankingViewModel(
    private val repository: CustomRankingRepository,
    private val session: AuthSession,
    initialQuery: CustomRankingQuery? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(
        CustomRankingUiState(draft = initialQuery ?: CustomRankingQuery()),
    )
    val state: StateFlow<CustomRankingUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var playlistJob: Job? = null

    init {
        viewModelScope.launch {
            session.state
                .map { VocaRole.fromKey(it.user?.role) }
                .distinctUntilChanged()
                .collectLatest { role ->
                    _state.update { it.copy(role = role) }
                    if (!hasRole(role, VocaRole.Sponsor)) {
                        loadJob?.cancel()
                        _state.update { it.copy(loading = false, page = null, applied = null) }
                    }
                }
        }
        if (initialQuery != null) generate()
    }

    fun updateDraft(transform: (CustomRankingQuery) -> CustomRankingQuery) {
        _state.update { current ->
            current.copy(
                draft = transform(current.draft).copy(page = 1),
                dirty = true,
            )
        }
    }

    fun updateFilter(filter: StatFilterState) =
        updateDraft { it.copy(filter = filter) }

    fun updateFormula(formula: CustomRankingFormula) =
        updateDraft { it.copy(formula = formula) }

    fun selectSourceType(type: CustomRankingSourceType) {
        updateDraft { query ->
            query.copy(
                source = CustomRankingSource(
                    type = type.key,
                    playlistId = query.source.playlistId,
                ),
            )
        }
        if (type == CustomRankingSourceType.Playlist) loadPlaylists()
    }

    fun selectPlaylist(playlist: PublicPlaylistDto) {
        updateDraft { query ->
            query.copy(
                source = CustomRankingSource(
                    type = CustomRankingSourceType.Playlist.key,
                    playlistId = playlist.id,
                ),
            )
        }
    }

    fun setIncludeAll(value: Boolean) {
        if (!_state.value.canIncludeAll) return
        updateDraft { it.copy(includeAll = value) }
    }

    fun setDedup(value: Boolean) = updateDraft { it.copy(dedup = value) }

    fun setLimit(value: Int) {
        val limit = value.coerceIn(1, MAX_CUSTOM_RANKING_LIMIT)
        updateDraft { it.copy(limit = limit) }
    }

    fun generate() {
        val current = _state.value
        if (!current.canUse) return
        val applied = current.draft.copy(page = 1)
        _state.update { it.copy(applied = applied, dirty = false) }
        load(applied)
    }

    fun refresh() {
        _state.value.applied?.let(::load)
    }

    fun previousPage() {
        val current = _state.value
        val applied = current.applied ?: return
        if (applied.page <= 1) return
        val next = applied.copy(page = applied.page - 1)
        _state.update { it.copy(applied = next) }
        load(next)
    }

    fun nextPage() {
        val current = _state.value
        val applied = current.applied ?: return
        val totalPages = current.page?.totalPages ?: return
        if (applied.page >= totalPages) return
        val next = applied.copy(page = applied.page + 1)
        _state.update { it.copy(applied = next) }
        load(next)
    }

    fun loadPlaylists(keyword: String? = null) {
        playlistJob?.cancel()
        playlistJob = viewModelScope.launch {
            _state.update { it.copy(playlistsLoading = true) }
            repository.loadPlaylists(keyword = keyword)
                .onSuccess { playlists ->
                    _state.update { it.copy(playlistsLoading = false, playlists = playlists) }
                }
                .onFailure {
                    _state.update { it.copy(playlistsLoading = false) }
                }
        }
    }

    suspend fun searchEntities(type: String, keyword: String): List<SearchEntityDto> =
        repository.searchEntities(type, keyword).getOrDefault(emptyList())

    fun export(context: Context) {
        val current = _state.value
        val applied = current.applied ?: return
        if (current.exporting) return
        viewModelScope.launch {
            _state.update { it.copy(exporting = true, message = null) }
            repository.loadAll(applied)
                .onSuccess { entries ->
                    if (entries.isEmpty()) {
                        _state.update { it.copy(exporting = false, message = "没有可导出的结果") }
                        return@onSuccess
                    }
                    val currentPage = _state.value.page
                    val csv = CustomRankingCsv.build(entries)
                    val name = CustomRankingCsv.fileName(
                        oldDate = currentPage?.oldDate,
                        newDate = currentPage?.newDate,
                    )
                    ShareFileStore.saveCsv(context, name, csv)
                        .onSuccess { uri ->
                            _state.update {
                                it.copy(exporting = false, message = "已导出：$uri")
                            }
                        }
                        .onFailure { error ->
                            _state.update {
                                it.copy(exporting = false, message = error.toUserMessage())
                            }
                        }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(exporting = false, message = error.toUserMessage())
                    }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun load(query: CustomRankingQuery) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.loadPage(query)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = null,
                            page = page,
                            applied = page.query,
                        )
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
