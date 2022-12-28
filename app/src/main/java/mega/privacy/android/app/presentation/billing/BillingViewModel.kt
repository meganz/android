package mega.privacy.android.app.presentation.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.Purchase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import mega.privacy.android.app.usecase.billing.LaunchPurchaseFlow
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.usecase.billing.MonitorBillingEvent
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.domain.usecase.billing.QuerySkus
import timber.log.Timber
import javax.inject.Inject

/**
 * Billing view model
 * Process query skus, purchase and handle start payment flow
 *
 */
@HiltViewModel
class BillingViewModel @Inject constructor(
    private val querySkus: QuerySkus,
    private val queryPurchase: QueryPurchase,
    private val launchPurchaseFlow: LaunchPurchaseFlow,
    monitorBillingEvent: MonitorBillingEvent,
) : ViewModel() {
    private val _skus = MutableStateFlow<List<MegaSku>>(emptyList())

    /**
     * Skus state, all skus from billing system
     */
    val skus = _skus.asStateFlow()

    private val _purchases = MutableStateFlow<List<MegaPurchase>>(emptyList())

    /**
     * All purchases from billing system
     */
    val purchases = _purchases.asStateFlow()

    /**
     * Billing update event
     */
    val billingUpdateEvent = monitorBillingEvent()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Load skus
     *
     */
    fun loadSkus() {
        viewModelScope.launch {
            _skus.value = runCatching { querySkus() }
                .onFailure {
                    Timber.e(it)
                }
                .getOrElse { emptyList() }
        }
    }

    /**
     * Load purchases
     *
     */
    fun loadPurchases() {
        viewModelScope.launch {
            _purchases.value = runCatching { queryPurchase() }
                .onFailure {
                    Timber.e(it)
                }
                .getOrElse { emptyList() }
        }
    }

    /**
     * Start purchase
     *
     */
    fun startPurchase(activity: Activity, productId: String) {
        viewModelScope.launch {
            launchPurchaseFlow(activity, productId)
        }
    }

    /**
     * Is purchased
     *
     */
    fun isPurchased(purchase: MegaPurchase): Boolean {
        return purchase.state == Purchase.PurchaseState.PURCHASED
    }
}