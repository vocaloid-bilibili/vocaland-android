package xyz.lisfox.vocaland.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.notification.NotificationEntryDto
import xyz.lisfox.vocaland.data.notification.NotificationRepository
import xyz.lisfox.vocaland.data.remote.toUserMessage

private const val NOTIFICATION_PAGE_SIZE = 20

enum class NotificationTab {
    Unread,
    All,
}

data class NotificationsUiState(
    val tab: NotificationTab = NotificationTab.Unread,
    val page: Int = 1,
    val entries: List<NotificationEntryDto> = emptyList(),
    val total: Int = 0,
    val unread: Int = 0,
    val loading: Boolean = false,
    val markingAll: Boolean = false,
    val error: String? = null,
) {
    val pages: Int
        get() = ((total + NOTIFICATION_PAGE_SIZE - 1) / NOTIFICATION_PAGE_SIZE).coerceAtLeast(1)
}

sealed interface NotificationEvent {
    data class Open(
        val targetKey: String?,
        val commentId: Int?,
    ) : NotificationEvent
}

class NotificationsViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val _events = Channel<NotificationEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setTab(tab: NotificationTab) {
        if (_state.value.tab == tab) return
        _state.update { it.copy(tab = tab, page = 1) }
        load()
    }

    fun setPage(page: Int) {
        val current = _state.value
        if (page < 1 || page > current.pages || page == current.page) return
        _state.update { it.copy(page = page) }
        load()
    }

    fun refresh() {
        load()
    }

    fun markAllRead() {
        if (_state.value.markingAll) return
        _state.update { it.copy(markingAll = true, error = null) }
        viewModelScope.launch {
            repository.markAllRead()
                .onSuccess {
                    _state.update { it.copy(markingAll = false) }
                    load()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            markingAll = false,
                            error = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun open(entry: NotificationEntryDto) {
        if (!entry.isRead) {
            markRead(entry)
        }
        viewModelScope.launch {
            _events.send(
                NotificationEvent.Open(
                    targetKey = entry.targetKey,
                    commentId = entry.commentId,
                ),
            )
        }
    }

    private fun markRead(entry: NotificationEntryDto) {
        _state.update { state ->
            state.copy(
                entries = if (state.tab == NotificationTab.Unread) {
                    state.entries.filterNot { it.id == entry.id }
                } else {
                    state.entries.map { if (it.id == entry.id) it.copy(isRead = true) else it }
                },
                total = if (state.tab == NotificationTab.Unread) {
                    (state.total - 1).coerceAtLeast(0)
                } else {
                    state.total
                },
                unread = (state.unread - 1).coerceAtLeast(0),
            )
        }
        viewModelScope.launch {
            repository.markRead(listOf(entry.id))
        }
    }

    private fun load() {
        val current = _state.value
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.loadPage(
                page = current.page,
                pageSize = NOTIFICATION_PAGE_SIZE,
                unreadOnly = current.tab == NotificationTab.Unread,
            ).onSuccess { page ->
                _state.update {
                    it.copy(
                        entries = page.data,
                        total = page.total,
                        unread = page.unread,
                        loading = false,
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

}
