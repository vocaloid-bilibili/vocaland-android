package xyz.lisfox.vocaland.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.lisfox.vocaland.data.model.EdBonusVoteDto
import xyz.lisfox.vocaland.data.model.EdCampaignDto
import xyz.lisfox.vocaland.data.model.EdCandidateDto
import xyz.lisfox.vocaland.data.model.EdCandidateListResponse
import xyz.lisfox.vocaland.data.model.EdCastVotesRequest
import xyz.lisfox.vocaland.data.model.EdEligibilityDto
import xyz.lisfox.vocaland.data.model.EdResultDto
import xyz.lisfox.vocaland.data.model.EdSubmitNominationRequest
import xyz.lisfox.vocaland.data.model.EdSubmitNominationResponse
import xyz.lisfox.vocaland.data.model.EdVoteDto
import xyz.lisfox.vocaland.data.model.EdVotingCandidatesResponse
import xyz.lisfox.vocaland.data.model.SearchEntityDto
import xyz.lisfox.vocaland.data.model.TrendingCampaignDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateDto
import xyz.lisfox.vocaland.data.model.TrendingCandidateListResponse
import xyz.lisfox.vocaland.data.model.TrendingCastVotesRequest
import xyz.lisfox.vocaland.data.model.TrendingMyVotesDto
import xyz.lisfox.vocaland.data.model.TrendingNominateRequest
import xyz.lisfox.vocaland.data.model.TrendingResultRowDto
import xyz.lisfox.vocaland.data.model.TrendingTallyDto
import xyz.lisfox.vocaland.data.model.WalletDto
import xyz.lisfox.vocaland.data.remote.CommunityApi
import xyz.lisfox.vocaland.data.remote.VocaLandApi

class VotingRepository(
    private val api: VocaLandApi,
    private val v2: CommunityApi,
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

    suspend fun loadEdNomination(campaignId: Int): Result<EdNominationData> = runCatching {
        coroutineScope {
            val candidates = async { api.edCandidates(campaignId) }
            val myNomination = async { api.edMyNomination(campaignId) }
            EdNominationData(
                candidates = candidates.await(),
                myNomination = myNomination.await(),
            )
        }
    }

    suspend fun checkEdEligibility(campaignId: Int, bvid: String): Result<EdEligibilityDto> =
        runCatching { api.edEligibility(campaignId, bvid) }

    suspend fun submitEdNomination(
        campaignId: Int,
        bvid: String,
        eligibility: EdEligibilityDto,
        name: String,
        vocalist: String,
        producer: String,
    ): Result<EdSubmitNominationResponse> = runCatching {
        api.submitEdNomination(
            EdSubmitNominationRequest(
                campaignId = campaignId,
                bvid = bvid,
                songId = eligibility.songId,
                name = if (eligibility.songId == null) name else null,
                vocalist = if (eligibility.songId == null) vocalist else null,
                producer = if (eligibility.songId == null) producer else null,
            ),
        )
    }

    suspend fun withdrawEdNomination(candidateId: Int): Result<Unit> = runCatching {
        api.withdrawEdNomination(candidateId)
    }

    suspend fun loadEdResults(campaignId: Int): Result<EdResultsData> = runCatching {
        coroutineScope {
            val results = async { api.edResults(campaignId).data }
            val ranking = async { api.edFullRanking(campaignId).data }
            EdResultsData(
                results = results.await(),
                ranking = ranking.await(),
            )
        }
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

    suspend fun loadTrendingNominations(
        campaignId: Int,
        includeMine: Boolean,
    ): Result<TrendingNominationData> = runCatching {
        coroutineScope {
            val candidates = async { api.trendingCandidates(campaignId) }
            val mine = if (includeMine) {
                async { api.trendingMyNominations(campaignId).data }
            } else {
                null
            }
            TrendingNominationData(
                candidates = candidates.await(),
                myNominations = mine?.await().orEmpty(),
            )
        }
    }

    suspend fun submitTrendingNomination(
        campaignId: Int,
        entityKind: String,
        entityId: Int,
    ): Result<TrendingCandidateDto> = runCatching {
        api.submitTrendingNomination(
            campaignId = campaignId,
            request = TrendingNominateRequest(
                entityKind = entityKind,
                entityId = entityId,
            ),
        )
    }

    suspend fun withdrawTrendingNomination(candidateId: Int): Result<Unit> = runCatching {
        api.withdrawTrendingNomination(candidateId)
    }

    suspend fun searchNominationEntities(
        entityKind: String,
        query: String,
    ): Result<List<SearchEntityDto>> = runCatching {
        api.search(
            type = entityKind,
            keyword = query,
            pageSize = 8,
        ).data
    }

    suspend fun loadTrendingTally(campaignId: Int): Result<TrendingTallyDto> = runCatching {
        api.trendingTally(campaignId)
    }

    suspend fun loadTrendingResults(campaignId: Int): Result<List<TrendingResultRowDto>> =
        runCatching { api.trendingResults(campaignId).data }

    suspend fun loadWallet(): Result<WalletDto> = runCatching { v2.myWallet() }

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

data class EdNominationData(
    val candidates: EdCandidateListResponse,
    val myNomination: EdCandidateDto?,
)

data class EdVotingData(
    val candidates: EdVotingCandidatesResponse,
    val myVotes: List<EdVoteDto>,
)

data class EdResultsData(
    val results: List<EdResultDto>,
    val ranking: List<EdResultDto>,
)

data class TrendingNominationData(
    val candidates: TrendingCandidateListResponse,
    val myNominations: List<TrendingCandidateDto>,
)

data class TrendingVotingData(
    val candidates: TrendingCandidateListResponse,
    val tally: TrendingTallyDto,
    val myVotes: List<Int>,
)
