package xyz.lisfox.vocaland.data.update

import java.io.IOException
import kotlinx.coroutines.CancellationException
import xyz.lisfox.vocaland.BuildConfig

class UpdateRepository(
    private val api: UpdateApi,
) {
    suspend fun checkForUpdate(): AppUpdate? {
        var failure: Throwable? = null
        for (url in RELEASE_URLS) {
            try {
                return api.latestRelease(url).toAppUpdate()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                failure = error
            }
        }
        throw failure ?: IOException("Unable to load release information")
    }

    private fun GitHubReleaseDto.toAppUpdate(): AppUpdate? {
        if (draft || prerelease) return null
        val version = tagName.removePrefix("v").trim()
        if (!isNewerVersion(version, BuildConfig.VERSION_NAME)) return null
        val asset = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return null
        return AppUpdate(
            versionName = version,
            changelog = body.orEmpty().trim(),
            publishedAt = publishedAt,
            apkName = asset.name,
            apkUrl = asset.browserDownloadUrl,
            mirrorApkUrl = MIRROR_PREFIX + asset.browserDownloadUrl,
        )
    }

    private companion object {
        const val REPOSITORY = "vocaloid-bilibili/vocaland-android"
        const val GITHUB_API =
            "https://api.github.com/repos/" + REPOSITORY + "/releases/latest"
        const val MIRROR_PREFIX = "https://gh-proxy.com/"
        val RELEASE_URLS = listOf(GITHUB_API, MIRROR_PREFIX + GITHUB_API)

        fun isNewerVersion(remote: String, current: String): Boolean {
            val remoteParts = versionParts(remote)
            val currentParts = versionParts(current)
            val size = maxOf(remoteParts.size, currentParts.size)
            for (index in 0 until size) {
                val remotePart = remoteParts.getOrElse(index) { 0 }
                val currentPart = currentParts.getOrElse(index) { 0 }
                if (remotePart != currentPart) return remotePart > currentPart
            }
            return false
        }

        fun versionParts(value: String): List<Int> = value
            .trim()
            .removePrefix("v")
            .substringBefore('-')
            .substringBefore('+')
            .split('.')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    }
}
