package xyz.lisfox.vocaland.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val body = try {
            response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        parseDetail(body) ?: when (code()) {
            401 -> "登录已过期，请重新登录"
            403 -> "没有权限执行该操作"
            404 -> "请求的内容不存在"
            else -> "请求失败（${code()}）"
        }
    }

    is IOException -> "网络连接失败，请检查网络后重试"
    else -> message?.takeIf { it.isNotBlank() } ?: "发生了未知错误"
}

private fun parseDetail(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        when (val detail = AppJson.parseToJsonElement(body).jsonObject["detail"]) {
            is JsonPrimitive -> detail.content.takeIf { it.isNotBlank() }
            is JsonObject -> detail["message"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
