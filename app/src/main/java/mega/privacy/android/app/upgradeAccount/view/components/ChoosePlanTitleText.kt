package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Composable UI for Title Choose the right plan to reuse on different screens
 *
 * @param testTag   String to tag the composable for the unit- tests
 */
@Composable
internal fun ChoosePlanTitleText(testTag: String) {
    MegaText(
        text = stringResource(id = R.string.account_upgrade_account_title_choose_right_plan),
        textColor = TextColor.Primary,
        style = MaterialTheme.typography.subtitle1medium,
        modifier = Modifier
            .padding(
                start = 24.dp,
                top = 8.dp,
                bottom = 24.dp
            )
            .testTag("${testTag}title"),
    )
}