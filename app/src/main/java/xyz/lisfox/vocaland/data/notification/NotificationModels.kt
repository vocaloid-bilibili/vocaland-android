package xyz.lisfox.vocaland.data.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationActorDto(
    val id: Int = 0,
    val nickname: String = "",
    val avatar: String? = null,
)

@Serializable
data class NotificationEntryDto(
    val id: Int = 0,
    val type: String = "",
    val actor: NotificationActorDto? = null,
    val commentId: Int? = null,
    val targetKey: String? = null,
    val targetName: String? = null,
    val contentPreview: String? = null,
    val isRead: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class NotificationPageDto(
    val data: List<NotificationEntryDto> = emptyList(),
    val total: Int = 0,
    val unread: Int = 0,
)

@Serializable
data class MarkReadRequest(
    val ids: List<Int>,
)

@Serializable
data class MarkReadResult(
    val updated: Int = 0,
)

@Serializable
data class NotificationPreferencesDto(
    val reply: Boolean = false,
    val like: Boolean = false,
    val wallComment: Boolean = false,
    val targetUpdate: Boolean = false,
    val mention: Boolean = false,
    val threadUpdate: Boolean = false,
)

@Serializable
data class NotificationPreferencesPatch(
    val reply: Boolean? = null,
    val like: Boolean? = null,
    val wallComment: Boolean? = null,
    val targetUpdate: Boolean? = null,
    val mention: Boolean? = null,
    val threadUpdate: Boolean? = null,
)

enum class NotificationKind(
    val apiValue: String,
    val verb: String,
    val prefLabel: String,
    val prefDesc: String,
) {
    Reply("reply", "回复了你", "回复", "有人回复你的评论"),
    Mention("mention", "提到了你", "提及", "有人在评论中 @ 你"),
    Like("like", "赞了你的评论", "点赞", "有人赞了你的评论"),
    WallComment("wall_comment", "在你的主页留言", "留言", "有人在你的主页留言"),
    ThreadUpdate("thread_update", "在讨论中发了新回复", "讨论更新", "你参与的讨论有新回复"),
    TargetUpdate("target_update", "在你评论过的地方发了新评论", "评论区动态", "你评论过的地方有新评论"),
    ;

    companion object {
        fun fromApi(value: String?): NotificationKind? =
            entries.firstOrNull { it.apiValue == value }

        val ordered: List<NotificationKind> = listOf(
            Reply,
            Mention,
            Like,
            WallComment,
            ThreadUpdate,
            TargetUpdate,
        )
    }
}

fun NotificationPreferencesDto.isEnabled(kind: NotificationKind): Boolean = when (kind) {
    NotificationKind.Reply -> reply
    NotificationKind.Mention -> mention
    NotificationKind.Like -> like
    NotificationKind.WallComment -> wallComment
    NotificationKind.ThreadUpdate -> threadUpdate
    NotificationKind.TargetUpdate -> targetUpdate
}

fun NotificationPreferencesDto.with(kind: NotificationKind, enabled: Boolean): NotificationPreferencesDto =
    when (kind) {
        NotificationKind.Reply -> copy(reply = enabled)
        NotificationKind.Mention -> copy(mention = enabled)
        NotificationKind.Like -> copy(like = enabled)
        NotificationKind.WallComment -> copy(wallComment = enabled)
        NotificationKind.ThreadUpdate -> copy(threadUpdate = enabled)
        NotificationKind.TargetUpdate -> copy(targetUpdate = enabled)
    }

fun NotificationPreferencesPatch.forKind(kind: NotificationKind, enabled: Boolean): NotificationPreferencesPatch =
    when (kind) {
        NotificationKind.Reply -> copy(reply = enabled)
        NotificationKind.Mention -> copy(mention = enabled)
        NotificationKind.Like -> copy(like = enabled)
        NotificationKind.WallComment -> copy(wallComment = enabled)
        NotificationKind.ThreadUpdate -> copy(threadUpdate = enabled)
        NotificationKind.TargetUpdate -> copy(targetUpdate = enabled)
    }

fun notificationPreviewText(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return raw
        .replace("<[^>]+>".toRegex(), "")
        .replace("\\[([^\\]]*)\\]\\([^)]*\\)".toRegex(), "$1")
        .replace("[*_~`#>]".toRegex(), "")
        .trim()
}
