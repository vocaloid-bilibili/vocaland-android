package xyz.lisfox.vocaland.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.account.AccountRepository
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.shop.BackfillInfoDto
import xyz.lisfox.vocaland.data.shop.LevelInfoDto
import xyz.lisfox.vocaland.data.shop.ShopRepository
import xyz.lisfox.vocaland.data.shop.WalletDto

data class WalletUiState(
    val wallet: WalletDto? = null,
    val level: LevelInfoDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val checkingIn: Boolean = false,
    val backfillInfo: BackfillInfoDto? = null,
    val backfillLoading: Boolean = false,
    val backfillBusy: Boolean = false,
    val sponsorClaiming: Boolean = false,
    val sponsorMessage: String? = null,
    val sponsorError: String? = null,
)

class WalletViewModel(
    private val shopRepository: ShopRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WalletUiState())
    val state: StateFlow<WalletUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                val walletDeferred = async { shopRepository.loadWallet() }
                val levelDeferred = async { shopRepository.loadLevel() }
                val walletResult = walletDeferred.await()
                val levelResult = levelDeferred.await()
                _state.update {
                    it.copy(
                        wallet = walletResult.getOrNull() ?: it.wallet,
                        level = levelResult.getOrNull() ?: it.level,
                        loading = false,
                        error = walletResult.exceptionOrNull()?.toUserMessage()
                            ?: levelResult.exceptionOrNull()?.toUserMessage(),
                    )
                }
            }
        }
    }

    fun checkin() {
        if (_state.value.checkingIn) return
        _state.update { it.copy(checkingIn = true, notice = null) }
        viewModelScope.launch {
            shopRepository.checkin()
                .onSuccess { result ->
                    val message = describeCheckin(result)
                    val wallet = shopRepository.loadWallet().getOrNull()
                    val level = shopRepository.loadLevel().getOrNull()
                    _state.update {
                        it.copy(
                            checkingIn = false,
                            notice = message,
                            wallet = wallet ?: it.wallet,
                            level = level ?: it.level,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            checkingIn = false,
                            notice = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun loadBackfillInfo() {
        if (_state.value.backfillLoading) return
        _state.update { it.copy(backfillLoading = true) }
        viewModelScope.launch {
            shopRepository.loadBackfillInfo()
                .onSuccess { info ->
                    _state.update {
                        it.copy(
                            backfillInfo = info,
                            backfillLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            backfillLoading = false,
                            notice = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun backfill(date: String) {
        if (_state.value.backfillBusy) return
        _state.update { it.copy(backfillBusy = true) }
        viewModelScope.launch {
            shopRepository.backfill(date)
                .onSuccess { result ->
                    if (!result.success) {
                        _state.update {
                            it.copy(
                                backfillBusy = false,
                                notice = result.error ?: "补签失败",
                            )
                        }
                        return@onSuccess
                    }
                    val message = "补签 ${result.date ?: date} 成功 +${result.expGained} 经验"
                    val wallet = shopRepository.loadWallet().getOrNull()
                    val level = shopRepository.loadLevel().getOrNull()
                    val info = shopRepository.loadBackfillInfo().getOrNull()
                    _state.update {
                        it.copy(
                            backfillBusy = false,
                            notice = message,
                            wallet = wallet ?: it.wallet,
                            level = level ?: it.level,
                            backfillInfo = info ?: it.backfillInfo,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            backfillBusy = false,
                            notice = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun claimSponsor(orderId: String) {
        if (_state.value.sponsorClaiming) return
        _state.update {
            it.copy(
                sponsorClaiming = true,
                sponsorMessage = null,
                sponsorError = null,
            )
        }
        viewModelScope.launch {
            accountRepository.claimSponsorOrder(orderId)
                .onSuccess { result ->
                    accountRepository.loadMe()
                    _state.update {
                        it.copy(
                            sponsorClaiming = false,
                            sponsorMessage = "兑换成功，增加 ${result.durationDays} 天赞助时长",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            sponsorClaiming = false,
                            sponsorError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }
}
