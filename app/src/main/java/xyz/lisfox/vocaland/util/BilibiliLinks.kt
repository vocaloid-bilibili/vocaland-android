package xyz.lisfox.vocaland.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

fun openBilibiliVideo(context: Context, bvid: String?) {
    val id = bvid?.trim().orEmpty()
    if (id.isEmpty()) return

    openBilibili(
        context = context,
        appUrl = "bilibili://video/$id",
        webUrl = "https://www.bilibili.com/video/$id",
    )
}

fun openBilibiliUser(context: Context, uid: String?) {
    val id = uid?.trim().orEmpty()
    if (id.isEmpty()) return

    openBilibili(
        context = context,
        appUrl = "bilibili://space/$id",
        webUrl = "https://space.bilibili.com/$id",
    )
}

private fun openBilibili(
    context: Context,
    appUrl: String,
    webUrl: String,
) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appUrl))
    if (context !is Activity) {
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (runCatching { context.startActivity(appIntent) }.isSuccess) {
        return
    }

    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
    if (context !is Activity) {
        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(webIntent) }
}
