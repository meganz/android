package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.card.PlanFeature
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as shareR

/**
 * Composable function to display a card for the Free plan.
 */
@Composable
fun FreePlanCard(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val features = remember {
        listOf(
            PlanFeature(
                icon = IconPackR.drawable.ic_cloud,
                title = context.getString(shareR.string.free_plan_card_storage_feature)
            ),
            PlanFeature(
                icon = IconPackR.drawable.ic_arrows_up_down,
                title = context.getString(shareR.string.free_plan_card_transfer_feature)
            ),
        )
    }
    Column(
        modifier = modifier
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.strong,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(LocalSpacing.current.x16)
            .fillMaxWidth()
            .testTag(TEST_TAG_FREE_PLAN_CARD),
        horizontalAlignment = Alignment.Start,
    ) {
        MegaText(
            text = stringResource(id = shareR.string.free_plan_card_title),
            style = MaterialTheme.typography.titleMedium,
            textColor = TextColor.Primary,
            modifier = Modifier.testTag(TEST_TAG_FREE_PLAN_CARD_TITLE)
        )
        MegaText(
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(TEST_TAG_FREE_PLAN_CARD_DESCRIPTION),
            text = stringResource(id = shareR.string.free_plan_card_description),
            style = MaterialTheme.typography.bodySmall,
            textColor = TextColor.Secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        features.forEach { feature ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                MegaIcon(
                    painter = painterResource(id = feature.icon),
                    tint = IconColor.Brand,
                    modifier = Modifier.size(24.dp),
                )
                MegaText(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    textColor = TextColor.Primary,
                )
            }
        }
        SecondaryFilledButton(
            text = stringResource(id = shareR.string.free_plan_card_button),
            onClick = onContinue,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .fillMaxWidth()
                .testTag(TEST_TAG_FREE_PLAN_CARD_BUTTON)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FreePlanCardPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        FreePlanCard(
            modifier = Modifier.padding(16.dp),
            onContinue = {}
        )
    }
}

/**
 * Tag for the FreePlanCard root container
 */
const val TEST_TAG_FREE_PLAN_CARD = "free_plan_card"

/**
 * Tag for the FreePlanCard title text
 */
const val TEST_TAG_FREE_PLAN_CARD_TITLE = "free_plan_card:title"

/**
 * Tag for the FreePlanCard description text
 */
const val TEST_TAG_FREE_PLAN_CARD_DESCRIPTION = "free_plan_card:description"

/**
 * Tag for the FreePlanCard action button
 */
const val TEST_TAG_FREE_PLAN_CARD_BUTTON = "free_plan_card:button"