package mega.privacy.android.data.mapper

import com.android.billingclient.api.ProductDetails
import mega.privacy.android.domain.entity.account.MegaSku

internal typealias MegaSkuMapper = (@JvmSuppressWildcards ProductDetails) -> @JvmSuppressWildcards MegaSku?

internal fun toMegaSku(product: ProductDetails): MegaSku? {
    return product.subscriptionOfferDetails?.firstOrNull()?.let { offerDetails ->
        val pricingPhase = offerDetails.pricingPhases.pricingPhaseList.first()
        return MegaSku(product.productId,
            pricingPhase.priceAmountMicros,
            pricingPhase.priceCurrencyCode
        )
    }
}