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
 * Card showing the user's current plan on the redesigned subscription page.
 *
 * @param currentPlanLabel label above the plan (e.g. "Current plan")
 * @param planName the current plan name (e.g. "Pro I")
 * @param cycleText the billing cycle or one-off period text (e.g. "Yearly subscription" or "12 months")
 * @param helpText supplementary text (e.g. "Renews on 8 July 2027" or "Expires on 8 July 2027"),
 * null when no renewal/expiry date is available
 * @param expiringLabel badge text shown next to [currentPlanLabel] when the plan expires soon (e.g.
 * "Expiring"), null/blank to hide the badge
 */
@Composable
fun CurrentPlanCard(
    currentPlanLabel: String,
    planName: String,
    cycleText: String,
    modifier: Modifier = Modifier,
    helpText: String? = null,
    expiringLabel: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = DSTokens.colors.background.surface1,
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.strong,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(16.dp)
            .testTag(TEST_TAG_CURRENT_PLAN_CARD),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MegaText(
                text = currentPlanLabel,
                style = MaterialTheme.typography.titleSmall,
                textColor = TextColor.Secondary,
                modifier = Modifier.testTag(TEST_TAG_CURRENT_PLAN_LABEL),
            )
            if (!expiringLabel.isNullOrBlank()) {
                ExpiringBadge(text = expiringLabel)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MegaText(
                text = planName,
                style = MaterialTheme.typography.titleMedium,
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_CURRENT_PLAN_NAME),
            )
            MegaText(
                text = "•",
                style = MaterialTheme.typography.titleMedium,
                textColor = TextColor.Primary,
            )
            MegaText(
                text = cycleText,
                style = MaterialTheme.typography.titleMedium,
                textColor = TextColor.Secondary,
                modifier = Modifier.testTag(TEST_TAG_CURRENT_PLAN_CYCLE),
            )
        }
        if (!helpText.isNullOrEmpty()) {
            MegaText(
                text = helpText,
                style = MaterialTheme.typography.bodySmall,
                textColor = TextColor.Secondary,
                modifier = Modifier.testTag(TEST_TAG_CURRENT_PLAN_HELP_TEXT),
            )
        }
    }
}

@Composable
private fun ExpiringBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    MegaText(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        textColor = TextColor.Error,
        modifier = modifier
            .background(
                color = DSTokens.colors.notifications.notificationError,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag(TEST_TAG_CURRENT_PLAN_EXPIRING_BADGE),
    )
}

@CombinedThemePreviews
@Composable
private fun CurrentPlanCardPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        CurrentPlanCard(
            currentPlanLabel = "Current plan",
            planName = "Pro I",
            cycleText = "Yearly subscription",
            helpText = "Renews on 8 July 2027",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CurrentPlanCardOneOffPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        CurrentPlanCard(
            currentPlanLabel = "Current plan",
            planName = "Pro I",
            cycleText = "12 months",
            helpText = "Expires on 8 July 2027",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CurrentPlanCardExpiringPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        CurrentPlanCard(
            currentPlanLabel = "Current plan",
            planName = "Pro I",
            cycleText = "12 months",
            helpText = "Expires on 8 July 2027",
            expiringLabel = "Expiring",
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Tag for the CurrentPlanCard root container
 */
const val TEST_TAG_CURRENT_PLAN_CARD = "current_plan_card"

/**
 * Tag for the CurrentPlanCard "Current plan" label
 */
const val TEST_TAG_CURRENT_PLAN_LABEL = "current_plan_card:label"

/**
 * Tag for the CurrentPlanCard plan name
 */
const val TEST_TAG_CURRENT_PLAN_NAME = "current_plan_card:name"

/**
 * Tag for the CurrentPlanCard billing cycle text
 */
const val TEST_TAG_CURRENT_PLAN_CYCLE = "current_plan_card:cycle"

/**
 * Tag for the CurrentPlanCard help text (renews/expires)
 */
const val TEST_TAG_CURRENT_PLAN_HELP_TEXT = "current_plan_card:help_text"

/**
 * Tag for the CurrentPlanCard "Expiring" badge
 */
const val TEST_TAG_CURRENT_PLAN_EXPIRING_BADGE = "current_plan_card:expiring_badge"
