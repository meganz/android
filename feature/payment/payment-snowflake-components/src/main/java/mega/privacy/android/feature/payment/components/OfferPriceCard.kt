package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack

/**
 * Discount variant of [PlanPriceCard] for the redesigned subscription page. Shows a promotional
 * badge, the original price with a strikethrough alongside the discounted price, a discount
 * description, plan benefits, and a buy button.
 *
 * @param planName the plan name (e.g. "Pro I")
 * @param priceText the discounted price shown as the main price (e.g. "€4.99/month" for monthly,
 * "€59.88 charged yearly" for yearly)
 * @param originalPriceText the pre-discount price shown with a strikethrough (e.g. "€9.99")
 * @param discountDescriptionText the discount description (e.g. "Discount price for the first 12 months")
 * @param discountBadgeText the promotional badge text (e.g. "Black Friday · 50% off")
 * @param storageText storage benefit (e.g. "2 TB cloud storage")
 * @param transferText transfer benefit (e.g. "2 TB transfer")
 * @param buyButtonText buy button label (e.g. "Get Pro I for €4.99/month")
 * @param onBuyClick called when the buy button is tapped
 * @param monthlyPriceText the per-month price shown above the yearly total (e.g. "€4.99/month"),
 * null for monthly plans
 */
@Composable
fun OfferPriceCard(
    planName: String,
    priceText: String,
    originalPriceText: String,
    discountDescriptionText: String,
    discountBadgeText: String,
    storageText: String,
    transferText: String,
    buyButtonText: String,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
    monthlyPriceText: String? = null,
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.strong,
                shape = cardShape,
            )
            .clip(cardShape)
            .testTag(TEST_TAG_OFFER_PRICE_CARD),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 44.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MegaText(
                text = planName,
                style = MaterialTheme.typography.headlineSmall,
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_OFFER_PRICE_CARD_TITLE),
            )
            if (!monthlyPriceText.isNullOrEmpty()) {
                MegaText(
                    text = monthlyPriceText,
                    style = MaterialTheme.typography.titleMedium,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_OFFER_PRICE_CARD_PRICE_PER_MONTH),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MegaText(
                    text = originalPriceText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.LineThrough,
                    ),
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_OFFER_PRICE_CARD_ORIGINAL_PRICE),
                )
                MegaText(
                    text = priceText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    textColor = TextColor.Primary,
                    modifier = Modifier.testTag(TEST_TAG_OFFER_PRICE_CARD_PRICE),
                )
            }
            MegaText(
                text = discountDescriptionText,
                style = MaterialTheme.typography.bodyMedium,
                textColor = TextColor.Secondary,
                modifier = Modifier.testTag(TEST_TAG_OFFER_PRICE_CARD_DISCOUNT_DESCRIPTION),
            )
            Column {
                PlanFeatureRow(
                    icon = IconPack.Medium.Thin.Outline.Cloud,
                    text = storageText,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_OFFER_PRICE_CARD_STORAGE),
                )
                PlanFeatureRow(
                    icon = IconPack.Medium.Thin.Outline.ArrowsUpDown,
                    text = transferText,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.testTag(TEST_TAG_OFFER_PRICE_CARD_TRANSFER),
                )
            }
            PrimaryFilledButton(
                text = buyButtonText,
                onClick = onBuyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TEST_TAG_OFFER_PRICE_CARD_BUTTON),
            )
        }
        DiscountBadge(
            text = discountBadgeText,
            modifier = Modifier
                .align(Alignment.TopStart)
                .testTag(TEST_TAG_OFFER_PRICE_CARD_BADGE),
        )
    }
}

@Composable
private fun DiscountBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    MegaText(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textColor = TextColor.OnColor,
        modifier = modifier
            .background(
                color = DSTokens.colors.brand.default,
                shape = RoundedCornerShape(bottomEnd = 8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@CombinedThemePreviews
@Composable
private fun OfferPriceCardMonthlyPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        OfferPriceCard(
            planName = "Pro I",
            priceText = "€4.99/month",
            originalPriceText = "€9.99",
            discountDescriptionText = "Discount price for the first 12 months",
            discountBadgeText = "Black Friday · 50% off",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            buyButtonText = "Get Pro I for €4.99/month",
            onBuyClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OfferPriceCardYearlyPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        OfferPriceCard(
            planName = "Pro I",
            priceText = "€59.88 charged yearly",
            originalPriceText = "€120",
            discountDescriptionText = "Discount price for the first 12 months",
            discountBadgeText = "Black Friday · 50% off",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            buyButtonText = "Get Pro I for €59.88",
            onBuyClick = {},
            monthlyPriceText = "€4.99/month",
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Tag for the OfferPriceCard root container
 */
const val TEST_TAG_OFFER_PRICE_CARD = "offer_price_card"

/**
 * Tag for the OfferPriceCard promotional badge
 */
const val TEST_TAG_OFFER_PRICE_CARD_BADGE = "offer_price_card:badge"

/**
 * Tag for the OfferPriceCard title
 */
const val TEST_TAG_OFFER_PRICE_CARD_TITLE = "offer_price_card:title"

/**
 * Tag for the OfferPriceCard per-month price shown on yearly cards
 */
const val TEST_TAG_OFFER_PRICE_CARD_PRICE_PER_MONTH = "offer_price_card:price_per_month"

/**
 * Tag for the OfferPriceCard original (pre-discount) price with strikethrough
 */
const val TEST_TAG_OFFER_PRICE_CARD_ORIGINAL_PRICE = "offer_price_card:original_price"

/**
 * Tag for the OfferPriceCard discounted main price
 */
const val TEST_TAG_OFFER_PRICE_CARD_PRICE = "offer_price_card:price"

/**
 * Tag for the OfferPriceCard discount description
 */
const val TEST_TAG_OFFER_PRICE_CARD_DISCOUNT_DESCRIPTION = "offer_price_card:discount_description"

/**
 * Tag for the OfferPriceCard storage text
 */
const val TEST_TAG_OFFER_PRICE_CARD_STORAGE = "offer_price_card:storage"

/**
 * Tag for the OfferPriceCard transfer text
 */
const val TEST_TAG_OFFER_PRICE_CARD_TRANSFER = "offer_price_card:transfer"

/**
 * Tag for the OfferPriceCard buy button
 */
const val TEST_TAG_OFFER_PRICE_CARD_BUTTON = "offer_price_card:button"
