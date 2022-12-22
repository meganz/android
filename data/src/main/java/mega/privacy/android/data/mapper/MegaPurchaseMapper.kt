package mega.privacy.android.data.mapper

import com.android.billingclient.api.Purchase
import mega.privacy.android.domain.entity.billing.MegaPurchase

internal typealias MegaPurchaseMapper = (@JvmSuppressWildcards Purchase) -> @JvmSuppressWildcards MegaPurchase

internal fun toMegaPurchase(purchase: Purchase) = MegaPurchase(
    purchase.products.first(),
    purchase.originalJson,
    purchase.purchaseState,
    purchase.purchaseToken
)