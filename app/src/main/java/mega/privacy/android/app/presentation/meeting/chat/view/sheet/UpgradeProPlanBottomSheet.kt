package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.drawableId
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary
import mega.privacy.mobile.analytics.event.MaxCallDurationReachedModalEvent
import mega.privacy.mobile.analytics.event.UpgradeToProToGetUnlimitedCallsDialogEvent

/**
 * Composable function to show the bottom sheet to upgrade to Pro plan.
 */
@Composable
fun UpgradeProPlanBottomSheet(
    modifier: Modifier = Modifier,
    hideSheet: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(UpgradeToProToGetUnlimitedCallsDialogEvent)
    }

    val context = LocalContext.current
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.background)
    ) {
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
        Text(
            text = stringResource(id = R.string.meetings_upgrade_pro_plan_title),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorPrimary,
            style = MaterialTheme.typography.subtitle1medium
        )
        Spacer(modifier = Modifier.padding(top = 20.dp))
        Text(
            text = stringResource(id = R.string.meetings_upgrade_pro_plan_body),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorSecondary,
            style = MaterialTheme.typography.caption,
        )
        Spacer(modifier = Modifier.padding(top = 32.dp))
        RaisedDefaultMegaButton(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            textId = R.string.meetings_upgrade_pro_plan_button,
            onClick = {
                Analytics.tracker.trackEvent(MaxCallDurationReachedModalEvent)
                context.startActivity(
                    Intent(
                        context,
                        UpgradeAccountActivity::class.java
                    )
                )
                hideSheet()
            }
        )
        Spacer(modifier = Modifier.padding(top = 24.dp))
    }
}


@CombinedThemePreviews
@Composable
private fun UpgradeProPlanBottomSheetPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        UpgradeProPlanBottomSheet()
    }
}

internal const val UPGRADE_IMAGE_TEST_TAG = "meetings_upgrade_pro_plan:image_rocket"
