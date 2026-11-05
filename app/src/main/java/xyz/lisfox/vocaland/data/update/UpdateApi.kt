package xyz.lisfox.vocaland.data.update

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface UpdateApi {
    @GET
    @Headers(
        "Accept: application/vnd.github+json",
        "User-Agent: VocaLand-Android",
    )
    suspend fun latestRelease(@Url url: String): GitHubReleaseDto
}
