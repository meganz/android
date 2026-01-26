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
import mega.privacy.android.domain.usecase.billing.IsExternalContentLinkSupportedUseCase
import mega.privacy.android.domain.entity.payment.UpgradeSource
import mega.privacy.android.domain.usecase.billing.MonitorBillingEventUseCase
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.feature.payment.domain.LaunchExternalContentLinkUseCase
import mega.privacy.android.feature.payment.domain.LaunchPurchaseFlowUseCase
import mega.privacy.android.feature.payment.model.BillingUIState
import mega.privacy.android.feature.payment.model.extensions.toWebClientProductId
import mega.privacy.android.feature.payment.usecase.GeneratePurchaseUrlUseCase
import timber.log.Timber
import javax.inject.Inject
import androidx.core.net.toUri
import mega.privacy.android.feature.payment.domain.CreateExternalContentLinkTokenUseCase
import mega.privacy.android.domain.entity.billing.ExternalContentLinkResult

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
    private val createExternalContentLinkTokenUseCase: CreateExternalContentLinkTokenUseCase,
    private val launchExternalContentLinkUseCase: LaunchExternalContentLinkUseCase,
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
     * This method follows the Google Play Billing Library's external content links integration:
     * 1. First checks if external content links billing program is available
     * 2. If available, generates the external website URL and uses launchExternalContentLink API
     * 3. If successful, triggers the URL launch event
     * 4. Otherwise, falls back to generating a purchase URL using the existing method
     *
     * The generated URL is set in [uiState] as a triggered event that can be observed
     * by the UI to launch the external purchase flow.
     * If URL generation fails, an error is set in [uiState.generalError] which can be
     * observed by the UI to show user feedback.
     *
     * @param activity The activity to launch from (required for external content links API)
     * @param subscription The subscription to purchase, containing account type information
     * @param monthly True if the billing period is monthly (1 month), false if yearly (12 months)
     */
    fun onExternalPurchaseClick(
        activity: Activity,
        subscription: Subscription,
        monthly: Boolean,
    ) {
        // Show loading indicator on button and disable it immediately when clicked
        _uiState.update {
            it.copy(isLoadingExternalCheckout = true)
        }

        viewModelScope.launch {
            runCatching {
                val externalTransactionToken = createExternalContentLinkTokenUseCase()

                // Log warning if token is null but external content links are available
                if (externalTransactionToken == null) {
                    Timber.Forest.w("External transaction token is null but external content links are available")
                }

                // Generate the external website URL first
                val externalCheckoutUrl = generatePurchaseUrlUseCase(
                    productId = subscription.accountType.toWebClientProductId(),
                    months = if (monthly) 1 else 12,
                    externalTransactionToken = externalTransactionToken
                )

                // Use Google Play Billing Library's external content links API
                val linkUri = externalCheckoutUrl.toUri()
                val result = launchExternalContentLinkUseCase(
                    activity = activity,
                    linkUri = linkUri,
                )

                when (result) {
                    is ExternalContentLinkResult.Success -> {
                        // Operation succeeded (token obtained and link approved)
                        // Launch the URL with the token (using CALLER_WILL_LAUNCH_LINK mode)
                        Timber.Forest.d("External content link operation succeeded, launching URL")
                        _uiState.update {
                            it.copy(
                                onExternalPurchaseClick = triggered(content = externalCheckoutUrl),
                                isLoadingExternalCheckout = false
                            )
                        }
                        // External checkout launched - purchase completion will be handled via billing events
                    }

                    is ExternalContentLinkResult.Cancelled -> {
                        // User cancelled the operation
                        Timber.Forest.d("User cancelled external content link operation")
                        _uiState.update {
                            it.copy(isLoadingExternalCheckout = false)
                        }
                    }

                    is ExternalContentLinkResult.Failed -> {
                        // Operation failed - hide loading state and show error
                        Timber.Forest.w(
                            "External content link operation failed: ${result.errorMessage}"
                        )
                        onGeneralError()
                    }
                }
            }.onFailure { exception ->
                Timber.Forest.e(exception, "Failed to launch purchase flow")
                onGeneralError()
            }
        }
    }

    fun onGeneralError() {
        _uiState.update {
            it.copy(
                generalError = triggered,
                isLoadingExternalCheckout = false
            )
        }
    }
}
