package xyz.lisfox.vocaland.data.notification

import kotlinx.serialization.json.JsonObject

class NotificationRepository(
    private val api: NotificationApi,
) {
    suspend fun loadPage(
        page: Int,
        pageSize: Int,
        unreadOnly: Boolean,
    ): Result<NotificationPageDto> = runCatching {
        api.page(page = page, pageSize = pageSize, unreadOnly = unreadOnly)
    }

    suspend fun markRead(ids: List<Int>): Result<MarkReadResult> = runCatching {
        require(ids.isNotEmpty()) { "没有需要标记的通知" }
        api.markRead(MarkReadRequest(ids = ids))
    }

    suspend fun markAllRead(): Result<MarkReadResult> = runCatching {
        api.markAllRead(JsonObject(emptyMap()))
    }

    suspend fun loadPreferences(): Result<NotificationPreferencesDto> = runCatching {
        api.preferences()
    }

    suspend fun updatePreferences(
        patch: NotificationPreferencesPatch,
    ): Result<NotificationPreferencesDto> = runCatching {
        api.updatePreferences(patch)
    }
}
