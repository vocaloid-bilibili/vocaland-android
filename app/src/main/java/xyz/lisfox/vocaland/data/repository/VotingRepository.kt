package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.model.EdBonusVoteDto
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.model.EdCastVotesRequest
import xyz.lisfox.vocaland.data.model.EdVoteDto
import xyz.lisfox.vocaland.data.model.EdVotingCandidatesResponse
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateListResponse
import xyz.lisfox.vocaland.data.model.TrendingCastVotesRequest
import xyz.lisfox.vocaland.data.model.TrendingMyVotesDto
import xyz.lisfox.vocaland.data.model.TrendingTallyDto
import xyz.lisfox.vocaland.data.remote.VocaLandApi

class VotingRepository(
    private val api: VocaLandApi,
) {
    suspend fun loadHub(): Result<VotingHubData> = runCatching {
        coroutineScope {
            val ed = async { api.edCampaigns().data }
            val trending = async { api.trendingCampaigns().data }
            VotingHubData(ed = ed.await(), trending = trending.await())
        }
    }

    suspend fun loadEdCampaign(campaignId: Int): Result<EdCampaignDto> = runCatching {
        api.edCampaign(campaignId)
    }

    suspend fun loadEdVoting(campaignId: Int): Result<EdVotingData> = runCatching {
        coroutineScope {
            val candidates = async { api.edVotingCandidates(campaignId) }
            val myVotes = async { api.edMyVotes(campaignId) }
            EdVotingData(
                candidates = candidates.await(),
                myVotes = myVotes.await(),
            )
        }
    }

    suspend fun castEdVotes(
        campaignId: Int,
        positiveCandidateIds: Set<Int>,
        negativeCandidateIds: Set<Int>,
        bonusVotes: List<EdBonusVoteDto>,
    ): Result<List<EdVoteDto>> = runCatching {
        api.castEdVotes(
            EdCastVotesRequest(
                campaignId = campaignId,
                positiveCandidateIds = positiveCandidateIds.toList(),
                negativeCandidateIds = negativeCandidateIds.toList(),
                bonusVotes = bonusVotes,
            ),
        )
    }

    suspend fun loadTrendingCampaign(campaignId: Int): Result<TrendingCampaignDto> =
        runCatching { api.trendingCampaign(campaignId) }

    suspend fun loadTrendingVoting(
        campaignId: Int,
        includeMyVotes: Boolean,
    ): Result<TrendingVotingData> = runCatching {
        coroutineScope {
            val candidates = async { api.trendingCandidates(campaignId) }
            val tally = async { api.trendingTally(campaignId) }
            val myVotes = if (includeMyVotes) {
                async { api.trendingMyVotes(campaignId).data }
            } else {
                null
            }
            TrendingVotingData(
                candidates = candidates.await(),
                tally = tally.await(),
                myVotes = myVotes?.await().orEmpty(),
            )
        }
    }

    suspend fun loadTrendingTally(campaignId: Int): Result<TrendingTallyDto> = runCatching {
        api.trendingTally(campaignId)
    }

    suspend fun castTrendingVotes(
        campaignId: Int,
        candidateIds: Set<Int>,
    ): Result<TrendingMyVotesDto> = runCatching {
        api.castTrendingVotes(
            campaignId = campaignId,
            request = TrendingCastVotesRequest(candidateIds = candidateIds.toList()),
        )
    }
}

data class VotingHubData(
    val ed: List<EdCampaignDto>,
    val trending: List<TrendingCampaignDto>,
)

data class EdVotingData(
    val candidates: EdVotingCandidatesResponse,
    val myVotes: List<EdVoteDto>,
)

data class TrendingVotingData(
    val candidates: TrendingCandidateListResponse,
    val tally: TrendingTallyDto,
    val myVotes: List<Int>,
)
