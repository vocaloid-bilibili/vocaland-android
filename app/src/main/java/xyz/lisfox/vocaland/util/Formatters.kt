package xyz.lisfox.vocaland.util

import android.text.Html
import android.util.Base64
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.SongBriefDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.SongProducerDto
import xyz.lisfox.vocaland.data.model.SongVocalistDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val shanghaiZone: ZoneId = ZoneId.of("Asia/Shanghai")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

fun toHttps(url: String?): String? {
    val trimmed = url?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return trimmed.replaceFirst("http://", "https://")
}

fun songDisplayName(song: SongDto): String =
    song.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: song.name

fun songDisplayName(song: SongBriefDto): String =
    song.displayName?.trim()?.takeIf { it.isNotEmpty() } ?: song.name

fun SearchEntityDto.songDisplayName(): String =
    displayName?.trim()?.takeIf { it.isNotEmpty() } ?: name

fun SearchEntityDto.songArtistLine(): String = artistLine(producers, vocalists)

fun SearchEntityDto.coverUrl(): String? =
    thumbnail?.trim()?.takeIf { it.isNotEmpty() }
        ?: videos.firstOrNull()?.thumbnail?.trim()?.takeIf { it.isNotEmpty() }

fun SongDto.artistLine(): String = artistLine(producers, vocalists)

fun SongDto.coverUrl(): String? =
    thumbnail?.trim()?.takeIf { it.isNotEmpty() }
        ?: videos.firstOrNull()?.thumbnail?.trim()?.takeIf { it.isNotEmpty() }

fun SongBriefDto.artistLine(): String = artistLine(producers, vocalists)

fun artistLine(
    producers: List<SongProducerDto>,
    vocalists: List<SongVocalistDto>,
): String {
    val producerNames = producers.map { it.producer }
        .distinctBy { it.id }
        .joinToString(" / ") { it.name }
    val vocalistNames = vocalists.map { it.vocalist }
        .distinctBy { it.id }
        .joinToString(" / ") { it.name }
    return listOf(producerNames, vocalistNames)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
        .ifBlank { "未知" }
}

fun targetLabel(targetKey: String, targetName: String? = null): String {
    targetName?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    parseTargetKey(targetKey)?.let { return it.displayLabel() }
    return fallbackTargetLabel(targetKey)
}

private fun fallbackTargetLabel(targetKey: String): String {
    val key = targetKey.trim()
    if (key.isEmpty()) return "社区"
    val rest = key.substringAfter(':', "")
    return when (key.substringBefore(':').lowercase()) {
        "page" -> pageLabel(rest.substringBefore(':'))
        "ranking" -> "排行榜"
        "song" -> "歌曲"
        "artist" -> "P主 / 歌手"
        "tag" -> "标签"
        "user" -> "用户"
        "playlist" -> "歌单"
        "ed", "trending", "voting" -> "投票"
        else -> "社区"
    }
}

private fun pageLabel(slug: String): String = when (slug.lowercase()) {
    "home" -> "首页"
    "ranking" -> "排行榜"
    "search" -> "搜索"
    "profile" -> "我的"
    "player" -> "播放器"
    "voting", "vote" -> "投票"
    "collection" -> "收藏"
    "playlist", "playlists" -> "歌单"
    "settings" -> "设置"
    "notifications" -> "通知"
    "tag", "tags" -> "标签"
    "shop" -> "商城"
    "wallet" -> "钱包"
    else -> "页面"
}

fun decodeHtmlText(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    if ('&' !in text && '<' !in text) return text
    return runCatching {
        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}

fun timeAgo(iso: String?): String {
    val millis = parseIsoMillis(iso) ?: return ""
    val diff = max(0L, System.currentTimeMillis() - millis)
    val minutes = diff / 60_000
    if (minutes < 1) return "刚刚"
    if (minutes < 60) return "$minutes 分钟前"

    val hours = minutes / 60
    if (hours < 24) return "$hours 小时前"

    val days = hours / 24
    if (days < 30) return "$days 天前"

    val months = days / 30
    if (months < 12) return "$months 个月前"

    return "${months / 12} 年前"
}

fun formatShortDate(iso: String?): String {
    if (iso.isNullOrBlank() || iso.length < 10) return ""
    return iso.substring(5, 10)
}

fun formatDateTime(iso: String?): String {
    val millis = parseIsoMillis(iso) ?: return ""
    return Instant.ofEpochMilli(millis)
        .atZone(shanghaiZone)
        .format(dateFormatter)
}

fun deadlineText(iso: String?): String? {
    val millis = parseIsoMillis(iso) ?: return null
    val diff = millis - System.currentTimeMillis()
    if (diff <= 0) return "已截止"

    val minutes = diff / 60_000
    if (minutes < 60) return "${max(1, minutes)} 分钟后截止"

    val hours = minutes / 60
    if (hours < 24) return "$hours 小时后截止"

    return "${hours / 24} 天后截止"
}

fun parseIsoMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching {
            // 后端部分接口返回不带时区的本地时间（如 2026-11-05T19:42:51.488972），按站点时区解释
            LocalDateTime.parse(iso.replace(' ', 'T'))
                .atZone(shanghaiZone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
}

fun compactParts(value: Long, decimals: Int = 0): Pair<String, String> {
    val number = if (value < 0) 0 else value
    return when {
        number >= 100_000_000 -> {
            val text = String.format(Locale.CHINA, "%.${decimals}f", number / 100_000_000.0)
            text to "亿"
        }

        number >= 10_000 -> {
            val text = String.format(Locale.CHINA, "%.${decimals}f", number / 10_000.0)
            text to "万"
        }

        else -> formatNumber(number) to ""
    }
}

fun formatNumber(value: Long): String = String.format(Locale.CHINA, "%,d", value)

fun formatDecimal(value: Double, decimals: Int = 1): String =
    String.format(Locale.CHINA, "%.${decimals}f", value)

/** 分数等大数值：整数补千分位，非整数保留一位小数。 */
fun formatScore(value: Double): String =
    if (value % 1.0 == 0.0) {
        formatNumber(value.toLong())
    } else {
        String.format(Locale.CHINA, "%,.1f", value)
    }

fun captchaImageModel(image: String): Any {
    val trimmed = image.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    val payload = if (trimmed.startsWith("data:")) {
        trimmed.substringAfter(',', trimmed)
    } else {
        trimmed
    }
    return runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrDefault(payload)
}
