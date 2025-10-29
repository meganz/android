package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.components.button.MegaRadioButton
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.resources.R as shareR

/**
 * Composable function to display a card for the Pro plan.
 */
@Composable
fun ProPlanCard(
    modifier: Modifier = Modifier,
    planName: String,
    isRecommended: Boolean,
    isSelected: Boolean,
    storage: String,
    transfer: String,
    price: String,
    billingInfo: String?,
    offerName: String? = null,
    discountedPrice: String? = null,
    isCurrentPlan: Boolean = false,
    onSelected: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) DSTokens.colors.border.strongSelected else DSTokens.colors.border.strong,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp)
            .clickable(enabled = !isCurrentPlan, onClick = onSelected)
            .fillMaxWidth()
            .testTag(TEST_TAG_PRO_PLAN_CARD),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            MegaText(
                text = planName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_PRO_PLAN_CARD_TITLE)
            )
            if (isCurrentPlan) {
                MegaText(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(
                            color = DSTokens.colors.notifications.notificationWarning,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag(TEST_TAG_PRO_PLAN_CARD_CURRENT_PLAN),
                    text = stringResource(shareR.string.account_upgrade_account_pro_plan_info_current_plan_label),
                    style = MaterialTheme.typography.bodySmall,
                    textColor = TextColor.Warning
                )
            } else if (!offerName.isNullOrEmpty()) {
                MegaText(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(
                            color = DSTokens.colors.brand.default,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag(TEST_TAG_PRO_PLAN_CARD_OFFER),
                    text = offerName,
                    style = MaterialTheme.typography.bodySmall,
                    textColor = TextColor.OnColor
                )
            } else if (isRecommended) {
                MegaText(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(
                            color = DSTokens.colors.notifications.notificationInfo,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag(TEST_TAG_PRO_PLAN_CARD_RECOMMENDED),
                    text = stringResource(shareR.string.account_upgrade_account_pro_plan_info_recommended_label),
                    style = MaterialTheme.typography.bodySmall,
                    textColor = TextColor.Info
                )
            }
            Spacer(Modifier.weight(1f))
            if (!isCurrentPlan) {
                MegaRadioButton(
                    selected = isSelected,
                    onOptionSelected = { onSelected() },
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(TEST_TAG_PRO_PLAN_CARD_RADIO),
                    identifier = planName
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                MegaText(
                    text = storage,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Primary,
                    modifier = Modifier.testTag(TEST_TAG_PRO_PLAN_CARD_STORAGE)
                )
                MegaText(
                    text = transfer,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Primary,
                    modifier = Modifier.testTag(TEST_TAG_PRO_PLAN_CARD_TRANSFER)
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Show original price with strikethrough if there's a discount
                val actualPrice = discountedPrice ?: price
                if (discountedPrice != null) {
                    MegaText(
                        text = price,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            textDecoration = TextDecoration.LineThrough
                        ),
                        textColor = TextColor.Secondary,
                        modifier = Modifier.testTag(TEST_TAG_PRO_PLAN_CARD_ORIGINAL_PRICE)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    MegaText(
                        text = actualPrice,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(TEST_TAG_PRO_PLAN_CARD_PRICE)
                    )
                    MegaText(
                        text = stringResource(shareR.string.general_month),
                        style = MaterialTheme.typography.bodyMedium,
                        textColor = TextColor.Secondary,
                        modifier = Modifier
                            .padding(start = 2.dp, bottom = 2.dp)
                            .testTag(TEST_TAG_PRO_PLAN_CARD_PRICE_UNIT)
                    )
                }
                if (!billingInfo.isNullOrEmpty()) {
                    SpannedText(
                        value = billingInfo,
                        baseTextColor = TextColor.Secondary,
                        baseStyle = MaterialTheme.typography.bodySmall,
                        spanStyles = mapOf(
                            SpanIndicator('A') to MegaSpanStyle.DefaultColorStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                        ),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .testTag(TEST_TAG_PRO_PLAN_CARD_BILLING_INFO)
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ProPlanCardPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        Column {
            // Card with discount
            ProPlanCard(
                modifier = Modifier.padding(16.dp),
                planName = "Pro I",
                isRecommended = false,
                isSelected = true,
                storage = "2 TB storage",
                transfer = "24 TB transfer",
                price = "€9.99",
                billingInfo = "€99.99 $49.99 for first year",
                offerName = "Black Friday offer",
                discountedPrice = "€4.99",
                isCurrentPlan = false
            )

            // Card without discount
            ProPlanCard(
                modifier = Modifier.padding(16.dp),
                planName = "Pro II",
                isRecommended = true,
                isSelected = false,
                storage = "8 TB storage",
                transfer = "96 TB transfer",
                price = "€16.67",
                billingInfo = "€199.99 billed yearly",
                isCurrentPlan = false
            )

            // Current plan
            ProPlanCard(
                modifier = Modifier.padding(16.dp),
                planName = "Pro II",
                isRecommended = false,
                isSelected = false,
                storage = "8 TB storage",
                transfer = "96 TB transfer",
                price = "€16.67",
                billingInfo = null,
                isCurrentPlan = true
            )
        }
    }
}

/**
 * Tag for the ProPlanCard root container
 */
const val TEST_TAG_PRO_PLAN_CARD = "pro_plan_card"

/**
 * Tag for the ProPlanCard title text
 */
const val TEST_TAG_PRO_PLAN_CARD_TITLE = "${TEST_TAG_PRO_PLAN_CARD}:title"

/**
 * Tag for the ProPlanCard recommended label
 */
const val TEST_TAG_PRO_PLAN_CARD_RECOMMENDED = "${TEST_TAG_PRO_PLAN_CARD}:recommended"

/**
 * Tag for the ProPlanCard radio button
 */
const val TEST_TAG_PRO_PLAN_CARD_RADIO = "${TEST_TAG_PRO_PLAN_CARD}:radio"

/**
 * Tag for the ProPlanCard storage text
 */
const val TEST_TAG_PRO_PLAN_CARD_STORAGE = "${TEST_TAG_PRO_PLAN_CARD}:storage"

/**
 * Tag for the ProPlanCard transfer text
 */
const val TEST_TAG_PRO_PLAN_CARD_TRANSFER = "${TEST_TAG_PRO_PLAN_CARD}:transfer"

/**
 * Tag for the ProPlanCard price text
 */
const val TEST_TAG_PRO_PLAN_CARD_PRICE = "${TEST_TAG_PRO_PLAN_CARD}:price"

/**
 * Tag for the ProPlanCard price unit text
 */
const val TEST_TAG_PRO_PLAN_CARD_PRICE_UNIT = "${TEST_TAG_PRO_PLAN_CARD}:price_unit"

/**
 * Tag for the ProPlanCard billing info text
 */
const val TEST_TAG_PRO_PLAN_CARD_BILLING_INFO = "${TEST_TAG_PRO_PLAN_CARD}:billing_info"

/**
 * Tag for the ProPlanCard current plan label
 */
const val TEST_TAG_PRO_PLAN_CARD_CURRENT_PLAN = "${TEST_TAG_PRO_PLAN_CARD}:current_plan"

/**
 * Tag for the ProPlanCard offer label
 */
const val TEST_TAG_PRO_PLAN_CARD_OFFER = "${TEST_TAG_PRO_PLAN_CARD}:offer"

/**
 * Tag for the ProPlanCard original price text
 */
const val TEST_TAG_PRO_PLAN_CARD_ORIGINAL_PRICE = "${TEST_TAG_PRO_PLAN_CARD}:original_price"