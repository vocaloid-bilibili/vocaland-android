package xyz.lisfox.vocaland.ui.stat

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
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.session.AuthSession
import xyz.lisfox.vocaland.data.stat.StatFilterState
import xyz.lisfox.vocaland.data.stat.StatOrder
import xyz.lisfox.vocaland.data.stat.StatOrderBy
import xyz.lisfox.vocaland.data.stat.StatPage
import xyz.lisfox.vocaland.data.stat.StatQuery
import xyz.lisfox.vocaland.data.stat.StatRepository
import xyz.lisfox.vocaland.data.stat.VocaRole
import xyz.lisfox.vocaland.data.stat.hasRole
import xyz.lisfox.vocaland.data.stat.resolveStatAccess
import xyz.lisfox.vocaland.data.stat.statMaxAccessiblePage

data class StatUiState(
    val role: VocaRole = VocaRole.Guest,
    val filter: StatFilterState = StatFilterState(),
    val page: Int = 1,
    val orderBy: StatOrderBy = StatOrderBy.View,
    val order: StatOrder = StatOrder.Desc,
    val includeAll: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val data: StatPage? = null,
) {
    val canFilter: Boolean get() = hasRole(role, VocaRole.Sponsor)
    val canSort: Boolean get() = hasRole(role, VocaRole.User)
    val canIncludeAll: Boolean get() = hasRole(role, VocaRole.Worker)
    val maxAccessiblePage: Int? get() = statMaxAccessiblePage(role)
    val total: Int get() = data?.total ?: 0
    val totalPages: Int get() = data?.totalPages ?: 1
    val effectivePage: Int get() = data?.page ?: page
}

class StatViewModel(
    private val repository: StatRepository,
    private val session: AuthSession,
) : ViewModel() {
    private val _state = MutableStateFlow(StatUiState())
    val state: StateFlow<StatUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            session.state
                .map { VocaRole.fromKey(it.user?.role) }
                .distinctUntilChanged()
                .collectLatest { role ->
                    _state.update { it.copy(role = role) }
                    load()
                }
        }
    }

    fun refresh() = load()

    fun applyFilter(filter: StatFilterState) {
        _state.update { it.copy(filter = filter, page = 1) }
        load()
    }

    fun resetFilter() {
        _state.update {
            it.copy(
                filter = StatFilterState(),
                page = 1,
                orderBy = StatOrderBy.View,
            )
        }
        load()
    }

    fun selectOrderBy(orderBy: StatOrderBy) {
        _state.update { it.copy(orderBy = orderBy, page = 1) }
        load()
    }

    fun toggleOrder() {
        _state.update {
            it.copy(
                order = if (it.order == StatOrder.Desc) StatOrder.Asc else StatOrder.Desc,
                page = 1,
            )
        }
        load()
    }

    fun setIncludeAll(includeAll: Boolean) {
        if (!_state.value.canIncludeAll) return
        _state.update { it.copy(includeAll = includeAll, page = 1) }
        load()
    }

    fun previousPage() {
        if (_state.value.effectivePage <= 1) return
        _state.update { it.copy(page = (it.page - 1).coerceAtLeast(1)) }
        load()
    }

    fun nextPage() {
        val current = _state.value
        val maxPage = current.maxAccessiblePage
        val next = current.page + 1
        if (current.totalPages > 0 && next > current.totalPages) return
        if (maxPage != null && next > maxPage) return
        _state.update { it.copy(page = next) }
        load()
    }

    suspend fun searchEntities(type: String, keyword: String) =
        repository.searchEntities(type = type, keyword = keyword)

    private fun load() {
        loadJob?.cancel()
        val requested = StatQuery(
            filter = _state.value.filter,
            page = _state.value.page,
            orderBy = _state.value.orderBy,
            order = _state.value.order,
            includeAll = _state.value.includeAll,
        )
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val effective = resolveStatAccess(_state.value.role, requested)
            repository.loadSongs(effective)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = null,
                            data = page,
                            page = page.page,
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
