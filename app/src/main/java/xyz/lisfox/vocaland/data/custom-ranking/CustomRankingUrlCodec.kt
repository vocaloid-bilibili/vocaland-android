package xyz.lisfox.vocaland.data.customranking

import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import xyz.lisfox.vocaland.data.remote.AppJson

/**
 * 自定义排行 URL 状态编解码。
 *
 * 页面 / 入口可以只用一段 `q` 参数保存全部状态（来源 / 公式 / 筛选 / 选项 / 页码），
 * 刷新、分享、返回时 decode 即还原。格式为 JSON + URL 编码，避免逐字段手写
 * 一份映射（与筛选编码的唯一来源 StatFilterCodec 并存）。
 */
object CustomRankingUrlCodec {
    private const val QUERY_KEY = "q"

    fun encode(query: CustomRankingQuery): String {
        val payload = AppJson.encodeToString(query)
        return URLEncoder.encode(payload, Charsets.UTF_8.name())
    }

    fun decode(raw: String?): CustomRankingQuery? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching {
            AppJson.decodeFromString<CustomRankingQuery>(value)
        }.getOrNull()
    }

    /** 从完整 URL / query 串里取 q 参数并解码。 */
    fun decodeFromUrl(url: String?): CustomRankingQuery? {
        val params = parseQueryString(url)
        return decode(params[QUERY_KEY])
    }

    fun buildHref(basePath: String, query: CustomRankingQuery): String {
        val path = basePath.ifBlank { "/custom-ranking" }
        return "$path?$QUERY_KEY=${encode(query)}"
    }

    fun hasState(url: String?): Boolean = !parseQueryString(url)[QUERY_KEY].isNullOrBlank()

    fun parseQueryString(url: String?): Map<String, String> {
        val raw = url?.trim().orEmpty()
        val queryPart = when {
            raw.contains("?") -> raw.substringAfter("?")
            raw.startsWith("q=") -> raw
            else -> ""
        }
        if (queryPart.isBlank()) return emptyMap()
        return queryPart.split("&").mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val index = part.indexOf('=')
            if (index < 0) return@mapNotNull null
            val key = decode(part.substring(0, index))
            val value = decode(part.substring(index + 1))
            key to value
        }.toMap()
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }
            .getOrDefault(value)
}
