package xyz.lisfox.vocaland.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.lisfox.vocaland.data.remote.toUserMessage
import xyz.lisfox.vocaland.data.shop.ShopItemDto
import xyz.lisfox.vocaland.data.shop.ShopRepository
import xyz.lisfox.vocaland.data.shop.WalletDto

data class ShopUiState(
    val items: List<ShopItemDto> = emptyList(),
    val wallet: WalletDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val walletLoading: Boolean = false,
    val walletError: String? = null,
    val purchasing: Boolean = false,
    val purchaseError: String? = null,
    val notice: String? = null,
)

class ShopViewModel(
    private val repository: ShopRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ShopUiState())
    val state: StateFlow<ShopUiState> = _state.asStateFlow()

    fun load(includeWallet: Boolean) {
        _state.update {
            it.copy(
                loading = true,
                error = null,
                walletLoading = includeWallet,
                walletError = null,
            )
        }
        viewModelScope.launch {
            coroutineScope {
                val itemsDeferred = async { repository.loadItems() }
                val walletDeferred = if (includeWallet) {
                    async { repository.loadWallet() }
                } else {
                    null
                }

                val itemsResult = itemsDeferred.await()
                val walletResult = walletDeferred?.await()

                _state.update {
                    it.copy(
                        items = itemsResult.getOrDefault(it.items),
                        loading = false,
                        error = itemsResult.exceptionOrNull()?.toUserMessage(),
                        wallet = walletResult?.getOrNull() ?: it.wallet,
                        walletLoading = false,
                        walletError = walletResult?.exceptionOrNull()?.toUserMessage(),
                    )
                }
            }
        }
    }

    fun purchase(item: ShopItemDto, quantity: Int) {
        if (_state.value.purchasing) return
        _state.update { it.copy(purchasing = true, purchaseError = null, notice = null) }
        viewModelScope.launch {
            repository.purchase(item.id, quantity)
                .onSuccess { result ->
                    val delivered = if (item.category == "ticket") {
                        (item.ticketCount ?: 0) * quantity
                    } else {
                        0
                    }
                    val message = if (delivered > 0) {
                        "已购买「${item.name}」×$quantity，到账 $delivered 张，剩余 ${result.vCoinsRemaining} V币"
                    } else {
                        "已购买「${item.name}」×$quantity，剩余 ${result.vCoinsRemaining} V币"
                    }
                    val refreshedWallet = repository.loadWallet().getOrNull()
                    _state.update {
                        it.copy(
                            purchasing = false,
                            notice = message,
                            wallet = refreshedWallet
                                ?: it.wallet?.copy(vCoins = result.vCoinsRemaining),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            purchasing = false,
                            purchaseError = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null, purchaseError = null) }
    }
}
