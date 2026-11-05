package xyz.lisfox.vocaland.data.collection

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.lisfox.vocaland.data.model.DataEnvelope

interface CollectionApi {
    @GET("collection")
    suspend fun collections(): DataEnvelope<List<CollectionDto>>

    @GET("collection/{id}")
    suspend fun collectionDetail(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 48,
        @Query("order_by") orderBy: String = "lit_count",
        @Query("order") order: String = "desc",
    ): CollectionDetailEnvelope

    @GET("collection/{kind}/{sourceId}/{entityType}")
    suspend fun derivedCollectionDetail(
        @Path("kind") kind: String,
        @Path("sourceId") sourceId: Int,
        @Path("entityType") entityType: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 48,
        @Query("order_by") orderBy: String = "lit_count",
        @Query("order") order: String = "desc",
    ): DerivedCollectionDetailEnvelope
}
