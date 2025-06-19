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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = DSTokens.colors.background.pageBackground.copy(alpha = alpha))
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp)
    ) {
        SecondaryTopNavigationButton(
            modifier = Modifier.align(
                Alignment.CenterEnd
            ),
            text = stringResource(R.string.choose_account_screen_maybe_later_button_text),
            onClick = onClick,
        )
    }
}