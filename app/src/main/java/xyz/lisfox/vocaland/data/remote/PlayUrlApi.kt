package xyz.lisfox.vocaland.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import xyz.lisfox.vocaland.data.model.PlayUrlRequest
import xyz.lisfox.vocaland.data.model.PlayUrlResponse

interface PlayUrlApi {
    @POST("api/playurl")
    suspend fun resolve(@Body request: PlayUrlRequest): PlayUrlResponse
}
