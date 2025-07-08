package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.SecondaryNavigationIconButton
import mega.android.core.ui.components.text.SecondaryTopNavigationButton
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.resources.R

/**
 * Composable function to display the top bar for the Choose Account screen.
 */
@Composable
fun ChooseAccountScreenTopBar(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    isUpgradeAccount: Boolean,
    maybeLaterClicked: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = DSTokens.colors.background.pageBackground.copy(alpha = alpha))
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp)
    ) {
        if (isUpgradeAccount) {
            SecondaryNavigationIconButton(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag(TEST_TAG_BACK_BUTTON),
                icon = painterResource(mega.android.core.ui.R.drawable.ic_arrow_left_medium_thin_outline),
                onClick = onBack,
            )
        } else {
            SecondaryTopNavigationButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .testTag(TEST_TAG_MAYBE_LATER_BUTTON),
                text = stringResource(R.string.choose_account_screen_maybe_later_button_text),
                onClick = maybeLaterClicked,
            )
        }
    }
}

internal const val TEST_TAG_BACK_BUTTON = "choose_account_screen_top_bar:back_button"
internal const val TEST_TAG_MAYBE_LATER_BUTTON = "choose_account_screen_top_bar:maybe_later_button"