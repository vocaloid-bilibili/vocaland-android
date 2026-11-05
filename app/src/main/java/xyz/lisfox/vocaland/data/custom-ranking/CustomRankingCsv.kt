package xyz.lisfox.vocaland.data.customranking

import java.util.Locale
import xyz.lisfox.vocaland.data.share.ShareFileStore
import xyz.lisfox.vocaland.util.songDisplayName

/**
 * 自定义排行 CSV 导出：中文表头 + 每个数据项的数值列与排名列。
 * 与网站 exportRankingCsv 的列口径一致；只构造 CSV 文本，落盘交给 ShareFileStore。
 */
object CustomRankingCsv {
    private val statColumns = CUSTOM_RANKING_STAT_KEYS.map { key ->
        key to (CUSTOM_RANKING_STAT_LABELS[key] ?: key)
    }

    fun build(entries: List<CustomRankingEntryDto>): String {
        val headers = buildList {
            add("排名")
            add("歌曲")
            add("歌手")
            add("P主")
            statColumns.forEach { (_, label) ->
                add(label)
                add("${label}排名")
            }
            add("得分")
        }
        val rows = entries.map { entry ->
            buildList {
                add(entry.rank.toString())
                add(songDisplayName(entry.song))
                add(vocalistLine(entry))
                add(producerLine(entry))
                statColumns.forEach { (key, _) ->
                    add(entry.statValue(key).toString())
                    add(entry.statRank(key).toString())
                }
                add(String.format(Locale.CHINA, "%.4f", entry.point))
            }
        }
        return ShareFileStore.buildCsv(headers, rows)
    }

    fun fileName(oldDate: String?, newDate: String?): String {
        val stamp = newDate?.takeIf { it.isNotBlank() }
            ?: oldDate?.takeIf { it.isNotBlank() }
            ?: System.currentTimeMillis().toString()
        return "custom-ranking-$stamp.csv"
    }

    private fun vocalistLine(entry: CustomRankingEntryDto): String =
        entry.song.vocalists.joinToString(" / ") { it.vocalist.name }

    private fun producerLine(entry: CustomRankingEntryDto): String =
        entry.song.producers.joinToString(" / ") { it.producer.name }
}
