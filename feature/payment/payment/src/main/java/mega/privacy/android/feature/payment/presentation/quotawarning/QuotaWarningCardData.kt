package mega.privacy.android.feature.payment.presentation.quotawarning

import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.feature.payment.components.QuotaUsageLevel

/**
 * Data for the current-plan card shown on the quota-warning upsell screen.
 *
 * @property planName the current plan name (e.g. "Free")
 * @property currentPlanLabel label shown next to the plan name (e.g. "Current plan")
 * @property usagePercentage current usage as a 0..100 value
 * @property usageLevel severity level driving the usage bar colour
 * @property usageText usage help text (e.g. "Storage: 19 GB out of 20 GB")
 */
internal data class CurrentCardData(
    val planName: String,
    val currentPlanLabel: String,
    val usagePercentage: Float,
    val usageLevel: QuotaUsageLevel,
    val usageText: String,
)

/**
 * Data for the recommended-plan card shown on the quota-warning upsell screen.
 *
 * @property planName the recommended plan name (e.g. "Essential")
 * @property monthlyPriceText per-month price text (e.g. "€3.33/month")
 * @property yearlyTotalText total yearly charge (e.g. "€40.01 charged yearly"), null for monthly billing
 * @property storageText storage feature text (e.g. "200 GB storage")
 * @property transferText transfer feature text (e.g. "2.4 TB transfer")
 * @property usagePercentage current usage against the recommended plan's quota, as a 0..100 value
 * @property usageLevel severity level driving the usage bar colour
 * @property usageText usage help text (e.g. "Storage: 19 GB out of 200 GB")
 * @property subscriptionToBuy the subscription launched when the upgrade button is tapped, null when unavailable
 * @property offer discount data when the recommended subscription carries an active offer, null otherwise
 */
internal data class RecommendedCardData(
    val planName: String,
    val monthlyPriceText: String,
    val yearlyTotalText: String?,
    val storageText: String,
    val transferText: String,
    val usagePercentage: Float,
    val usageLevel: QuotaUsageLevel,
    val usageText: String,
    val subscriptionToBuy: Subscription?,
    val offer: RecommendedOfferData? = null,
)

/**
 * Discount data for the recommended-plan card when the recommended subscription carries an active
 * offer. When present, the card renders as a promotional offer card instead of the regular one.
 *
 * @property priceText the discounted price shown as the main price (e.g. "€4.99/month")
 * @property originalPriceText the pre-discount price shown with a strikethrough (e.g. "€9.99")
 * @property discountDescriptionText the discount explanation (e.g. "Billed at ... for the first year")
 * @property discountBadgeText the promotional badge text (e.g. "Special offer · 50% off")
 * @property monthlyPriceText per-month discounted price shown above the total on yearly plans, null otherwise
 */
internal data class RecommendedOfferData(
    val priceText: String,
    val originalPriceText: String,
    val discountDescriptionText: String,
    val discountBadgeText: String,
    val monthlyPriceText: String?,
)
