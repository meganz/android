package mega.privacy.android.data.mapper

import com.android.billingclient.api.ProductDetails
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.account.OfferDetail
import mega.privacy.android.domain.entity.account.OfferPeriod
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Mapper to convert ProductDetails to MegaSku
 */
class MegaSkuMapper @Inject constructor() {
    /**
     * Convert ProductDetails to MegaSku
     * @param product ProductDetails from Google Play Billing
     * @return MegaSku object or null if conversion fails
     */
    operator fun invoke(product: ProductDetails): MegaSku? =
        product.subscriptionOfferDetails?.takeIf { it.isNotEmpty() }?.let { offerDetails ->
            // Note: This field is only set for a discounted offer. Returns null for a regular base plan.
            // https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.SubscriptionOfferDetails#getOfferId()
            val basePricePhase = offerDetails.find { it.offerId.isNullOrEmpty() }?.pricingPhases
                ?.pricingPhaseList?.firstOrNull()
            val basePrice = basePricePhase?.priceAmountMicros ?: 0L
            val priceCurrencyCode = basePricePhase?.priceCurrencyCode.orEmpty()
            val offers = offerDetails.filter { !it.offerId.isNullOrEmpty() }
                .mapNotNull { offerDetails ->
                    val pricingPhases = offerDetails.pricingPhases.pricingPhaseList

                    if (pricingPhases.size > 1) {
                        val introPhase = pricingPhases.first()
                        val regularPhase = pricingPhases.last()

                        val offerPeriod = createOfferPeriod(
                            introPhase.billingPeriod,
                            introPhase.recurrenceMode,
                            introPhase.billingCycleCount
                        )
                        val introMonthlyPrice =
                            calculateMonthlyPrice(introPhase.priceAmountMicros, offerPeriod)
                        val regularOfferPeriod =
                            createOfferPeriod(
                                regularPhase.billingPeriod,
                                regularPhase.recurrenceMode,
                                regularPhase.billingCycleCount
                            )
                        val regularMonthlyPrice =
                            calculateMonthlyPrice(
                                regularPhase.priceAmountMicros,
                                regularOfferPeriod
                            )

                        val discountPercentage = if (regularMonthlyPrice > 0) {
                            val discountAmount = regularMonthlyPrice - introMonthlyPrice
                            ((discountAmount * 100.0) / regularMonthlyPrice).roundToInt()
                        } else null

                        OfferDetail(
                            offerId = offerDetails.offerId,
                            discountedPriceMonthly = CurrencyPoint.LocalCurrencyPoint(
                                introMonthlyPrice
                            ),
                            discountPercentage = discountPercentage,
                            offerPeriod = offerPeriod,
                        )
                    } else null
                }

            return MegaSku(
                sku = product.productId,
                priceAmountMicros = basePrice,
                priceCurrencyCode = priceCurrencyCode,
                offers = offers
            )
        }

    /**
     * Calculate monthly equivalent price from OfferPeriod
     * @param priceAmountMicros Price in micros
     * @param offerPeriod OfferPeriod object (Month or Year)
     * @return Monthly equivalent price in micros
     */
    private fun calculateMonthlyPrice(priceAmountMicros: Long, offerPeriod: OfferPeriod?): Long =
        when (offerPeriod) {
            is OfferPeriod.Year -> {
                priceAmountMicros / (offerPeriod.value * 12) // Convert years to months
            }

            is OfferPeriod.Month -> {
                priceAmountMicros / offerPeriod.value // Already in months
            }

            null -> priceAmountMicros // Default to original price if no period
        }

    /**
     * Create an OfferPeriod based on billingPeriod and billingCycleCount
     * @param billingPeriod ISO 8601 duration format (e.g., "P1M", "P1Y", "P3M")
     * @param recurrenceMode Recurrence mode from ProductDetails
     * @param billingCycleCount Number of billing cycles
     * @return Appropriate OfferPeriod object
     */
    private fun createOfferPeriod(
        billingPeriod: String,
        recurrenceMode: Int,
        billingCycleCount: Int,
    ): OfferPeriod? {
        val cycle = if (recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING) {
            billingCycleCount
        } else {
            1
        }
        val formattedBillingPeriod = billingPeriod.replace("P", "")
        return when {
            billingPeriod.contains("Y") -> {
                val years = formattedBillingPeriod.replace("Y", "").toIntOrNull() ?: 1
                OfferPeriod.Year(years * cycle)
            }

            billingPeriod.contains("M") -> {
                val months = formattedBillingPeriod.replace("M", "").toIntOrNull() ?: 1
                OfferPeriod.Month(months * cycle)
            }

            else -> null
        }
    }
}