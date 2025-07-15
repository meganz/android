package mega.privacy.android.data.mapper

import com.android.billingclient.api.Purchase
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.billing.MegaPurchaseState

internal typealias MegaPurchaseMapper = (@JvmSuppressWildcards Purchase) -> @JvmSuppressWildcards MegaPurchase

internal fun toMegaPurchase(purchase: Purchase) = MegaPurchase(
    sku = purchase.products.first(),
    receipt = purchase.originalJson,
    state = purchase.purchaseState,
    megaPurchaseState = when (purchase.purchaseState) {
        Purchase.PurchaseState.PURCHASED -> MegaPurchaseState.Purchased
        Purchase.PurchaseState.PENDING -> MegaPurchaseState.Pending
        else -> MegaPurchaseState.Unspecified
    },
    token = purchase.purchaseToken
)