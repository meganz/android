package mega.privacy.android.app.upgradeAccount.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary

internal const val IMAGE_TAG = "onboarding_screen_variant_a:image"
internal const val PRO_PLAN_TITLE = "onboarding_screen_variant_a:pro_plan_title"
internal const val PRO_PLAN_TEXT = "onboarding_screen_variant_a:pro_plan_description"
internal const val FEATURE_TITLE = "onboarding_screen_variant_a:feature_title"
internal const val STORAGE_DESCRIPTION_ROW = "onboarding_screen_variant_a:storage_description_row"
internal const val TRANSFER_DESCRIPTION_ROW = "onboarding_screen_variant_a:transfer_description_row"
internal const val SECURITY_DESCRIPTION_ROW = "onboarding_screen_variant_a:security_description_row"
internal const val SKIP_BUTTON = "onboarding_screen_variant_a:skip_button"
internal const val VIEW_PRO_PLAN_BUTTON = "onboarding_screen_variant_a:view_pro_plan_button"

/**
 *  Compose UI for new Onboarding dialog (Choose account screen), this is Variant A
 *  User will see this account when the registration was finished and user signs in for the first time ever
 */
@Composable
fun VariantAOnboardingDialogView(
    onSkipPressed: () -> Unit,
    onViewPlansPressed: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp)
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.image_upselling_onboarding_dialog),
            contentDescription = "",
            modifier = Modifier.testTag(IMAGE_TAG)
        )
        Spacer(modifier = Modifier.height(16.dp))
        GetProPlanColumn()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Some features to look forward to are:",
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorPrimary,
            modifier = Modifier.testTag(FEATURE_TITLE),
        )
        Spacer(modifier = Modifier.height(16.dp))
        //Storage
        FeatureRow(
            drawableID = painterResource(id = R.drawable.ic_storage_onboarding_dialog),
            title = "Generous storage",
            description = "Store unlimited data, starting from [A].",
            testTag = STORAGE_DESCRIPTION_ROW,
        )
        //Transfer
        FeatureRow(
            drawableID = painterResource(id = R.drawable.ic_transfer_onboarding_dialog),
            title = "Transfer sharing",
            description = "The people you share data with can use your transfer quota to download and stream the items you shared.",
            testTag = TRANSFER_DESCRIPTION_ROW,
        )
        //Security
        FeatureRow(
            drawableID = painterResource(id = R.drawable.ic_security_onboarding_dialog),
            title = "Additional security when sharing",
            description = "Set passwords and expiry dates for file and folder links.",
            testTag = SECURITY_DESCRIPTION_ROW,
        )
        Spacer(modifier = Modifier.height(60.dp))
        ButtonsRow(
            onSkipPressed = onSkipPressed,
            onViewPlansPressed = onViewPlansPressed,
        )
    }
}

@Composable
fun GetProPlanColumn() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Get more with a Pro plan",
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorPrimary,
            modifier = Modifier.testTag(PRO_PLAN_TITLE),
        )
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(PRO_PLAN_TEXT),
            text = "Upgrade to a Pro plan for more storage and lots of extra features. Our plans start at [A] a month.",
            style = MaterialTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorPrimary,
        )
    }
}

@Composable
fun ButtonsRow(
    onSkipPressed: () -> Unit,
    onViewPlansPressed: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            ),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedMegaButton(
            textId = R.string.general_skip,
            onClick = onSkipPressed,
            modifier = Modifier
                .padding(end = 8.dp)
                .testTag(SKIP_BUTTON),
        )
        RaisedDefaultMegaButton(
            textId = R.string.upgrade_pro,
            onClick = onViewPlansPressed,
            modifier = Modifier.testTag(VIEW_PRO_PLAN_BUTTON)
        )
    }
}

@CombinedThemePreviews
@Composable
fun PreviewVariantAOnboardingDialogView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        VariantAOnboardingDialogView(
            onSkipPressed = {},
            onViewPlansPressed = {},
        )
    }
}