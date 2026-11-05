package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import xyz.lisfox.vocaland.data.community.CommunityExtrasApi
import xyz.lisfox.vocaland.data.community.CommunityRepository
import xyz.lisfox.vocaland.data.model.CheckinResultDto
import xyz.lisfox.vocaland.data.model.LevelInfoDto
import xyz.lisfox.vocaland.data.model.PublicProfileResponseDto
import xyz.lisfox.vocaland.data.model.PublicUserDto
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.UserLitStatsDto
import xyz.lisfox.vocaland.data.model.WalletDto
import xyz.lisfox.vocaland.data.remote.AppJson
import xyz.lisfox.vocaland.data.remote.CommunityApi
import xyz.lisfox.vocaland.data.remote.VocaLandApi

data class UserPageContent(
    val user: PublicUserDto,
    val litStats: UserLitStatsDto?,
    val ratings: List<RatingDto>,
    val ratingsTotal: Int,
    val ratingsTotalPages: Int,
)

data class UserRatingPage(
    val items: List<RatingDto>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
)

class UserRepository(
    private val v3: VocaLandApi,
    private val v2: CommunityApi,
    extras: CommunityExtrasApi? = null,
) {
    val community: CommunityRepository? = extras?.let { CommunityRepository(v2, it) }

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
            val ratingItems = ratingResponse?.data.orEmpty()
            val ratingTotal = ratingResponse?.total ?: 0
            UserPageContent(
                user = user.await(),
                litStats = litStats.await(),
                ratings = ratingItems,
                ratingsTotal = ratingTotal,
                ratingsTotalPages = pagesFor(
                    ratingTotal.coerceAtLeast(ratingItems.size),
                    USER_RATINGS_PAGE_SIZE,
                ),
            )
        }
    }

    suspend fun loadRatings(userId: Int, page: Int): Result<UserRatingPage> = runCatching {
        val response = v2.userRatings(
            id = userId,
            page = page,
            pageSize = USER_RATINGS_PAGE_SIZE,
        )
        val total = response.total.coerceAtLeast(response.data.size)
        UserRatingPage(
            items = response.data,
            total = total,
            page = page,
            totalPages = pagesFor(total, USER_RATINGS_PAGE_SIZE),
        )
    }

    suspend fun loadProfile(userId: Int): Result<PublicProfileResponseDto?> =
        community?.loadProfile(userId)
            ?: Result.success<PublicProfileResponseDto?>(null)

    suspend fun loadWallet(): Result<WalletDto> =
        community?.loadWallet() ?: Result.failure(IllegalStateException("社区接口未接入"))

    suspend fun loadLevel(): Result<LevelInfoDto> =
        community?.loadLevel() ?: Result.failure(IllegalStateException("社区接口未接入"))

    suspend fun checkin(): Result<CheckinResultDto> =
        community?.checkin() ?: Result.failure(IllegalStateException("社区接口未接入"))

    private fun pagesFor(total: Int, pageSize: Int): Int =
        maxOf(1, kotlin.math.ceil(total.toDouble() / pageSize).toInt())

    private fun parsePublicUser(element: JsonElement): PublicUserDto? {
        val objectValue = element as? JsonObject ?: return null
        val payload = objectValue["data"] as? JsonObject ?: objectValue
        return runCatching {
            AppJson.decodeFromJsonElement(PublicUserDto.serializer(), payload)
        }.getOrNull()
    }

    private companion object {
        const val USER_RATINGS_PAGE_SIZE = 20
    }
}
