package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.entity.PurchaseType
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.billing.MegaPurchaseState
import mega.privacy.android.domain.entity.payment.UpgradeSource
import mega.privacy.android.domain.usecase.billing.MonitorBillingEventUseCase
import mega.privacy.android.navigation.contract.queue.NavPriority
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import mega.privacy.android.navigation.destination.PurchaseResultDialogNavKey
import timber.log.Timber
import javax.inject.Inject

class PurchaseResultInitialiser @Inject constructor(
    private val monitorBillingEventUseCase: MonitorBillingEventUseCase,
    private val appDialogsEventQueue: AppDialogsEventQueue,
) : PostLoginInitialiser(
    action = { _, _ ->
        monitorBillingEventUseCase()
            .catch { Timber.e(it, "Failed to monitor billing event") }
            .filterIsInstance<BillingEvent.OnPurchaseUpdate>()
            .collectLatest { billingEvent ->
                handlePurchaseUpdate(
                    source = billingEvent.upgradeSource,
                    appDialogsEventQueue = appDialogsEventQueue,
                    billingEvent = billingEvent
                )
            }
    }
)

private suspend fun handlePurchaseUpdate(
    source: UpgradeSource,
    appDialogsEventQueue: AppDialogsEventQueue,
    billingEvent: BillingEvent.OnPurchaseUpdate,
) {
    val purchaseType = getPurchaseType(billingEvent.purchases)

    if (source == UpgradeSource.Main) {
        billingEvent.activeSubscription?.sku?.removeSuffix(".test")?.let { activeSubscriptionSku ->
            appDialogsEventQueue.emit(
                AppDialogEvent(
                    PurchaseResultDialogNavKey(
                        purchaseType = purchaseType,
                        activeSubscriptionSku = activeSubscriptionSku
                    )
                ),
                priority = NavPriority.Default
            )
        }
    }
}

private fun getPurchaseType(purchases: List<MegaPurchase>): PurchaseType {
    return if (purchases.isNotEmpty()) {
        val purchase = purchases.first()
        if (purchase.megaPurchaseState == MegaPurchaseState.Purchased) {
            PurchaseType.SUCCESS
        } else {
            PurchaseType.PENDING
        }
    } else {
        PurchaseType.DOWNGRADE
    }
}


