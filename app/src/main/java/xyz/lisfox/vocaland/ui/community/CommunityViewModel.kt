package xyz.lisfox.vocaland.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.community.CommunityCommentDto
import xyz.lisfox.vocaland.data.community.CommunityRepository
import xyz.lisfox.vocaland.data.model.RatingDto
import xyz.lisfox.vocaland.data.model.RatingSummaryDto
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.session.AuthSession

internal const val COMMUNITY_RATING_MIN = 0
internal const val COMMUNITY_RATING_MAX = 10
internal const val COMMUNITY_RATING_COMMENT_MAX = 1000
internal const val COMMUNITY_COMMENT_MAX = 2000
internal val COMMUNITY_SCORE_RANGE = COMMUNITY_RATING_MIN..COMMUNITY_RATING_MAX

data class RatingDraft(
    val score: Int = 0,
    val comment: String = "",
    val isAnonymous: Boolean = false,
) {
    companion object {
        fun from(rating: RatingDto?): RatingDraft = if (rating == null) {
            RatingDraft()
        } else {
            RatingDraft(
                score = rating.score,
                comment = rating.comment.orEmpty(),
                isAnonymous = rating.isAnonymous,
            )
        }
    }
}

data class CommunityUiState(
    val targetKey: String = "",
    val isLoggedIn: Boolean = false,
    val ratingSummary: RatingSummaryDto? = null,
    val ratings: List<RatingDto> = emptyList(),
    val ratingsPage: Int = 1,
    val ratingsTotalPages: Int = 1,
    val ratingsTotal: Int = 0,
    val ratingsLoading: Boolean = true,
    val ratingsError: String? = null,
    val myRating: RatingDto? = null,
    val myRatingLoading: Boolean = false,
    val ratingDraft: RatingDraft = RatingDraft(),
    val ratingSubmitting: Boolean = false,
    val ratingError: String? = null,
    val ratingNotice: String? = null,
    val comments: List<CommunityCommentDto> = emptyList(),
    val commentsPage: Int = 1,
    val commentsTotal: Int = 0,
    val commentsTotalPages: Int = 1,
    val commentsLoading: Boolean = false,
    val commentsError: String? = null,
    val commentPosting: Boolean = false,
    val commentActionError: String? = null,
    val commentResetKey: Int = 0,
    val likePendingIds: Set<Int> = emptySet(),
    val deletingCommentIds: Set<Int> = emptySet(),
)

class CommunityViewModel(
    private val repository: CommunityRepository,
    private val session: AuthSession?,
    private val targetKey: String,
    private val includeRatings: Boolean,
    private val includeComments: Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(CommunityUiState(targetKey = targetKey))
    val state: StateFlow<CommunityUiState> = _state.asStateFlow()

    /** 评论操作成功后的一次性提示，由界面交给全局 Snackbar 展示。 */
    private val _commentMessages = Channel<String>(Channel.BUFFERED)
    val commentMessages: Flow<String> = _commentMessages.receiveAsFlow()

    private var summaryJob: Job? = null
    private var ratingsJob: Job? = null
    private var myRatingJob: Job? = null
    private var commentsJob: Job? = null
    private var currentViewerId: Int? = null

    init {
        observeSession()
        if (includeRatings) {
            loadRatingSummary()
            loadRatings(1)
        }
    }

    fun retryRatings() {
        if (!includeRatings) return
        if (_state.value.ratingSummary == null) {
            loadRatingSummary()
        }
        loadRatings(_state.value.ratingsPage)
    }

    fun previousRatingsPage() {
        val current = _state.value
        if (current.ratingsPage <= 1 || current.ratingsLoading) return
        loadRatings(current.ratingsPage - 1)
    }

    fun nextRatingsPage() {
        val current = _state.value
        if (current.ratingsPage >= current.ratingsTotalPages || current.ratingsLoading) return
        loadRatings(current.ratingsPage + 1)
    }

    fun retryComments() {
        if (!includeComments || !_state.value.isLoggedIn) return
        loadComments(_state.value.commentsPage)
    }

    fun previousCommentsPage() {
        val current = _state.value
        if (current.commentsPage <= 1 || current.commentsLoading) return
        loadComments(current.commentsPage - 1)
    }

    fun nextCommentsPage() {
        val current = _state.value
        if (current.commentsPage >= current.commentsTotalPages || current.commentsLoading) return
        loadComments(current.commentsPage + 1)
    }

    fun selectRatingScore(score: Int) {
        if (score !in COMMUNITY_SCORE_RANGE) return
        _state.update {
            it.copy(
                ratingDraft = it.ratingDraft.copy(score = score),
                ratingError = null,
                ratingNotice = null,
            )
        }
    }

    fun updateRatingComment(text: String) {
        _state.update {
            it.copy(
                ratingDraft = it.ratingDraft.copy(comment = text.take(COMMUNITY_RATING_COMMENT_MAX)),
                ratingError = null,
                ratingNotice = null,
            )
        }
    }

    fun toggleRatingAnonymous() {
        _state.update {
            it.copy(
                ratingDraft = it.ratingDraft.copy(isAnonymous = !it.ratingDraft.isAnonymous),
                ratingError = null,
                ratingNotice = null,
            )
        }
    }

    fun submitRating() {
        val current = _state.value
        if (!current.isLoggedIn || current.ratingSubmitting) return
        val draft = current.ratingDraft
        if (draft.score !in COMMUNITY_SCORE_RANGE) {
            _state.update { it.copy(ratingError = "请先选择 0-10 分") }
            return
        }

        val hadRating = current.myRating != null
        _state.update { it.copy(ratingSubmitting = true, ratingError = null, ratingNotice = null) }
        viewModelScope.launch {
            repository.submitRating(
                targetKey = targetKey,
                score = draft.score,
                comment = draft.comment,
                isAnonymous = draft.isAnonymous,
            )
                .onSuccess { rating ->
                    _state.update {
                        it.copy(
                            myRating = rating,
                            ratingDraft = RatingDraft.from(rating),
                            ratingSubmitting = false,
                            ratingError = null,
                            ratingNotice = if (hadRating) "评分已更新" else "评分已提交",
                        )
                    }
                    loadRatingSummary()
                    loadRatings(1)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            ratingSubmitting = false,
                            ratingError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun withdrawRating() {
        val current = _state.value
        if (!current.isLoggedIn || current.ratingSubmitting || current.myRating == null) return

        _state.update { it.copy(ratingSubmitting = true, ratingError = null, ratingNotice = null) }
        viewModelScope.launch {
            repository.withdrawRating(targetKey)
                .onSuccess {
                    _state.update {
                        it.copy(
                            myRating = null,
                            ratingDraft = RatingDraft(),
                            ratingSubmitting = false,
                            ratingError = null,
                            ratingNotice = "评分已撤回",
                        )
                    }
                    loadRatingSummary()
                    loadRatings(1)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            ratingSubmitting = false,
                            ratingError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun postComment(
        content: String,
        parentId: Int? = null,
        replyToId: Int? = null,
    ) {
        val trimmed = content.trim()
        val current = _state.value
        if (trimmed.isEmpty() || current.commentPosting || !current.isLoggedIn) return

        _state.update { it.copy(commentPosting = true, commentActionError = null) }
        viewModelScope.launch {
            repository.postComment(
                targetKey = targetKey,
                content = trimmed,
                parentId = parentId,
                replyToId = replyToId,
            )
                .onSuccess {
                    _state.update {
                        it.copy(
                            commentPosting = false,
                            commentActionError = null,
                            commentResetKey = it.commentResetKey + 1,
                        )
                    }
                    _commentMessages.trySend(if (parentId == null) "评论已发布" else "回复已发布")
                    loadComments(1)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            commentPosting = false,
                            commentActionError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun deleteComment(commentId: Int) {
        val current = _state.value
        if (!current.isLoggedIn || commentId in current.deletingCommentIds) return

        _state.update {
            it.copy(
                deletingCommentIds = it.deletingCommentIds + commentId,
                commentActionError = null,
            )
        }
        viewModelScope.launch {
            repository.deleteComment(commentId)
                .onSuccess {
                    _state.update {
                        it.copy(deletingCommentIds = it.deletingCommentIds - commentId)
                    }
                    _commentMessages.trySend("评论已删除")
                    loadComments(_state.value.commentsPage)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            deletingCommentIds = it.deletingCommentIds - commentId,
                            commentActionError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun toggleCommentLike(commentId: Int) {
        val current = _state.value
        if (!current.isLoggedIn || commentId in current.likePendingIds) return

        _state.update { it.copy(likePendingIds = it.likePendingIds + commentId) }
        viewModelScope.launch {
            repository.toggleCommentLike(commentId)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            comments = updateLike(
                                comments = it.comments,
                                commentId = commentId,
                                liked = result.liked,
                                likeCount = result.likeCount,
                            ),
                            likePendingIds = it.likePendingIds - commentId,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            likePendingIds = it.likePendingIds - commentId,
                            commentActionError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun observeSession() {
        val currentSession = session
        if (currentSession == null) {
            _state.update { it.copy(isLoggedIn = false) }
            return
        }

        viewModelScope.launch {
            currentSession.state
                .map { it.user?.id to it.isLoggedIn }
                .distinctUntilChanged()
                .collect { (viewerId, loggedIn) ->
                    handleAuthChange(viewerId, loggedIn)
                }
        }
    }

    private fun handleAuthChange(viewerId: Int?, loggedIn: Boolean) {
        val accountChanged = viewerId != currentViewerId
        currentViewerId = viewerId
        val keepViewerData = loggedIn && !accountChanged
        val hadRating = if (accountChanged) null else _state.value.myRating
        _state.update {
            it.copy(
                isLoggedIn = loggedIn,
                myRating = if (keepViewerData) it.myRating else null,
                myRatingLoading = false,
                ratingDraft = if (keepViewerData) it.ratingDraft else RatingDraft(),
                ratingError = null,
                ratingNotice = null,
                commentActionError = null,
            )
        }

        if (loggedIn) {
            if (includeRatings && hadRating == null) {
                loadMyRating()
            }
            if (includeComments) {
                loadComments(1)
            }
        } else {
            myRatingJob?.cancel()
            commentsJob?.cancel()
            _state.update {
                it.copy(
                    comments = emptyList(),
                    commentsPage = 1,
                    commentsTotal = 0,
                    commentsTotalPages = 1,
                    commentsLoading = false,
                    commentsError = null,
                    likePendingIds = emptySet(),
                    deletingCommentIds = emptySet(),
                )
            }
        }
    }

    private fun loadRatingSummary() {
        if (!includeRatings) return
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            val result = repository.loadRatingSummary(targetKey)
            if (!isActive) return@launch
            result.onSuccess { summary ->
                _state.update { it.copy(ratingSummary = summary) }
            }
        }
    }

    private fun loadRatings(page: Int) {
        if (!includeRatings) return
        ratingsJob?.cancel()
        _state.update { it.copy(ratingsLoading = true, ratingsError = null) }

        ratingsJob = viewModelScope.launch {
            val result = repository.loadRatings(targetKey, page)
            if (!isActive) return@launch
            result
                .onSuccess { ratingPage ->
                    _state.update {
                        it.copy(
                            ratings = ratingPage.items,
                            ratingsPage = ratingPage.page,
                            ratingsTotalPages = ratingPage.totalPages,
                            ratingsTotal = ratingPage.total,
                            ratingsLoading = false,
                            ratingsError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            ratingsLoading = false,
                            ratingsError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun loadMyRating() {
        if (!includeRatings || !_state.value.isLoggedIn) return
        myRatingJob?.cancel()
        _state.update { it.copy(myRatingLoading = true) }

        myRatingJob = viewModelScope.launch {
            val result = repository.loadMyRating(targetKey)
            if (!isActive) return@launch
            result
                .onSuccess { rating ->
                    _state.update {
                        it.copy(
                            myRating = rating,
                            myRatingLoading = false,
                            ratingDraft = RatingDraft.from(rating),
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(myRatingLoading = false) }
                }
        }
    }

    private fun loadComments(page: Int) {
        if (!includeComments || !_state.value.isLoggedIn) return
        commentsJob?.cancel()
        _state.update { it.copy(commentsLoading = true, commentsError = null) }

        commentsJob = viewModelScope.launch {
            val result = repository.loadComments(targetKey, page)
            if (!isActive) return@launch
            result
                .onSuccess { commentPage ->
                    _state.update {
                        it.copy(
                            comments = commentPage.items,
                            commentsPage = commentPage.page,
                            commentsTotal = commentPage.total,
                            commentsTotalPages = commentPage.totalPages,
                            commentsLoading = false,
                            commentsError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            commentsLoading = false,
                            commentsError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun updateLike(
        comments: List<CommunityCommentDto>,
        commentId: Int,
        liked: Boolean,
        likeCount: Int,
    ): List<CommunityCommentDto> = comments.map { comment ->
        if (comment.id == commentId) {
            comment.copy(isLiked = liked, likeCount = likeCount)
        } else {
            comment.copy(
                replies = updateLike(comment.replies, commentId, liked, likeCount),
            )
        }
    }

}
