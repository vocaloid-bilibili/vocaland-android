package xyz.lisfox.vocaland.data.share

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 分享图 / CSV 落地。minSdk 29+，优先写 MediaStore（相册 / 下载），
 * 不需要额外存储权限；失败时退回应用私有目录，保证导出能力可用。
 */
object ShareFileStore {
    suspend fun saveImage(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "vocaland-${System.currentTimeMillis()}.png",
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/VocaLand",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching saveImageToAppDir(context, bitmap, fileName)
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.recoverCatching {
            saveImageToAppDir(context, bitmap, fileName)
        }
    }

    suspend fun saveCsv(
        context: Context,
        fileName: String,
        content: String,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/VocaLand",
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching saveCsvToAppDir(context, fileName, content)
            resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(content)
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.recoverCatching {
            saveCsvToAppDir(context, fileName, content)
        }
    }

    fun buildCsv(headers: List<String>, rows: List<List<String>>): String {
        val escape: (String) -> String = { cell -> "\"${cell.replace("\"", "\"\"")}\"" }
        val lines = buildList {
            add(headers.joinToString(",") { escape(it) })
            rows.forEach { row ->
                add(row.joinToString(",") { escape(it) })
            }
        }
        return "\uFEFF" + lines.joinToString("\n")
    }

    private fun saveImageToAppDir(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
    ): Uri {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, "share")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        return Uri.fromFile(file)
    }

    private fun saveCsvToAppDir(
        context: Context,
        fileName: String,
        content: String,
    ): Uri {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "export")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return Uri.fromFile(file)
    }
}
