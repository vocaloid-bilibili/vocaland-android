package xyz.lisfox.vocaland.data.danmaku

import android.util.LruCache
import android.util.Xml
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Inflater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import xyz.lisfox.vocaland.data.model.DanmakuItem

private const val DANMAKU_BASE_URL = "https://comment.bilibili.com/"
private const val CACHE_SIZE = 8
private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

class DanmakuRepository(
    private val client: OkHttpClient,
) {
    private val cache = LruCache<Long, List<DanmakuItem>>(CACHE_SIZE)

    suspend fun load(cid: Long): Result<List<DanmakuItem>> = withContext(Dispatchers.IO) {
        cache.get(cid)?.let { return@withContext Result.success(it) }
        runCatching {
            fetch(cid).also { items -> cache.put(cid, items) }
        }.onFailure { error ->
            if (error is CancellationException) throw error
        }
    }

    private fun fetch(cid: Long): List<DanmakuItem> {
        val request = Request.Builder()
            .url(DANMAKU_BASE_URL + cid + ".xml")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("响应为空")
            val data = body.bytes()
            val xml = if (
                response.header("Content-Encoding")?.contains("deflate", ignoreCase = true) == true
            ) {
                inflate(data)
            } else {
                data
            }
            return parse(xml)
        }
    }

    private fun parse(xml: ByteArray): List<DanmakuItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(xml), "UTF-8")

        val items = mutableListOf<DanmakuItem>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "d") {
                val p = parser.getAttributeValue(null, "p")
                val text = parser.nextText()
                parseItem(p, text)?.let(items::add)
                event = parser.eventType
            }
            event = parser.next()
        }
        items.sortBy { it.timeMs }
        return items
    }

    private fun parseItem(p: String?, text: String): DanmakuItem? {
        if (p.isNullOrBlank() || text.isBlank()) return null
        val parts = p.split(',')
        if (parts.size < 4) return null
        val timeSeconds = parts[0].toFloatOrNull() ?: return null
        val mode = parts[1].toIntOrNull() ?: return null
        if (mode !in 1..3) return null
        val color = parts[3].toIntOrNull() ?: 0xFFFFFF
        return DanmakuItem(
            timeMs = (timeSeconds * 1000).toLong(),
            text = text,
            color = color and 0xFFFFFF,
        )
    }

    private fun inflate(data: ByteArray): ByteArray =
        runCatching { inflate(data, nowrap = true) }
            .getOrElse { inflate(data, nowrap = false) }

    private fun inflate(data: ByteArray, nowrap: Boolean): ByteArray {
        val inflater = Inflater(nowrap)
        try {
            inflater.setInput(data)
            val output = ByteArrayOutputStream(data.size * 3)
            val buffer = ByteArray(8 * 1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count > 0) {
                    output.write(buffer, 0, count)
                } else if (inflater.needsInput() || inflater.needsDictionary()) {
                    break
                }
            }
            return output.toByteArray()
        } finally {
            inflater.end()
        }
    }
}
