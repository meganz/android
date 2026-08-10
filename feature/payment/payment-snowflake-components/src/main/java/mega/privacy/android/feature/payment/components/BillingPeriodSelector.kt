package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Pill segmented control to switch between Monthly and Yearly billing on the redesigned
 * subscription page. The Yearly segment shows an inline savings label.
 *
 * @param isMonthly whether the Monthly segment is selected
 * @param onPeriodSelected called with true when Monthly is tapped, false when Yearly is tapped
 * @param monthlyLabel label for the Monthly segment
 * @param yearlyLabel label for the Yearly segment
 * @param saveLabel savings label shown next to the Yearly label (e.g. "Save up to 16%")
 */
@Composable
fun BillingPeriodSelector(
    isMonthly: Boolean,
    onPeriodSelected: (isMonthly: Boolean) -> Unit,
    monthlyLabel: String,
    yearlyLabel: String,
    saveLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = DSTokens.colors.background.surface2,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BillingPeriodSegment(
                selected = isMonthly,
                onClick = { onPeriodSelected(true) },
                modifier = Modifier.testTag(TEST_TAG_BILLING_PERIOD_MONTHLY),
            ) {
                SegmentLabel(text = monthlyLabel, selected = isMonthly)
            }
            BillingPeriodSegment(
                selected = !isMonthly,
                onClick = { onPeriodSelected(false) },
                modifier = Modifier.testTag(TEST_TAG_BILLING_PERIOD_YEARLY),
            ) {
                SegmentLabel(text = yearlyLabel, selected = !isMonthly)
                MegaText(
                    text = saveLabel,
                    style = MaterialTheme.typography.titleSmall,
                    textColor = TextColor.Brand,
                    modifier = Modifier.testTag(TEST_TAG_BILLING_PERIOD_SAVE_LABEL),
                )
            }
        }
    }
}

@Composable
private fun BillingPeriodSegment(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                color = if (selected) {
                    DSTokens.colors.background.pageBackground
                } else {
                    DSTokens.colors.background.surface2
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun SegmentLabel(text: String, selected: Boolean) {
    MegaText(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        ),
        textColor = TextColor.Primary,
    )
}

@CombinedThemePreviews
@Composable
private fun BillingPeriodSelectorMonthlyPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        BillingPeriodSelector(
            isMonthly = true,
            onPeriodSelected = {},
            monthlyLabel = "Monthly",
            yearlyLabel = "Yearly",
            saveLabel = "Save up to 16%",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun BillingPeriodSelectorYearlyPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        BillingPeriodSelector(
            isMonthly = false,
            onPeriodSelected = {},
            monthlyLabel = "Monthly",
            yearlyLabel = "Yearly",
            saveLabel = "Save up to 16%",
        )
    }
}

/**
 * Tag for the Monthly segment of the billing period selector
 */
const val TEST_TAG_BILLING_PERIOD_MONTHLY = "billing_period_selector:monthly"

/**
 * Tag for the Yearly segment of the billing period selector
 */
const val TEST_TAG_BILLING_PERIOD_YEARLY = "billing_period_selector:yearly"

/**
 * Tag for the savings label of the billing period selector
 */
const val TEST_TAG_BILLING_PERIOD_SAVE_LABEL = "billing_period_selector:save_label"
