package xyz.lisfox.vocaland.data.remote

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
    namingStrategy = JsonNamingStrategy.SnakeCase
}
