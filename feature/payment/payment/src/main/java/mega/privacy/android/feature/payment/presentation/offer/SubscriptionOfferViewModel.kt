package mega.privacy.android.feature.payment.presentation.offer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.usecase.billing.MonitorSubscriptionOfferUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the subscription offer landing screen. Loads the cheapest higher-tier plan that
 * carries an active mobile offer and exposes it as a [mega.privacy.android.feature.payment.model.LocalisedSubscription]
 * on the billing period the offer applies to, flagging whether the campaign discounts other plans too.
 *
 * The offer is monitored rather than loaded once, so buying a plan elsewhere in the app clears the
 * offer and closes this screen through [SubscriptionOfferState.offerSubscription] going null.
 */
@HiltViewModel
class SubscriptionOfferViewModel @Inject constructor(
    private val monitorSubscriptionOfferUseCase: MonitorSubscriptionOfferUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionOfferState())

    /**
     * The current UI state.
     */
    val state = _state.asStateFlow()

    private val retryTrigger = MutableStateFlow(0)

    init {
        monitorConnectivity()
        monitorOffer()
    }

    /** Reloads the offer, for the error state's "Try again" action. */
    fun onRetry() {
        _state.update { it.copy(isLoading = true, hasLoadError = false) }
        retryTrigger.update { it + 1 }
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .distinctUntilChanged()
                .catch { Timber.e(it) }
                .collect { isConnected ->
                    _state.update { it.copy(isConnected = isConnected) }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorOffer() {
        viewModelScope.launch {
            retryTrigger
                .flatMapLatest { monitorSubscriptionOfferUseCase() }
                .catch { Timber.e(it, "Failed to monitor the recommended offer") }
                .collect { result ->
                    result.onFailure { Timber.e(it, "Failed to load the recommended offer") }
                    applyOffer(result.getOrNull(), hasLoadError = result.isFailure)
                }
        }
    }

    private fun applyOffer(offer: RecommendedSubscriptionOffer?, hasLoadError: Boolean) {
        val subscription = offer?.subscription
        if (subscription == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    hasLoadError = hasLoadError,
                    offerSubscription = null,
                    offerValidUntil = null,
                    hasMultipleOffers = false,
                )
            }
            return
        }
        val isMonthly = subscription.sku.endsWith(MONTHLY_SKU_SUFFIX)
        _state.update {
            it.copy(
                isLoading = false,
                hasLoadError = false,
                offerSubscription = localisedSubscriptionMapper(
                    monthlySubscription = subscription.takeIf { isMonthly },
                    yearlySubscription = subscription.takeUnless { isMonthly },
                ),
                isMonthly = isMonthly,
                offerValidUntil = subscription.offerValidUntil,
                hasMultipleOffers = offer.hasMultipleOffers,
            )
        }
    }

    private companion object {
        private const val MONTHLY_SKU_SUFFIX = ".onemonth"
    }
}
