package xyz.lisfox.vocaland.data.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

fun enqueueUpdateDownload(
    context: Context,
    update: AppUpdate,
    source: UpdateDownloadSource,
): Boolean {
    val manager = context.getSystemService(DownloadManager::class.java) ?: return false
    return runCatching {
        val fileName = buildString {
            append(update.apkName.removeSuffix(".apk"))
            append('-')
            append(System.currentTimeMillis())
            append(".apk")
        }
        val request = DownloadManager.Request(Uri.parse(update.downloadUrl(source)))
            .setTitle("VocaLand ${update.versionName}")
            .setDescription("正在下载应用更新")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        manager.enqueue(request)
        true
    }.getOrDefault(false)
}
