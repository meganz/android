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
import mega.android.core.ui.components.button.BrandFilledButton
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
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
 * @param useBrandButton when true, renders the brand (red) buy button instead of the primary one
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
    useBrandButton: Boolean = false,
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
            .background(
                color = DSTokens.colors.brand.containerDefault,
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
                    .testTag(TEST_TAG_OFFER_PRICE_CARD_PRICE),
            )
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
            if (useBrandButton) {
                BrandFilledButton(
                    text = buyButtonText,
                    onClick = onBuyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_OFFER_PRICE_CARD_BUTTON),
                )
            } else {
                PrimaryFilledButton(
                    text = buyButtonText,
                    onClick = onBuyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_OFFER_PRICE_CARD_BUTTON),
                )
            }
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
internal fun DiscountBadge(
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
            discountDescriptionText = "Billed at €4.99/month for the first year, €9.99/month after",
            discountBadgeText = "Black Friday · 50% off",
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
private fun OfferPriceCardYearlyPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        OfferPriceCard(
            planName = "Pro I",
            priceText = "€59.88/year",
            originalPriceText = "€120",
            discountDescriptionText = "Billed at €59.88 for the first year, €120 charged yearly after",
            discountBadgeText = "Black Friday · 50% off",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            buyButtonText = "Get Pro I",
            onBuyClick = {},
            monthlyPriceText = "€4.99/month",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OfferPriceCardBrandButtonPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        OfferPriceCard(
            planName = "Pro I",
            priceText = "€4.99/month",
            originalPriceText = "€9.99",
            discountDescriptionText = "Billed at €4.99/month for the first year, €9.99/month after",
            discountBadgeText = "Black Friday · 50% off",
            storageText = "2 TB cloud storage",
            transferText = "2 TB transfer",
            buyButtonText = "Get Pro I",
            onBuyClick = {},
            useBrandButton = true,
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
 * Tag for the OfferPriceCard billing row (strikethrough original price and discounted price)
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
