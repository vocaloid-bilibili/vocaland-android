package xyz.lisfox.vocaland.data.stat

import retrofit2.http.GET
import retrofit2.http.QueryMap
import xyz.lisfox.vocaland.data.model.SongListResponse

/**
 * 统计 / 自定义排行用到的歌曲列表接口。
 *
 * 与 VocaLandApi 的 songsList 区别：这里支持全部筛选维度（@QueryMap 透传），
 * 参数由 StatFilterCodec 统一产出。VocaLandApi 是公共 API，本模块不修改它。
 */
interface StatApi {
    @GET("song/list")
    suspend fun songsList(@QueryMap params: Map<String, String>): SongListResponse
}
