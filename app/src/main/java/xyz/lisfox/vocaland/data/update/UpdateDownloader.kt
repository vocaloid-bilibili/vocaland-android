package xyz.lisfox.vocaland.data.update

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request

private const val PART_COUNT = 8
private const val MIN_PART_SIZE = 1L * 1024 * 1024
private const val PROGRESS_INTERVAL_MS = 100L
private const val BUFFER_SIZE = 64 * 1024
private const val USER_AGENT = "VocaLand-Android"

class UpdateDownloader(
    private val context: Context,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .dispatcher(
            Dispatcher().apply {
                maxRequests = PART_COUNT
                maxRequestsPerHost = PART_COUNT
            },
        )
        .build()

    suspend fun download(
        update: AppUpdate,
        source: UpdateDownloadSource,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Download")
        val directory = File(downloads, "updates")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("无法创建更新目录")
        }
        val file = File(directory, update.apkName.substringAfterLast('/'))
        var failure: Exception? = null
        for (url in update.downloadUrls(source)) {
            try {
                onProgress(0, 0)
                downloadFrom(url, file, onProgress)
                if (!isApk(file)) throw IOException("下载内容不是有效的安装包")
                return@withContext file
            } catch (error: CancellationException) {
                file.delete()
                throw error
            } catch (error: Exception) {
                file.delete()
                failure = error
            }
        }
        throw failure ?: IOException("没有可用的下载地址")
    }

    private suspend fun downloadFrom(
        url: String,
        file: File,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        val probe = probe(url)
        val total = probe.total
        if (probe.supportsRange && total != null && total > 0 && total >= MIN_PART_SIZE) {
            downloadRanged(url, file, total, onProgress)
        } else {
            downloadSingle(url, file, onProgress)
        }
    }

    private fun probe(url: String): Probe {
        val request = request(url).header("Range", "bytes=0-0").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val total = response.header("Content-Range")
                ?.substringAfterLast('/')
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
            val supportsRange = response.code == 206 && total != null
            val fallbackTotal = if (response.code == 206) {
                null
            } else {
                response.body?.contentLength()?.takeIf { it > 0 }
            }
            return Probe(total = total ?: fallbackTotal, supportsRange = supportsRange)
        }
    }

    private suspend fun downloadRanged(
        url: String,
        file: File,
        total: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        val partCount = minOf(
            PART_COUNT.toLong(),
            ((total + MIN_PART_SIZE - 1) / MIN_PART_SIZE).coerceAtLeast(1),
        ).toInt()
        val partSize = (total + partCount - 1) / partCount
        val downloaded = AtomicLong(0)
        val lastReport = AtomicLong(0)

        file.parentFile?.mkdirs()
        RandomAccessFile(file, "rw").use { access ->
            access.setLength(total)
            val channel = access.channel
            try {
                coroutineScope {
                    (0 until partCount).map { index ->
                        val start = index * partSize
                        val end = minOf(total - 1, start + partSize - 1)
                        async(Dispatchers.IO) {
                            downloadPart(
                                url = url,
                                start = start,
                                end = end,
                                channel = channel,
                                downloaded = downloaded,
                                total = total,
                                lastReport = lastReport,
                                onProgress = onProgress,
                            )
                        }
                    }.awaitAll()
                }
            } finally {
                channel.close()
            }
        }

        if (file.length() != total) {
            file.delete()
            throw IOException("下载文件不完整")
        }
        onProgress(total, total)
    }

    private suspend fun downloadPart(
        url: String,
        start: Long,
        end: Long,
        channel: FileChannel,
        downloaded: AtomicLong,
        total: Long,
        lastReport: AtomicLong,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        val range = "bytes=$start-$end"
        val request = request(url).header("Range", range).build()
        client.newCall(request).execute().use { response ->
            if (response.code != 206) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("响应为空")
            val source = body.source()
            val buffer = ByteArray(BUFFER_SIZE)
            var position = start
            while (true) {
                coroutineContext.ensureActive()
                val read = source.read(buffer, 0, buffer.size)
                if (read == -1) break
                var writtenOffset = 0
                while (writtenOffset < read) {
                    val written = channel.write(
                        ByteBuffer.wrap(buffer, writtenOffset, read - writtenOffset),
                        position + writtenOffset,
                    )
                    if (written <= 0) throw IOException("写入更新文件失败")
                    writtenOffset += written
                }
                position += read
                reportProgress(downloaded.addAndGet(read.toLong()), total, lastReport, onProgress)
            }
            if (position != end + 1) throw IOException("分段下载不完整")
        }
    }

    private suspend fun downloadSingle(
        url: String,
        file: File,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        val request = request(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("响应为空")
            val total = body.contentLength().takeIf { it > 0 } ?: 0
            val source = body.source()
            val buffer = ByteArray(BUFFER_SIZE)
            var downloaded = 0L
            file.outputStream().buffered().use { output ->
                while (true) {
                    coroutineContext.ensureActive()
                    val read = source.read(buffer, 0, buffer.size)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded, total)
                }
            }
            if (total > 0 && file.length() != total) {
                file.delete()
                throw IOException("下载文件不完整")
            }
        }
    }

    private fun isApk(file: File): Boolean {
        if (file.length() < 4) return false
        return file.inputStream().use { input ->
            val header = ByteArray(4)
            input.read(header) == 4 &&
                header[0] == 'P'.code.toByte() &&
                header[1] == 'K'.code.toByte()
        }
    }

    private fun request(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Accept-Encoding", "identity")

    private fun reportProgress(
        downloaded: Long,
        total: Long,
        lastReport: AtomicLong,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        val now = System.currentTimeMillis()
        val previous = lastReport.get()
        if (downloaded >= total || now - previous >= PROGRESS_INTERVAL_MS) {
            if (lastReport.compareAndSet(previous, now)) {
                onProgress(downloaded, total)
            }
        }
    }

    private data class Probe(
        val total: Long?,
        val supportsRange: Boolean,
    )
}
