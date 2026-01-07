package mega.privacy.android.feature.payment.presentation.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.payment.UpgradeSource
import mega.privacy.android.domain.usecase.billing.MonitorBillingEventUseCase
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.feature.payment.domain.LaunchPurchaseFlowUseCase
import mega.privacy.android.feature.payment.model.BillingUIState
import mega.privacy.android.feature.payment.model.extensions.toWebClientProductId
import mega.privacy.android.feature.payment.usecase.GeneratePurchaseUrlUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Billing view model
 * Process query skus, purchase and handle start payment flow
 *
 */
@HiltViewModel
class BillingViewModel @Inject constructor(
    private val queryPurchase: QueryPurchase,
    private val launchPurchaseFlowUseCase: LaunchPurchaseFlowUseCase,
    private val generatePurchaseUrlUseCase: GeneratePurchaseUrlUseCase,
    monitorBillingEventUseCase: MonitorBillingEventUseCase,
) : ViewModel() {

    /**
     * Billing update event
     */
    private val _billingUpdateEvent = MutableStateFlow<BillingEvent?>(null)

    /**
     * Billing update event
     */
    val billingUpdateEvent = _billingUpdateEvent.asStateFlow()

    /**
     * Billing UI state containing error events and external purchase click events
     */
    private val _uiState = MutableStateFlow(BillingUIState())

    /**
     * Billing UI state containing error events and external purchase click events
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            monitorBillingEventUseCase()
                .catch { Timber.Forest.e(it, "Failed to monitor billing event") }
                .collect {
                    _billingUpdateEvent.value = it
                }
        }
    }

    /**
     * Load purchases
     *
     */
    fun loadPurchases() {
        viewModelScope.launch {
            runCatching { queryPurchase() }
                .onFailure {
                    Timber.Forest.e(it, "Failed to query purchase")
                }
                .getOrElse { emptyList() }
        }
    }

    /**
     * Start purchase
     *
     */
    fun startPurchase(
        activity: Activity,
        subscription: Subscription,
        source: UpgradeSource,
    ) {
        viewModelScope.launch {
            runCatching {
                launchPurchaseFlowUseCase(
                    activity = activity,
                    source = source,
                    productId = subscription.sku,
                    offerId = subscription.offerId
                )
            }.onFailure {
                Timber.Forest.e(it, "Failed to launch purchase flow")
            }
        }
    }

    /**
     * Clears the external purchase error state.
     *
     * This should be called after the error has been displayed to the user to reset
     * the error state and allow new errors to be shown.
     */
    fun clearExternalPurchaseError() {
        _uiState.update {
            it.copy(generalError = consumed)
        }
    }

    /**
     * Clears the external purchase click event state.
     *
     * This should be called after the external purchase click event has been consumed
     * by the UI (e.g., after launching the external browser with the purchase URL) to reset
     * the event state and allow new events to be triggered.
     */
    fun onExternalPurchaseClickEventConsumed() {
        _uiState.update {
            it.copy(onExternalPurchaseClick = consumed())
        }
    }

    /**
     * Is purchased
     *
     */
    fun isPurchased(purchase: MegaPurchase): Boolean =
        purchase.state == Purchase.PurchaseState.PURCHASED

    /**
     * Mark handle billing event
     *
     */
    fun markHandleBillingEvent() {
        viewModelScope.launch {
            _billingUpdateEvent.value = null
        }
    }

    /**
     * Handles external purchase click by generating a purchase URL and updating the UI state.
     *
     * This method generates a purchase URL for external checkout based on the subscription
     * and billing period (monthly or yearly). The generated URL is set in [uiState]
     * as a triggered event that can be observed by the UI to launch the external purchase flow.
     * If URL generation fails, an error is set in [uiState.generalError] which can be
     * observed by the UI to show user feedback.
     *
     * @param subscription The subscription to purchase, containing account type information
     * @param monthly True if the billing period is monthly (1 month), false if yearly (12 months)
     */
    fun onExternalPurchaseClick(subscription: Subscription, monthly: Boolean) {
        viewModelScope.launch {
            runCatching {
                val externalCheckoutUrl = generatePurchaseUrlUseCase(
                    subscription.accountType.toWebClientProductId(),
                    if (monthly) 1 else 12
                )
                _uiState.update {
                    it.copy(
                        onExternalPurchaseClick = triggered(content = externalCheckoutUrl)
                    )
                }
            }.onFailure { exception ->
                Timber.Forest.e(exception, "Failed to launch purchase flow")
                onGeneralError()
            }
        }
    }

    fun onGeneralError() {
        _uiState.update {
            it.copy(generalError = triggered)
        }
    }
}
