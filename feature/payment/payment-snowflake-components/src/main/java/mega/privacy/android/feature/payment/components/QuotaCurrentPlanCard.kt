package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Card showing the user's current plan and their usage on the quota-warning upsell screen:
 * plan name, a "Current plan" label, a usage progress bar, and the usage help text.
 *
 * @param planName the current plan name (e.g. "Free")
 * @param currentPlanLabel label shown after the plan name (e.g. "Current plan")
 * @param usagePercentage current usage as a 0..100 value
 * @param usageLevel severity level driving the bar colour
 * @param usageText usage help text (e.g. "Storage: 19 GB out of 20 GB")
 */
@Composable
fun QuotaCurrentPlanCard(
    planName: String,
    currentPlanLabel: String,
    usagePercentage: Float,
    usageLevel: QuotaUsageLevel,
    usageText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = DSTokens.colors.background.surface1,
                shape = RoundedCornerShape(16.dp),
            )
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.strong,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp)
            .testTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MegaText(
                text = planName,
                style = MaterialTheme.typography.titleMedium,
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_QUOTA_CURRENT_PLAN_NAME),
            )
            MegaText(
                text = "•",
                style = MaterialTheme.typography.titleMedium,
                textColor = TextColor.Primary,
            )
            MegaText(
                text = currentPlanLabel,
                style = MaterialTheme.typography.bodyMedium,
                textColor = TextColor.Secondary,
                modifier = Modifier.testTag(TEST_TAG_QUOTA_CURRENT_PLAN_LABEL),
            )
        }
        QuotaUsageProgressBar(
            percentage = usagePercentage,
            level = usageLevel,
            modifier = Modifier.testTag(TEST_TAG_QUOTA_CURRENT_PLAN_PROGRESS),
        )
        MegaText(
            text = usageText,
            style = MaterialTheme.typography.bodyMedium,
            textColor = TextColor.Secondary,
            modifier = Modifier.testTag(TEST_TAG_QUOTA_CURRENT_PLAN_USAGE),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun QuotaCurrentPlanCardPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        QuotaCurrentPlanCard(
            planName = "Free",
            currentPlanLabel = "Current plan",
            usagePercentage = 95f,
            usageLevel = QuotaUsageLevel.Warning,
            usageText = "Storage: 19 GB out of 20 GB",
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Tag for the QuotaCurrentPlanCard root container
 */
const val TEST_TAG_QUOTA_CURRENT_PLAN_CARD = "quota_current_plan_card"

/**
 * Tag for the QuotaCurrentPlanCard plan name
 */
const val TEST_TAG_QUOTA_CURRENT_PLAN_NAME = "quota_current_plan_card:name"

/**
 * Tag for the QuotaCurrentPlanCard "Current plan" label
 */
const val TEST_TAG_QUOTA_CURRENT_PLAN_LABEL = "quota_current_plan_card:label"

/**
 * Tag for the QuotaCurrentPlanCard usage progress bar
 */
const val TEST_TAG_QUOTA_CURRENT_PLAN_PROGRESS = "quota_current_plan_card:progress"

/**
 * Tag for the QuotaCurrentPlanCard usage help text
 */
const val TEST_TAG_QUOTA_CURRENT_PLAN_USAGE = "quota_current_plan_card:usage"
