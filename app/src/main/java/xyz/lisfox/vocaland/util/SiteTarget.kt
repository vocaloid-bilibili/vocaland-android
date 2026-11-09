package xyz.lisfox.vocaland.util

import xyz.lisfox.vocaland.data.model.ArtistKind
import xyz.lisfox.vocaland.data.model.RankingBoard
import xyz.lisfox.vocaland.data.model.VotingKind

sealed interface SiteTarget {
    data class Song(val id: Int) : SiteTarget

    data class Artist(val kind: ArtistKind, val id: Int) : SiteTarget

    data class Tag(val id: Int) : SiteTarget

    data class User(val id: Int) : SiteTarget

    data class Playlist(val id: Int) : SiteTarget

    data object PlaylistSquare : SiteTarget

    data class Voting(val kind: VotingKind, val campaignId: Int) : SiteTarget

    data class Ranking(val board: RankingBoard? = null, val issue: Int? = null) : SiteTarget

    data object CustomRanking : SiteTarget
}

/**
 * target_key 解析后的展示名。
 *
 * 评论接口只给 `target_key`（如 `song:61118`），不带实体名，所以这里退回「类型 #ID」，
 * 与网站首页「最近评论」的兜底文案一致；评分接口带 `target_name` 时优先用真名。
 */
fun SiteTarget.displayLabel(): String = when (this) {
    is SiteTarget.Song -> "歌曲 #$id"
    is SiteTarget.Artist -> "${kind.label} #$id"
    is SiteTarget.Tag -> "标签 #$id"
    is SiteTarget.User -> "用户 #$id"
    is SiteTarget.Playlist -> "歌单 #$id"
    SiteTarget.PlaylistSquare -> "歌单广场"
    is SiteTarget.Voting -> kind.label
    is SiteTarget.Ranking -> when {
        board == null -> "排行榜"
        issue == null -> board.label
        else -> "${board.label} #$issue"
    }

    SiteTarget.CustomRanking -> "自定义排行榜"
}

fun parseTargetKey(targetKey: String): SiteTarget? {
    val key = targetKey.trim()
    if (key.isEmpty()) return null

    val separator = key.indexOf(':')
    if (separator < 0) {
        return when (key.lowercase()) {
            "ranking" -> SiteTarget.Ranking()
            "custom-ranking", "custom_ranking" -> SiteTarget.CustomRanking
            else -> null
        }
    }
    if (separator == 0) return null

    val type = key.substring(0, separator)
    val rest = key.substring(separator + 1).trim()
    if (rest.isEmpty()) return null

    return when (type) {
        "song" -> rest.toId()?.let(SiteTarget::Song)
        "tag" -> rest.toId()?.let(SiteTarget::Tag)
        "user" -> rest.toId()?.let(SiteTarget::User)
        "playlist" -> if (rest.equals("public-square", ignoreCase = true)) {
            SiteTarget.PlaylistSquare
        } else {
            rest.toId()?.let(SiteTarget::Playlist)
        }
        "artist" -> parseArtistTarget(rest)
        "ranking" -> parseRankingTarget(rest)
        "interaction" -> {
            val subSeparator = rest.indexOf(':')
            if (subSeparator <= 0) {
                null
            } else {
                val subType = rest.substring(0, subSeparator).lowercase()
                val subId = rest.substring(subSeparator + 1).trim()
                if (subType == "tag") {
                    subId.toId()?.let(SiteTarget::Tag)
                } else {
                    null
                }
            }
        }

        else -> null
    }
}

fun parseSitePath(raw: String): SiteTarget? {
    val path = normalizeSitePath(raw) ?: return null

    val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
    if (segments.isEmpty()) return null

    return when (segments.first()) {
        "song" -> segments.singleId()?.let(SiteTarget::Song)
        "producer" -> segments.singleId()?.let { SiteTarget.Artist(ArtistKind.Producer, it) }
        "vocalist" -> segments.singleId()?.let { SiteTarget.Artist(ArtistKind.Vocalist, it) }
        "synthesizer" -> segments.singleId()?.let { SiteTarget.Artist(ArtistKind.Synthesizer, it) }
        "uploader" -> segments.singleId()?.let { SiteTarget.Artist(ArtistKind.Uploader, it) }
        "user" -> segments.singleId()?.let(SiteTarget::User)
        "tag" -> segments.singleId()?.let(SiteTarget::Tag)
        "playlist" -> segments.singleId()?.let(SiteTarget::Playlist)
        // 后台内容里有把 ED 活动写成 /campaign/{id} 的旧链接，等价于 /ed/campaign/{id}
        "campaign" -> segments.singleId()?.let { SiteTarget.Voting(VotingKind.Ed, it) }
        "ed" -> if (segments.size == 3 && segments[1] == "campaign") {
            segments[2].toId()?.let { SiteTarget.Voting(VotingKind.Ed, it) }
        } else {
            null
        }

        "voting" -> if (
            segments.size == 4 &&
            segments[1] == "trending" &&
            segments[2] == "campaign"
        ) {
            segments[3].toId()?.let { SiteTarget.Voting(VotingKind.Trending, it) }
        } else {
            null
        }

        "ranking" -> when {
            segments.size == 1 -> SiteTarget.Ranking()
            segments.size == 2 -> boardByKey(segments[1])?.let { SiteTarget.Ranking(board = it) }
            segments.size == 3 -> boardByKey(segments[1])?.let {
                SiteTarget.Ranking(board = it, issue = segments[2].toId())
            }

            else -> null
        }

        "custom-ranking" -> if (segments.size == 1) SiteTarget.CustomRanking else null
        else -> null
    }
}

private fun parseRankingTarget(rest: String): SiteTarget? {
    if (rest.equals("custom", ignoreCase = true)) return SiteTarget.CustomRanking

    val parts = rest.split(':')
    if (parts.size > 2) return null
    val board = boardByKey(parts[0]) ?: return null
    return SiteTarget.Ranking(board = board, issue = parts.getOrNull(1)?.toId())
}

private fun boardByKey(key: String): RankingBoard? =
    RankingBoard.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }

/**
 * 把站内链接归一化成 `/a/b`：
 * - 绝对链接只接受 vocabili.top 域名（含 www / api 子域），其他外链返回 null
 * - 去掉 query 与 hash
 */
private fun normalizeSitePath(raw: String): String? {
    val trimmed = raw.trim()
    val path = if (
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        val afterScheme = trimmed.substringAfter("://")
        val host = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        if (!host.endsWith("vocabili.top", ignoreCase = true)) return null
        "/" + afterScheme.substringAfter('/', "")
    } else {
        trimmed
    }

    return path.substringBefore('#').substringBefore('?').trim().takeIf { it.startsWith('/') }
}

private fun parseArtistTarget(rest: String): SiteTarget? {
    val boundary = rest.indexOf('-')
    if (boundary <= 0) return null

    val kind = ArtistKind.fromApiType(rest.substring(0, boundary)) ?: return null
    val id = rest.substring(boundary + 1).toId() ?: return null
    return SiteTarget.Artist(kind, id)
}

private fun String.toId(): Int? = toIntOrNull()?.takeIf { it > 0 }

private fun List<String>.singleId(): Int? =
    if (size == 2) this[1].toId() else null
