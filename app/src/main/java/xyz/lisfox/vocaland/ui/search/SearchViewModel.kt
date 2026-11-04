package xyz.lisfox.vocaland.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.model.SearchGroupDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.repository.SearchRepository

data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val target: String? = null,
    val page: Int = 1,
    val groups: List<SearchGroupDto> = emptyList(),
    val total: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = false,
    val searched: Boolean = false,
    val error: String? = null,
)

class SearchViewModel(
    private val repository: SearchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value.take(MAX_QUERY_LENGTH)) }
    }

    fun clearQuery() {
        loadJob?.cancel()
        _state.value = SearchUiState()
    }

    fun submit() {
        val keyword = _state.value.query.trim()
        if (keyword.isEmpty()) {
            clearQuery()
            return
        }

        loadJob?.cancel()
        _state.update {
            it.copy(
                submittedQuery = keyword,
                page = 1,
                groups = emptyList(),
                total = 0,
                totalPages = 1,
                searched = true,
                error = null,
            )
        }
        load()
    }

    fun selectTarget(target: String?) {
        if (_state.value.target == target) return

        _state.update {
            it.copy(
                target = target,
                page = 1,
                groups = emptyList(),
                total = 0,
                totalPages = 1,
                error = null,
            )
        }
        if (_state.value.submittedQuery.isNotBlank()) {
            load()
        }
    }

    fun previousPage() {
        val current = _state.value
        if (current.page <= 1) return

        _state.update { it.copy(page = current.page - 1) }
        load()
    }

    fun nextPage() {
        val current = _state.value
        if (current.page >= current.totalPages) return

        _state.update { it.copy(page = current.page + 1) }
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        val snapshot = _state.value
        if (snapshot.submittedQuery.isBlank()) return

        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }

        loadJob = viewModelScope.launch {
            val target = snapshot.target
            val result = if (target == null) {
                repository.searchAll(snapshot.submittedQuery, snapshot.page)
            } else {
                repository.searchType(
                    type = target,
                    keyword = snapshot.submittedQuery,
                    page = snapshot.page,
                )
            }

            result.onSuccess { pageData ->
                _state.update {
                    it.copy(
                        page = pageData.page,
                        groups = pageData.groups,
                        total = pageData.total,
                        totalPages = pageData.totalPages,
                        loading = false,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.toUserMessage(),
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 80
    }
}
