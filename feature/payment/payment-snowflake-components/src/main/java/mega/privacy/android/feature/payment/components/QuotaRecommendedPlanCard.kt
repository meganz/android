package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack

/**
 * Card recommending the next-tier plan on the quota-warning upsell screen: a "Best for you"
 * badge, plan name, price, storage/transfer features, and a bar showing how the current usage
 * would sit against the recommended plan's quota. The buy action lives in the anchored button
 * bar, so this card has no button of its own.
 *
 * @param planName the recommended plan name (e.g. "Essential")
 * @param monthlyPriceText per-month price text (e.g. "€3.33/month")
 * @param storageText storage feature text (e.g. "200 GB storage")
 * @param transferText transfer feature text (e.g. "2.4 TB transfer")
 * @param badgeLabel the badge label (e.g. "Best for you")
 * @param usagePercentage current usage against the recommended plan's quota, as a 0..100 value
 * @param usageLevel severity level driving the bar colour
 * @param usageText usage help text (e.g. "Storage: 19 GB out of 200 GB")
 * @param yearlyTotalText total yearly charge (e.g. "€40.01 charged yearly"), null for monthly billing
 */
@Composable
fun QuotaRecommendedPlanCard(
    planName: String,
    monthlyPriceText: String,
    storageText: String,
    transferText: String,
    badgeLabel: String,
    usagePercentage: Float,
    usageLevel: QuotaUsageLevel,
    usageText: String,
    modifier: Modifier = Modifier,
    yearlyTotalText: String? = null,
) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = DSTokens.colors.border.strong, shape = cardShape)
            .clip(cardShape)
            .testTag(TEST_TAG_QUOTA_RECOMMENDED_CARD),
    ) {
        Badge(
            badgeType = BadgeType.Mega,
            text = badgeLabel,
            modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_BADGE),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MegaText(
                    text = planName,
                    style = MaterialTheme.typography.headlineMedium,
                    textColor = TextColor.Primary,
                    modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_NAME),
                )
                if (yearlyTotalText.isNullOrEmpty()) {
                    MegaText(
                        text = monthlyPriceText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_PRICE),
                    )
                } else {
                    MegaText(
                        text = monthlyPriceText,
                        style = MaterialTheme.typography.titleMedium,
                        textColor = TextColor.Secondary,
                        modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_PRICE_PER_MONTH),
                    )
                    MegaText(
                        text = yearlyTotalText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_PRICE),
                    )
                }
                Column {
                    QuotaFeatureRow(
                        icon = IconPack.Medium.Thin.Outline.Cloud,
                        text = storageText,
                        modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_STORAGE),
                    )
                    QuotaFeatureRow(
                        icon = IconPack.Medium.Thin.Outline.ArrowsUpDown,
                        text = transferText,
                        modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_TRANSFER),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuotaUsageProgressBar(
                    percentage = usagePercentage,
                    level = usageLevel,
                    modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_PROGRESS),
                )
                MegaText(
                    text = usageText,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_QUOTA_RECOMMENDED_USAGE),
                )
            }
        }
    }
}

@Composable
private fun QuotaFeatureRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MegaIcon(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            tint = IconColor.Brand,
        )
        MegaText(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textColor = TextColor.Secondary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun QuotaRecommendedPlanCardPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        QuotaRecommendedPlanCard(
            planName = "Essential",
            monthlyPriceText = "€3.33/month",
            yearlyTotalText = "€40.01 charged yearly",
            storageText = "200 GB storage",
            transferText = "2.4 TB transfer",
            badgeLabel = "Best for you",
            usagePercentage = 10f,
            usageLevel = QuotaUsageLevel.Normal,
            usageText = "Storage: 19 GB out of 200 GB",
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Tag for the QuotaRecommendedPlanCard root container
 */
const val TEST_TAG_QUOTA_RECOMMENDED_CARD = "quota_recommended_plan_card"

/**
 * Tag for the QuotaRecommendedPlanCard "Best for you" badge
 */
const val TEST_TAG_QUOTA_RECOMMENDED_BADGE = "quota_recommended_plan_card:badge"

/**
 * Tag for the QuotaRecommendedPlanCard plan name
 */
const val TEST_TAG_QUOTA_RECOMMENDED_NAME = "quota_recommended_plan_card:name"

/**
 * Tag for the QuotaRecommendedPlanCard main price (per month for monthly, total for yearly)
 */
const val TEST_TAG_QUOTA_RECOMMENDED_PRICE = "quota_recommended_plan_card:price"

/**
 * Tag for the QuotaRecommendedPlanCard per-month price shown on yearly cards
 */
const val TEST_TAG_QUOTA_RECOMMENDED_PRICE_PER_MONTH = "quota_recommended_plan_card:price_per_month"

/**
 * Tag for the QuotaRecommendedPlanCard storage feature text
 */
const val TEST_TAG_QUOTA_RECOMMENDED_STORAGE = "quota_recommended_plan_card:storage"

/**
 * Tag for the QuotaRecommendedPlanCard transfer feature text
 */
const val TEST_TAG_QUOTA_RECOMMENDED_TRANSFER = "quota_recommended_plan_card:transfer"

/**
 * Tag for the QuotaRecommendedPlanCard usage progress bar
 */
const val TEST_TAG_QUOTA_RECOMMENDED_PROGRESS = "quota_recommended_plan_card:progress"

/**
 * Tag for the QuotaRecommendedPlanCard usage help text
 */
const val TEST_TAG_QUOTA_RECOMMENDED_USAGE = "quota_recommended_plan_card:usage"
