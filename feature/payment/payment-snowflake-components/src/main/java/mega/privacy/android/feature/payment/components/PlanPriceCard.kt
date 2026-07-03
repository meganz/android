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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack

/**
 * Plan card for the redesigned subscription page (no-offer state). Each card shows the plan name,
 * price, benefits, and its own buy button.
 *
 * @param planName the plan name (e.g. "Pro I")
 * @param monthlyPriceText the per-month price text (e.g. "€4.99/month")
 * @param storageText storage benefit (e.g. "2 TB cloud storage")
 * @param transferText transfer benefit (e.g. "2 TB transfer")
 * @param buyButtonText buy button label (e.g. "Get Pro I")
 * @param onBuyClick called when the buy button is tapped
 * @param isRecommended whether this is the recommended plan (primary button + recommended label)
 * @param recommendedLabel the recommended label text, required when [isRecommended] is true
 * @param yearlyTotalText the total yearly charge text (e.g. "€29.94 charged yearly"), null for monthly
 * @param isCurrentPlan whether this plan is the user's current plan (hides the buy button)
 */
@Composable
fun PlanPriceCard(
    planName: String,
    monthlyPriceText: String,
    storageText: String,
    transferText: String,
    buyButtonText: String,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRecommended: Boolean = false,
    recommendedLabel: String? = null,
    yearlyTotalText: String? = null,
    isCurrentPlan: Boolean = false,
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isRecommended) 2.dp else 1.dp,
                color = if (isRecommended) {
                    DSTokens.colors.border.strongSelected
                } else {
                    DSTokens.colors.border.strong
                },
                shape = cardShape,
            )
            .clip(cardShape)
            .testTag(TEST_TAG_PLAN_PRICE_CARD),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                    top = if (isRecommended) 44.dp else 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MegaText(
                text = planName,
                style = MaterialTheme.typography.headlineSmall,
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_PLAN_PRICE_CARD_TITLE),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (yearlyTotalText.isNullOrEmpty()) {
                    MegaText(
                        text = monthlyPriceText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(TEST_TAG_PLAN_PRICE_CARD_PRICE),
                    )
                } else {
                    MegaText(
                        text = monthlyPriceText,
                        style = MaterialTheme.typography.titleMedium,
                        textColor = TextColor.Secondary,
                        modifier = Modifier.testTag(TEST_TAG_PLAN_PRICE_CARD_PRICE_PER_MONTH),
                    )
                    MegaText(
                        text = yearlyTotalText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(TEST_TAG_PLAN_PRICE_CARD_PRICE),
                    )
                }
            }
            Column {
                PlanFeatureRow(
                    icon = IconPack.Medium.Thin.Outline.Cloud,
                    text = storageText,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_PLAN_PRICE_CARD_STORAGE),
                )
                PlanFeatureRow(
                    icon = IconPack.Medium.Thin.Outline.ArrowsUpDown,
                    text = transferText,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_PLAN_PRICE_CARD_TRANSFER),
                )
            }
            if (!isCurrentPlan) {
                if (isRecommended) {
                    PrimaryFilledButton(
                        text = buyButtonText,
                        onClick = onBuyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TEST_TAG_PLAN_PRICE_CARD_BUTTON),
                    )
                } else {
                    SecondaryFilledButton(
                        text = buyButtonText,
                        onClick = onBuyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TEST_TAG_PLAN_PRICE_CARD_BUTTON),
                    )
                }
            }
        }
        if (isRecommended && !recommendedLabel.isNullOrEmpty()) {
            RecommendedBadge(
                text = recommendedLabel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .testTag(TEST_TAG_PLAN_PRICE_CARD_RECOMMENDED),
            )
        }
    }
}

@Composable
private fun RecommendedBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    MegaText(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        textColor = TextColor.Info,
        modifier = modifier
            .background(
                color = DSTokens.colors.notifications.notificationInfo,
                shape = RoundedCornerShape(bottomEnd = 8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@CombinedThemePreviews
@Composable
private fun PlanPriceCardMonthlyPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        PlanPriceCard(
            planName = "Pro I",
            monthlyPriceText = "€4.99/month",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            buyButtonText = "Get Pro I",
            onBuyClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PlanPriceCardYearlyRecommendedPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        PlanPriceCard(
            planName = "Pro II",
            monthlyPriceText = "€9.99/month",
            yearlyTotalText = "€119.88 charged yearly",
            storageText = "10 TB cloud storage",
            transferText = "10 TB transfer",
            buyButtonText = "Get Pro II",
            onBuyClick = {},
            isRecommended = true,
            recommendedLabel = "Recommended",
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Tag for the PlanPriceCard root container
 */
const val TEST_TAG_PLAN_PRICE_CARD = "plan_price_card"

/**
 * Tag for the PlanPriceCard recommended label
 */
const val TEST_TAG_PLAN_PRICE_CARD_RECOMMENDED = "plan_price_card:recommended"

/**
 * Tag for the PlanPriceCard title
 */
const val TEST_TAG_PLAN_PRICE_CARD_TITLE = "plan_price_card:title"

/**
 * Tag for the PlanPriceCard main price (per month for monthly, total for yearly)
 */
const val TEST_TAG_PLAN_PRICE_CARD_PRICE = "plan_price_card:price"

/**
 * Tag for the PlanPriceCard per-month price shown on yearly cards
 */
const val TEST_TAG_PLAN_PRICE_CARD_PRICE_PER_MONTH = "plan_price_card:price_per_month"

/**
 * Tag for the PlanPriceCard storage text
 */
const val TEST_TAG_PLAN_PRICE_CARD_STORAGE = "plan_price_card:storage"

/**
 * Tag for the PlanPriceCard transfer text
 */
const val TEST_TAG_PLAN_PRICE_CARD_TRANSFER = "plan_price_card:transfer"

/**
 * Tag for the PlanPriceCard buy button
 */
const val TEST_TAG_PLAN_PRICE_CARD_BUTTON = "plan_price_card:button"
