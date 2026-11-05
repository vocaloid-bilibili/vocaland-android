package xyz.lisfox.vocaland.data.songactions

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class SongListenStateDto(
    val songId: Int = 0,
    val status: String? = null,
    val litCount: Int = 0,
    val updatedAt: String? = null,
)

@Serializable
data class SetSongListenStatusRequest(
    val status: String,
)

@Serializable
data class LitCountDto(
    val entityType: String = "",
    val entityId: Int = 0,
    val litCount: Int = 0,
    val litByMe: Boolean = false,
)

@Serializable
data class LitCountListDto(
    val data: List<LitCountDto> = emptyList(),
)

interface SongActionsApi {
    @GET("listen/songs/{songId}")
    suspend fun songListenState(@Path("songId") songId: Int): SongListenStateDto

    @PUT("listen/songs/{songId}")
    suspend fun setSongListenStatus(
        @Path("songId") songId: Int,
        @Body request: SetSongListenStatusRequest,
    ): SongListenStateDto

    @DELETE("listen/songs/{songId}")
    suspend fun clearSongListenStatus(@Path("songId") songId: Int): SongListenStateDto

    @GET("lit/counts")
    suspend fun litCounts(
        @Query("entity_type") entityType: String,
        @Query("ids") ids: String,
    ): LitCountListDto
}
