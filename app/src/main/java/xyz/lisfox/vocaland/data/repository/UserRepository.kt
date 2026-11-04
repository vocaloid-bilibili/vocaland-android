package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import xyz.lisfox.vocaland.data.model.PublicUserDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.UserLitStatsDto
import xyz.lisfox.vocaland.data.remote.AppJson
import xyz.lisfox.vocaland.data.remote.CommunityApi
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class UserPageContent(
    val user: PublicUserDto,
    val litStats: UserLitStatsDto?,
    val ratings: List<RatingDto>,
    val ratingsTotal: Int,
)

class UserRepository(
    private val v3: VocaLandApi,
    private val v2: CommunityApi,
) {
    suspend fun load(userId: Int): Result<UserPageContent> = runCatching {
        coroutineScope {
            val user = async {
                parsePublicUser(v2.publicUser(userId))
                    ?: throw IllegalStateException("用户不存在")
            }
            val litStats = async {
                runCatching { v3.userLitStats(userId) }.getOrNull()
            }
            val ratings = async {
                runCatching { v2.userRatings(userId) }.getOrNull()
            }

            val ratingResponse = ratings.await()
            UserPageContent(
                user = user.await(),
                litStats = litStats.await(),
                ratings = ratingResponse?.data.orEmpty(),
                ratingsTotal = ratingResponse?.total ?: 0,
            )
        }
    }

    private fun parsePublicUser(element: JsonElement): PublicUserDto? {
        val objectValue = element as? JsonObject ?: return null
        val payload = objectValue["data"] as? JsonObject ?: objectValue
        return runCatching {
            AppJson.decodeFromJsonElement(PublicUserDto.serializer(), payload)
        }.getOrNull()
    }
}
