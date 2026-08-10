package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.drawableId
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.mobile.analytics.event.MaxCallDurationReachedModalEvent
import mega.privacy.mobile.analytics.event.UpgradeToProToGetUnlimitedCallsDialogEvent

/**
 * Composable function to show the bottom sheet to upgrade to Pro plan.
 */
@Composable
fun UpgradeProPlanBottomSheet(
    modifier: Modifier = Modifier,
    onUpgradeToProPlan: () -> Unit = {},
    hideSheet: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(UpgradeToProToGetUnlimitedCallsDialogEvent)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_rocket),
                contentDescription = "Upgrade to Pro Plan Image",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 32.dp)
                    .width(90.dp)
                    .height(90.dp)
                    .semantics { drawableId = R.drawable.ic_rocket }
                    .testTag(UPGRADE_IMAGE_TEST_TAG)
            )
        }
        MegaText(
            text = stringResource(id = R.string.meetings_upgrade_pro_plan_title),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.padding(top = 20.dp))
        MegaText(
            text = stringResource(id = R.string.meetings_upgrade_pro_plan_body),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.padding(top = 32.dp))
        PrimaryFilledButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.meetings_upgrade_pro_plan_button),
            onClick = {
                Analytics.tracker.trackEvent(MaxCallDurationReachedModalEvent)
                hideSheet()
                onUpgradeToProPlan()
            }
        )
        Spacer(modifier = Modifier.padding(top = 24.dp))
        Spacer(
            Modifier.windowInsetsBottomHeight(
                WindowInsets.systemBars
            )
        )
    }
}


@CombinedThemePreviews
@Composable
private fun UpgradeProPlanBottomSheetPreview() {
    AndroidThemeForPreviews {
        UpgradeProPlanBottomSheet()
    }
}

internal const val UPGRADE_IMAGE_TEST_TAG = "meetings_upgrade_pro_plan:image_rocket"
