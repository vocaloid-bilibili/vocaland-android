package xyz.lisfox.vocaland.data.repository

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.DataEnvelope
import xyz.lisfox.vocaland.data.model.PlaylistAddSongsResult
import xyz.lisfox.vocaland.data.model.PlaylistCoverResult
import xyz.lisfox.vocaland.data.model.PlaylistDetailDto
import xyz.lisfox.vocaland.data.model.PlaylistInfoDto
import xyz.lisfox.vocaland.data.model.PlaylistListResponse
import xyz.lisfox.vocaland.data.model.PlaylistMoveResult
import xyz.lisfox.vocaland.data.model.PlaylistNoteResult
import xyz.lisfox.vocaland.data.model.PlaylistPage
import xyz.lisfox.vocaland.data.model.PublicPlaylistDto
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class PublicPlaylistPage(
    val items: List<PublicPlaylistDto>,
    val total: Int,
    val page: Int,
    val perPage: Int,
)

interface PlaylistWriteApi {
    @GET("playlist")
    suspend fun myPlaylists(
        @Query("song_id") songId: Int? = null,
    ): PlaylistListResponse

    @GET("playlist/{id}")
    suspend fun playlistDetail(
        @Path("id") id: Int,
        @Query("sort_mode") sortMode: String? = null,
    ): DataEnvelope<PlaylistDetailDto>

    @POST("playlist")
    suspend fun createPlaylist(@Body body: JsonObject): PlaylistInfoDto

    @PATCH("playlist/{id}")
    suspend fun updatePlaylist(
        @Path("id") id: Int,
        @Body body: JsonObject,
    ): PlaylistInfoDto

    @DELETE("playlist/{id}")
    suspend fun deletePlaylist(@Path("id") id: Int)

    @POST("playlist/{id}/songs")
    suspend fun addSongs(
        @Path("id") id: Int,
        @Body body: JsonObject,
    ): PlaylistAddSongsResult

    @DELETE("playlist/{id}/songs/{songId}")
    suspend fun removeSong(
        @Path("id") id: Int,
        @Path("songId") songId: Int,
    )

    @PATCH("playlist/{id}/songs/{songId}")
    suspend fun saveNote(
        @Path("id") id: Int,
        @Path("songId") songId: Int,
        @Body body: JsonObject,
    ): PlaylistNoteResult

    @POST("playlist/{id}/songs/{songId}/move")
    suspend fun moveSong(
        @Path("id") id: Int,
        @Path("songId") songId: Int,
        @Body body: JsonObject,
    ): PlaylistMoveResult

    @PATCH("playlist/{id}/cover")
    suspend fun setCover(
        @Path("id") id: Int,
        @Body body: JsonObject,
    ): PlaylistCoverResult
}

class PlaylistRepository(
    private val v3: VocaLandApi,
    private val writeApi: PlaylistWriteApi? = null,
) {
    suspend fun loadPublicPage(
        page: Int = 1,
        sort: String = "hot",
        keyword: String? = null,
    ): Result<PublicPlaylistPage> = runCatching {
        val response = v3.publicPlaylists(
            page = page,
            sort = sort,
            keyword = keyword?.trim()?.takeIf { it.isNotEmpty() },
        )
        PublicPlaylistPage(
            items = response.data,
            total = response.total,
            page = response.page,
            perPage = response.perPage,
        )
    }

    suspend fun loadMyPlaylists(songId: Int? = null): Result<PlaylistPage> = runCatching {
        val response = requireWriteApi().myPlaylists(songId)
        PlaylistPage(
            items = response.data,
            total = response.total,
            limit = response.limit,
        )
    }

    suspend fun loadDetail(
        id: Int,
        sortMode: String? = null,
    ): Result<PlaylistDetailDto> = runCatching {
        val api = writeApi
        if (sortMode != null && api != null) {
            api.playlistDetail(id, sortMode).data
        } else {
            v3.playlistDetail(id).data
        }
    }

    suspend fun createPlaylist(
        name: String,
        description: String? = null,
    ): Result<PlaylistInfoDto> = runCatching {
        val body = buildJsonObject {
            put("name", JsonPrimitive(name.trim()))
            description?.trim()?.takeIf { it.isNotEmpty() }?.let {
                put("description", JsonPrimitive(it))
            }
        }
        requireWriteApi().createPlaylist(body)
    }

    suspend fun updatePlaylist(
        id: Int,
        name: String? = null,
        description: String? = null,
        clearDescription: Boolean = false,
        visibility: String? = null,
        sortMode: String? = null,
    ): Result<PlaylistInfoDto> = runCatching {
        val body = buildJsonObject {
            name?.trim()?.takeIf { it.isNotEmpty() }?.let {
                put("name", JsonPrimitive(it))
            }
            if (clearDescription) {
                put("description", JsonNull)
            } else {
                description?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    put("description", JsonPrimitive(it))
                }
            }
            visibility?.let { put("visibility", JsonPrimitive(it)) }
            sortMode?.let { put("sort_mode", JsonPrimitive(it)) }
        }
        requireWriteApi().updatePlaylist(id, body)
    }

    suspend fun deletePlaylist(id: Int): Result<Unit> = runCatching {
        requireWriteApi().deletePlaylist(id)
    }

    suspend fun addSongs(
        id: Int,
        songIds: List<Int>,
    ): Result<PlaylistAddSongsResult> = runCatching {
        val ids = songIds.map { it }.filter { it > 0 }.distinct()
        require(ids.isNotEmpty()) { "没有要添加的歌曲" }
        requireWriteApi().addSongs(
            id,
            buildJsonObject {
                put("song_ids", kotlinx.serialization.json.JsonArray(ids.map { JsonPrimitive(it) }))
            },
        )
    }

    suspend fun removeSong(
        id: Int,
        songId: Int,
    ): Result<Unit> = runCatching {
        requireWriteApi().removeSong(id, songId)
    }

    suspend fun saveNote(
        id: Int,
        songId: Int,
        note: String?,
    ): Result<PlaylistNoteResult> = runCatching {
        requireWriteApi().saveNote(
            id,
            songId,
            buildJsonObject {
                val value = note?.trim()?.takeIf { it.isNotEmpty() }
                put("note", value?.let { JsonPrimitive(it) } ?: JsonNull)
            },
        )
    }

    suspend fun moveSong(
        id: Int,
        songId: Int,
        beforeSongId: Int?,
    ): Result<PlaylistMoveResult> = runCatching {
        requireWriteApi().moveSong(
            id,
            songId,
            buildJsonObject {
                put(
                    "before_song_id",
                    beforeSongId?.let { JsonPrimitive(it) } ?: JsonNull,
                )
            },
        )
    }

    suspend fun setCover(
        id: Int,
        bvid: String?,
    ): Result<PlaylistCoverResult> = runCatching {
        requireWriteApi().setCover(
            id,
            buildJsonObject {
                put("bvid", bvid?.let { JsonPrimitive(it) } ?: JsonNull)
            },
        )
    }

    private fun requireWriteApi(): PlaylistWriteApi =
        writeApi ?: throw IllegalStateException("歌单写入接口尚未接入")
}
