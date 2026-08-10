package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack

/**
 * Discount variant of [QuotaRecommendedPlanCard] for the quota-warning upsell screen. Shows the
 * recommended plan as a promotional offer: a discount badge, the original price with a strikethrough
 * alongside the discounted price, a discount description, plan benefits, and a bar showing how the
 * current usage would sit against the recommended plan's quota. The buy action lives in the anchored
 * button bar, so this card has no button of its own.
 *
 * @param planName the recommended plan name (e.g. "Pro I")
 * @param priceText the discounted price shown as the main price (e.g. "€4.99/month" for monthly,
 * "€59.88 charged yearly" for yearly)
 * @param originalPriceText the pre-discount price shown with a strikethrough (e.g. "€9.99")
 * @param discountDescriptionText the discount description (e.g. "Billed at €4.99/month for the first year")
 * @param discountBadgeText the promotional badge text (e.g. "Special offer · 50% off")
 * @param storageText storage feature text (e.g. "2 TB cloud storage")
 * @param transferText transfer feature text (e.g. "2 TB transfer")
 * @param usagePercentage current usage against the recommended plan's quota, as a 0..100 value
 * @param usageLevel severity level driving the usage bar colour
 * @param usageText usage help text (e.g. "Storage: 19 GB out of 3 TB")
 * @param monthlyPriceText the per-month price shown above the total (e.g. "€4.99/month"), null for
 * monthly plans
 */
@Composable
fun QuotaOfferPlanCard(
    planName: String,
    priceText: String,
    originalPriceText: String,
    discountDescriptionText: String,
    discountBadgeText: String,
    storageText: String,
    transferText: String,
    usagePercentage: Float,
    usageLevel: QuotaUsageLevel,
    usageText: String,
    modifier: Modifier = Modifier,
    monthlyPriceText: String? = null,
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = DSTokens.colors.border.strong, shape = cardShape)
            .background(color = DSTokens.colors.brand.containerDefault, shape = cardShape)
            .clip(cardShape)
            .testTag(TEST_TAG_QUOTA_OFFER_CARD),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 44.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MegaText(
                    text = planName,
                    style = MaterialTheme.typography.headlineMedium,
                    textColor = TextColor.Primary,
                    modifier = Modifier.testTag(TEST_TAG_QUOTA_OFFER_NAME),
                )
                if (!monthlyPriceText.isNullOrEmpty()) {
                    MegaText(
                        text = monthlyPriceText,
                        style = MaterialTheme.typography.titleMedium,
                        textColor = TextColor.Secondary,
                        modifier = Modifier.testTag(TEST_TAG_QUOTA_OFFER_PRICE_PER_MONTH),
                    )
                }
                SpannedText(
                    value = "[A]$originalPriceText[/A] [B]$priceText[/B]",
                    baseTextColor = TextColor.Secondary,
                    baseStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    spanStyles = mapOf(
                        SpanIndicator('A') to MegaSpanStyle.TextColorStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough),
                            TextColor.Secondary,
                        ),
                        SpanIndicator('B') to MegaSpanStyle.TextColorStyle(
                            SpanStyle(),
                            TextColor.Primary,
                        ),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_QUOTA_OFFER_PRICE),
                )
                MegaText(
                    text = discountDescriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_QUOTA_OFFER_DISCOUNT_DESCRIPTION),
                )
                Column {
                    PlanFeatureRow(
                        icon = IconPack.Medium.Thin.Outline.Cloud,
                        text = storageText,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        textColor = TextColor.Secondary,
                        modifier = Modifier.testTag(TEST_TAG_QUOTA_OFFER_STORAGE),
                    )
                    PlanFeatureRow(
                        icon = IconPack.Medium.Thin.Outline.ArrowsUpDown,
                        text = transferText,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        textColor = TextColor.Secondary,
                        modifier = Modifier.testTag(TEST_TAG_QUOTA_OFFER_TRANSFER),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuotaUsageProgressBar(
                    percentage = usagePercentage,
                    level = usageLevel,
                    modifier = Modifier.testTag(TEST_TAG_QUOTA_OFFER_PROGRESS),
                )
                MegaText(
                    text = usageText,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_QUOTA_OFFER_USAGE),
                )
            }
        }
        DiscountBadge(
            text = discountBadgeText,
            modifier = Modifier
                .align(Alignment.TopStart)
                .testTag(TEST_TAG_QUOTA_OFFER_BADGE),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun QuotaOfferPlanCardPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        QuotaOfferPlanCard(
            planName = "Pro I",
            priceText = "€29.94 charged yearly",
            originalPriceText = "€59.88",
            discountDescriptionText = "Billed at €29.94 for the first year, €119.88 charged yearly after",
            discountBadgeText = "Special offer · 50% off",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            usagePercentage = 10f,
            usageLevel = QuotaUsageLevel.Normal,
            usageText = "Storage: 19 GB out of 3 TB",
            monthlyPriceText = "€4.99/month",
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Tag for the QuotaOfferPlanCard root container
 */
const val TEST_TAG_QUOTA_OFFER_CARD = "quota_offer_plan_card"

/**
 * Tag for the QuotaOfferPlanCard promotional badge
 */
const val TEST_TAG_QUOTA_OFFER_BADGE = "quota_offer_plan_card:badge"

/**
 * Tag for the QuotaOfferPlanCard plan name
 */
const val TEST_TAG_QUOTA_OFFER_NAME = "quota_offer_plan_card:name"

/**
 * Tag for the QuotaOfferPlanCard per-month price shown above the total
 */
const val TEST_TAG_QUOTA_OFFER_PRICE_PER_MONTH = "quota_offer_plan_card:price_per_month"

/**
 * Tag for the QuotaOfferPlanCard billing row (strikethrough original price and discounted price)
 */
const val TEST_TAG_QUOTA_OFFER_PRICE = "quota_offer_plan_card:price"

/**
 * Tag for the QuotaOfferPlanCard discount description
 */
const val TEST_TAG_QUOTA_OFFER_DISCOUNT_DESCRIPTION = "quota_offer_plan_card:discount_description"

/**
 * Tag for the QuotaOfferPlanCard storage feature text
 */
const val TEST_TAG_QUOTA_OFFER_STORAGE = "quota_offer_plan_card:storage"

/**
 * Tag for the QuotaOfferPlanCard transfer feature text
 */
const val TEST_TAG_QUOTA_OFFER_TRANSFER = "quota_offer_plan_card:transfer"

/**
 * Tag for the QuotaOfferPlanCard usage progress bar
 */
const val TEST_TAG_QUOTA_OFFER_PROGRESS = "quota_offer_plan_card:progress"

/**
 * Tag for the QuotaOfferPlanCard usage help text
 */
const val TEST_TAG_QUOTA_OFFER_USAGE = "quota_offer_plan_card:usage"
