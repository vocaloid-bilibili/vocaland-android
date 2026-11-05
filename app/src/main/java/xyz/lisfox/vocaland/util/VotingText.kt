package xyz.lisfox.vocaland.util

import xyz.lisfox.vocaland.data.model.EdVotingCandidateDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto

fun boardLabel(board: String): String = when (board) {
    "vocaloid-weekly" -> "周刊"
    "vocaloid-monthly" -> "月刊"
    "vocaloid-daily" -> "日刊"
    else -> board
}

fun trendingEntityKindLabel(kind: String): String = when (kind) {
    "song" -> "歌曲"
    "producer" -> "P主"
    "vocalist" -> "歌手"
    else -> kind
}

fun votingStatusLabel(status: String): String = when (status) {
    "nomination" -> "提名中"
    "voting" -> "投票中"
    "finalized" -> "已结算"
    else -> status
}

fun EdVotingCandidateDto.displayTitle(): String =
    song?.displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: song?.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: video?.title?.trim()?.takeIf { it.isNotEmpty() }
        ?: bvid

fun EdVotingCandidateDto.displayArtistLine(): String {
    val producers = song?.producers.orEmpty()
        .map { it.name.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(" / ")
    val vocalists = song?.vocalists.orEmpty()
        .map { it.name.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(" / ")
    return listOf(producers, vocalists)
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
        .ifBlank { video?.uploaderName.orEmpty() }
}

fun EdVotingCandidateDto.categoryLabels(): List<String> = buildList {
    if (isDaily) add("日榜")
    if (isWeekly) add("周榜")
    if (isMonthly) add("月榜")
}

fun TrendingCandidateDto.displayTitle(): String = when (entityKind) {
    "song" -> song?.displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: song?.name?.trim()?.takeIf { it.isNotEmpty() }
        ?: "未知歌曲"

    "producer" -> producer?.name?.trim()?.takeIf { it.isNotEmpty() } ?: "未知 P主"
    "vocalist" -> vocalist?.name?.trim()?.takeIf { it.isNotEmpty() } ?: "未知歌手"
    else -> "未知候选"
}

fun TrendingCandidateDto.displaySubtitle(): String = when (entityKind) {
    "song" -> song?.artistLine().orEmpty()
    "producer" -> "P主"
    "vocalist" -> "歌手"
    else -> entityKind
}
