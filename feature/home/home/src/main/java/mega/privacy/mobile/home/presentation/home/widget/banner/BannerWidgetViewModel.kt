package mega.privacy.mobile.home.presentation.home.widget.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.banner.DismissBannerUseCase
import mega.privacy.android.domain.usecase.banner.GetPromoBannersUseCase
import mega.privacy.android.domain.usecase.billing.MonitorSubscriptionOfferBannerClosedUseCase
import mega.privacy.android.domain.usecase.billing.MonitorSubscriptionOfferUseCase
import mega.privacy.android.domain.usecase.billing.SetSubscriptionOfferBannerClosedUseCase
import mega.privacy.mobile.home.presentation.home.widget.banner.mapper.SubscriptionOfferBannerMapper
import mega.privacy.mobile.home.presentation.home.widget.banner.mapper.SubscriptionOfferBannerMapper.Companion.SUBSCRIPTION_OFFER_BANNER_ID
import mega.privacy.mobile.home.presentation.home.widget.banner.model.BannerUiState
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for banner widget
 */
@HiltViewModel
class BannerWidgetViewModel @Inject constructor(
    private val getPromoBannersUseCase: GetPromoBannersUseCase,
    private val dismissBannerUseCase: DismissBannerUseCase,
    private val monitorSubscriptionOfferUseCase: MonitorSubscriptionOfferUseCase,
    private val monitorSubscriptionOfferBannerClosedUseCase: MonitorSubscriptionOfferBannerClosedUseCase,
    private val setSubscriptionOfferBannerClosedUseCase: SetSubscriptionOfferBannerClosedUseCase,
    private val subscriptionOfferBannerMapper: SubscriptionOfferBannerMapper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BannerUiState())
    val uiState: StateFlow<BannerUiState> = _uiState.asStateFlow()

    init {
        loadBanners()
        monitorSubscriptionOfferBanner()
    }

    private fun loadBanners() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true)
            }
            val banners = runCatching { getPromoBannersUseCase() }
                .onFailure { Timber.e(it, "Failed to load banners") }
                .getOrDefault(emptyList())
            _uiState.update {
                it.copy(banners = banners, isLoading = false)
            }
        }
    }

    /**
     * Monitor the subscription offer banner. It is not delivered by the banners API, so it is built
     * locally from the subscription that currently carries an offer, and dropped again once the
     * account moves to a plan the campaign no longer targets.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorSubscriptionOfferBanner() {
        viewModelScope.launch {
            monitorSubscriptionOfferBannerClosedUseCase()
                .distinctUntilChanged()
                .flatMapLatest { isClosed ->
                    if (isClosed) {
                        flowOf(null)
                    } else {
                        monitorSubscriptionOfferUseCase().map { result ->
                            result
                                .onFailure { Timber.e(it, "Failed to load the offer banner") }
                                .getOrNull()
                        }
                    }
                }
                .map { offer ->
                    offer?.let {
                        subscriptionOfferBannerMapper(it.subscription, Locale.getDefault())
                    }
                }
                .catch { Timber.e(it, "Failed to monitor subscription offer banner") }
                .collect { offerBanner ->
                    _uiState.update { it.copy(offerBanner = offerBanner) }
                }
        }
    }

    /**
     * Dismiss a banner. The subscription offer banner only exists locally, so instead of calling the
     * banners API its dismissal is persisted per account, keeping it hidden on the next app launch.
     *
     * @param bannerId The ID of the banner to dismiss
     */
    fun dismissBanner(bannerId: Int) {
        if (bannerId == SUBSCRIPTION_OFFER_BANNER_ID) {
            viewModelScope.launch {
                runCatching {
                    setSubscriptionOfferBannerClosedUseCase()
                }.onFailure { exception ->
                    Timber.e(exception, "Failed to persist subscription offer banner dismissal")
                }
                _uiState.update { it.copy(offerBanner = null) }
            }
        } else {
            viewModelScope.launch {
                runCatching {
                    dismissBannerUseCase(bannerId)
                }.onSuccess {
                    removeBanner(bannerId)
                }.onFailure { exception ->
                    Timber.e(exception, "Failed to dismiss banner $bannerId")
                }
            }
        }
    }

    private fun removeBanner(bannerId: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                banners = currentState.banners.filter { it.id != bannerId }
            )
        }
    }
}
