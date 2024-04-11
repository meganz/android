package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.view.SKIP_BUTTON
import mega.privacy.android.app.upgradeAccount.view.VIEW_PRO_PLAN_BUTTON
import mega.privacy.android.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_900
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Composable UI for Buttons Row to use on Onboarding dialog for Variant A
 */
@Composable
fun ButtonsRow(
    onSkipPressed: () -> Unit,
    onViewPlansPressed: () -> Unit,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            )
            .placeholder(
                color = MaterialTheme.colors.grey_020_grey_900,
                shape = RoundedCornerShape(4.dp),
                highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                visible = isLoading,
            ),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedMegaButton(
            textId = R.string.general_skip,
            onClick = onSkipPressed,
            modifier = Modifier
                .padding(end = 8.dp)
                .testTag(SKIP_BUTTON),
            rounded = false,
        )
        RaisedDefaultMegaButton(
            textId = R.string.dialog_onboarding_button_view_pro_plan,
            onClick = onViewPlansPressed,
            modifier = Modifier.testTag(VIEW_PRO_PLAN_BUTTON)
        )
    }
}

@CombinedThemePreviews
@Composable
fun ButtonsRowPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ButtonsRow(
            onSkipPressed = {},
            onViewPlansPressed = {},
            isLoading = false
        )
    }
}