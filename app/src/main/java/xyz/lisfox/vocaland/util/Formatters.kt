package xyz.lisfox.vocaland.util

import android.util.Base64
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.SongBriefDto
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.SongProducerDto
import xyz.lisfox.vocaland.data.model.SongVocalistDto
import java.time.Instant
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

fun targetLabel(targetKey: String): String {
    val separator = targetKey.indexOf(':')
    if (separator <= 0) return targetKey

    val type = targetKey.substring(0, separator)
    val rest = targetKey.substring(separator + 1)

    return when (type) {
        "song" -> "歌曲 #$rest"
        "tag" -> "标签 #$rest"
        "user" -> "用户 #$rest"
        "artist" -> {
            val boundary = rest.indexOf('-')
            if (boundary <= 0) {
                targetKey
            } else {
                "${artistKindLabel(rest.substring(0, boundary))} #${rest.substring(boundary + 1)}"
            }
        }

        "interaction" -> {
            if (rest.startsWith("tag:")) {
                "标签 #${rest.removePrefix("tag:")}"
            } else {
                targetKey
            }
        }

        else -> targetKey
    }
}

private fun artistKindLabel(kind: String): String = when (kind) {
    "producer" -> "P主"
    "vocalist" -> "歌手"
    "synthesizer" -> "引擎"
    "uploader" -> "UP主"
    else -> kind
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
