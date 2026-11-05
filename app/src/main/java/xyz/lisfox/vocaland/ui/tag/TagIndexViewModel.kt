package xyz.lisfox.vocaland.ui.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.discovery.TagCategoryCountDto
import xyz.lisfox.vocaland.data.discovery.TagGroupedCategoryDto
import xyz.lisfox.vocaland.data.model.TagItemDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.discovery.TagIndexRepository
import kotlin.random.Random

data class TagIndexUiState(
    val scope: String? = null,
    val category: String? = null,
    val search: String = "",
    val sort: String = "count",
    val page: Int = 1,
    val groups: List<TagGroupedCategoryDto> = emptyList(),
    val tags: List<TagItemDto> = emptyList(),
    val categories: List<TagCategoryCountDto> = emptyList(),
    val totalTags: Int = 0,
    val total: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = true,
    val error: String? = null,
) {
    val filtered: Boolean
        get() = category != null || search.isNotBlank() || scope != null
}

class TagIndexViewModel(
    private val repository: TagIndexRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TagIndexUiState())
    val state: StateFlow<TagIndexUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        load()
    }

    fun setScope(scope: String?) {
        searchJob?.cancel()
        if (_state.value.scope == scope) return
        _state.update { it.copy(scope = scope, page = 1) }
        load()
    }

    fun setCategory(category: String?) {
        searchJob?.cancel()
        if (_state.value.category == category) return
        _state.update { it.copy(category = category, page = 1) }
        load()
    }

    fun setSearch(search: String) {
        _state.update { it.copy(search = search.take(MAX_SEARCH_LENGTH), page = 1) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load()
        }
    }

    fun setSort(sort: String) {
        searchJob?.cancel()
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort, page = 1) }
        load()
    }

    fun clearFilters() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                scope = null,
                category = null,
                search = "",
                page = 1,
            )
        }
        load()
    }

    fun previousPage() {
        if (_state.value.page <= 1) return
        _state.update { it.copy(page = _state.value.page - 1) }
        load()
    }

    fun nextPage() {
        if (_state.value.page >= _state.value.totalPages) return
        _state.update { it.copy(page = _state.value.page + 1) }
        load()
    }

    fun retry() {
        load()
    }

    fun randomTag(): TagItemDto? {
        val pool = if (_state.value.filtered) _state.value.tags else {
            _state.value.groups.flatMap { it.tags }
        }
        return pool.takeIf { it.isNotEmpty() }?.let { Random.nextInt(it.size) }
            ?.let(pool::get)
    }

    private fun load() {
        val snapshot = _state.value
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }

        loadJob = viewModelScope.launch {
            if (snapshot.filtered) {
                repository.loadFiltered(
                    scope = snapshot.scope,
                    category = snapshot.category,
                    search = snapshot.search.trim().ifBlank { null },
                    sort = snapshot.sort,
                    page = snapshot.page,
                ).onSuccess { pageData ->
                    _state.update {
                        it.copy(
                            tags = pageData.tags,
                            total = pageData.total,
                            totalPages = pageData.totalPages,
                            page = snapshot.page.coerceAtMost(pageData.totalPages),
                            loading = false,
                            error = null,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(loading = false, error = error.toUserMessage())
                    }
                }
            } else {
                repository.loadWall(
                    scope = snapshot.scope,
                    sort = snapshot.sort,
                ).onSuccess { wall ->
                    _state.update {
                        it.copy(
                            groups = wall.groups,
                            categories = wall.categories,
                            totalTags = wall.totalTags,
                            tags = emptyList(),
                            total = wall.totalTags,
                            totalPages = 1,
                            page = 1,
                            loading = false,
                            error = null,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(loading = false, error = error.toUserMessage())
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_SEARCH_LENGTH = 64
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
