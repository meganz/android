package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Composable function to display the bottom bar for the Buy Plan screen.
 */
@Composable
fun BuyPlanBottomBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    text: String,
) {
    Box(
        modifier = modifier
            .background(color = DSTokens.colors.background.pageBackground)
            .navigationBarsPadding()
            .fillMaxWidth(),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = DSTokens.colors.border.strong
        )
        PrimaryFilledButton(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            text = text,
            onClick = onClick,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun BuyPlanBottomBarPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        BuyPlanBottomBar(
            text = "Buy now",
        )
    }
}