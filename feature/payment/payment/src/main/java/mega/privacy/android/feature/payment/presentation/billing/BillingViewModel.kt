package mega.privacy.android.feature.payment.presentation.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.payment.UpgradeSource
import mega.privacy.android.domain.usecase.billing.MonitorBillingEventUseCase
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.feature.payment.domain.LaunchPurchaseFlowUseCase
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
}
