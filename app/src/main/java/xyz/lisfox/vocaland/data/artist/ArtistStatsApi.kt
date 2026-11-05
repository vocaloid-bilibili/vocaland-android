package xyz.lisfox.vocaland.data.artist

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.ArtistBriefDto
import xyz.lisfox.vocaland.data.model.DataEnvelope
import xyz.lisfox.vocaland.data.model.SongDto
import xyz.lisfox.vocaland.data.model.SynthesizerDto
import xyz.lisfox.vocaland.data.model.VocalistDto

@Serializable
data class VocalistStatDto(
    val vocalist: VocalistDto = VocalistDto(),
    val count: Int = 0,
)

@Serializable
data class ProducerStatDto(
    val producer: ArtistBriefDto = ArtistBriefDto(),
    val count: Int = 0,
)

@Serializable
data class SynthesizerStatDto(
    val synthesizer: SynthesizerDto? = null,
    val count: Int = 0,
)

@Serializable
data class VocalistStatListResponse(
    val data: List<VocalistStatDto> = emptyList(),
    val total: Int = 0,
)

interface ArtistStatsApi {
    @GET("{type}/{id}/stats/top-songs")
    suspend fun topSongs(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("limit") limit: Int = 12,
    ): DataEnvelope<List<SongDto>>

    @GET("{type}/{id}/stats/latest-songs")
    suspend fun latestSongs(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("limit") limit: Int = 12,
    ): DataEnvelope<List<SongDto>>

    @GET("producer/{id}/stats/vocalists")
    suspend fun producerVocalists(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 12,
    ): DataEnvelope<List<VocalistStatDto>>

    @GET("vocalist/{id}/stats/synthesizers")
    suspend fun vocalistSynthesizers(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 12,
    ): DataEnvelope<List<SynthesizerStatDto>>

    @GET("vocalist/{id}/stats/producers")
    suspend fun vocalistProducers(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 12,
    ): DataEnvelope<List<ProducerStatDto>>

    @GET("synthesizer/{id}/stats/vocalists")
    suspend fun synthesizerVocalists(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 12,
    ): VocalistStatListResponse

    @GET("synthesizer/{id}/stats/producers")
    suspend fun synthesizerProducers(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 12,
    ): DataEnvelope<List<ProducerStatDto>>

    @GET("uploader/{id}/stats/vocalists")
    suspend fun uploaderVocalists(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 12,
    ): DataEnvelope<List<VocalistStatDto>>

    @GET("uploader/{id}/stats/producers")
    suspend fun uploaderProducers(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 12,
    ): DataEnvelope<List<ProducerStatDto>>
}
