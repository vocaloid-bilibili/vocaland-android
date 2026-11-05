package xyz.lisfox.vocaland.data.update

import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    val tagName: String,
    val body: String? = null,
    val publishedAt: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@Serializable
data class GitHubReleaseAssetDto(
    val name: String,
    val browserDownloadUrl: String,
)

data class AppUpdate(
    val versionName: String,
    val changelog: String,
    val publishedAt: String?,
    val apkName: String,
    val apkUrl: String,
    val mirrorApkUrls: List<String>,
) {
    fun downloadUrls(source: UpdateDownloadSource): List<String> = when (source) {
        UpdateDownloadSource.GitHub -> listOf(apkUrl)
        UpdateDownloadSource.Mirror -> mirrorApkUrls
    }
}

enum class UpdateDownloadSource {
    GitHub,
    Mirror,
}
