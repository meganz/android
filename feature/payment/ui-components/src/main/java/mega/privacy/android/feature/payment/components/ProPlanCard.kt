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
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaRadioButton
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
    priceUnit: String,
    billingInfo: String,
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
            .clickable(onClick = onSelected)
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
            if (isRecommended) {
                MegaText(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(
                            color = DSTokens.colors.notifications.notificationInfo,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag(TEST_TAG_PRO_PLAN_CARD_RECOMMENDED),
                    text = stringResource(shareR.string.account_upgrade_account_pro_plan_info_recommended_label),
                    style = MaterialTheme.typography.bodySmall,
                    textColor = TextColor.Info
                )
            }
            Spacer(Modifier.weight(1f))
            MegaRadioButton(
                selected = isSelected,
                onOptionSelected = { onSelected() },
                modifier = Modifier.size(20.dp).testTag(TEST_TAG_PRO_PLAN_CARD_RADIO),
                identifier = planName
            )
        }
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
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
                Row(verticalAlignment = Alignment.Bottom) {
                    MegaText(
                        text = price,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textColor = TextColor.Primary,
                        modifier = Modifier.testTag(TEST_TAG_PRO_PLAN_CARD_PRICE)
                    )
                    if (priceUnit.isNotEmpty()) {
                        MegaText(
                            text = "/${priceUnit}",
                            style = MaterialTheme.typography.bodyMedium,
                            textColor = TextColor.Secondary,
                            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp).testTag(TEST_TAG_PRO_PLAN_CARD_PRICE_UNIT)
                        )
                    }
                }
                MegaText(
                    text = billingInfo,
                    style = MaterialTheme.typography.bodySmall,
                    textColor = TextColor.Secondary,
                    modifier = Modifier.padding(top = 2.dp).testTag(TEST_TAG_PRO_PLAN_CARD_BILLING_INFO)
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ProPlanCardPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        Column {
            ProPlanCard(
                modifier = Modifier.padding(16.dp),
                planName = "Pro II",
                isRecommended = true,
                isSelected = true,
                storage = "8 TB storage",
                transfer = "96 TB transfer",
                price = "€16.67",
                priceUnit = "month",
                billingInfo = "€199.99 billed yearly"
            )

            ProPlanCard(
                modifier = Modifier.padding(16.dp),
                planName = "Pro II",
                isRecommended = false,
                isSelected = false,
                storage = "8 TB storage",
                transfer = "96 TB transfer",
                price = "€16.67",
                priceUnit = "",
                billingInfo = "EUR per month"
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
const val TEST_TAG_PRO_PLAN_CARD_TITLE = "pro_plan_card:title"
/**
 * Tag for the ProPlanCard recommended label
 */
const val TEST_TAG_PRO_PLAN_CARD_RECOMMENDED = "pro_plan_card:recommended"
/**
 * Tag for the ProPlanCard radio button
 */
const val TEST_TAG_PRO_PLAN_CARD_RADIO = "pro_plan_card:radio"
/**
 * Tag for the ProPlanCard storage text
 */
const val TEST_TAG_PRO_PLAN_CARD_STORAGE = "pro_plan_card:storage"
/**
 * Tag for the ProPlanCard transfer text
 */
const val TEST_TAG_PRO_PLAN_CARD_TRANSFER = "pro_plan_card:transfer"
/**
 * Tag for the ProPlanCard price text
 */
const val TEST_TAG_PRO_PLAN_CARD_PRICE = "pro_plan_card:price"
/**
 * Tag for the ProPlanCard price unit text
 */
const val TEST_TAG_PRO_PLAN_CARD_PRICE_UNIT = "pro_plan_card:price_unit"
/**
 * Tag for the ProPlanCard billing info text
 */
const val TEST_TAG_PRO_PLAN_CARD_BILLING_INFO = "pro_plan_card:billing_info"