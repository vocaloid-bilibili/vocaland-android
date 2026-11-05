package xyz.lisfox.vocaland.data.customranking

import retrofit2.http.Body
import retrofit2.http.POST

interface CustomRankingApi {
    @POST("ranking/custom")
    suspend fun customRanking(
        @Body request: CustomRankingRequestDto,
    ): CustomRankingResponseDto
}
