package mega.privacy.android.shared.original.core.ui.controls.banners

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Warning banner with two actions compose view
 */
@Composable
fun ActionBanner(
    mainText: String,
    leftActionText: String,
    leftActionClicked: () -> Unit,
    modifier: Modifier = Modifier,
    rightActionText: String? = null,
    rightActionClicked: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        MegaText(
            text = mainText,
            textColor = TextColor.Primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextMegaButton(
                text = leftActionText,
                onClick = leftActionClicked,
                modifier = Modifier.padding(end = 8.dp)
            )

            if (rightActionText != null && rightActionClicked != null) {
                TextMegaButton(
                    text = rightActionText,
                    onClick = rightActionClicked,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
internal fun PreviewTwoActionsBanner() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ActionBanner(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
            mainText = "Battery optimisation permission allow MEGA to run " +
                    "in the background. You can change this any time going to " +
                    "Settings -> App.",
            leftActionText = "Learn more",
            rightActionText = "Allow",
            leftActionClicked = {},
            rightActionClicked = {}
        )
    }
}