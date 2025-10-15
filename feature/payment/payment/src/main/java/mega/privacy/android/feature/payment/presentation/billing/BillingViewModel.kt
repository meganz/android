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
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.usecase.billing.MonitorBillingEventUseCase
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.domain.usecase.billing.QuerySkus
import mega.privacy.android.feature.payment.domain.LaunchPurchaseFlowUseCase
import mega.privacy.android.feature.payment.model.mapper.AccountTypeToProductIdMapper
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
    private val launchPurchaseFlowUseCase: LaunchPurchaseFlowUseCase,
    private val accountTypeToProductIdMapper: AccountTypeToProductIdMapper,
    monitorBillingEventUseCase: MonitorBillingEventUseCase,
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
     * Load skus
     *
     */
    fun loadSkus() {
        viewModelScope.launch {
            _skus.value = runCatching { querySkus() }
                .onFailure {
                    Timber.Forest.e(it, "Failed to query SKUs")
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
                    Timber.Forest.e(it, "Failed to query purchase")
                }
                .getOrElse { emptyList() }
        }
    }

    /**
     * Start purchase
     *
     */
    fun startPurchase(activity: Activity, accountType: AccountType, isMonthly: Boolean) {
        viewModelScope.launch {
            runCatching {
                val productId = accountTypeToProductIdMapper(accountType, isMonthly)
                launchPurchaseFlowUseCase(activity, productId)
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