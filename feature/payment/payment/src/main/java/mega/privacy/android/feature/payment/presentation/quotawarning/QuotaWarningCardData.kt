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
)
