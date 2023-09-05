package mega.privacy.android.core.ui.controls.banners

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Warning banner with two actions compose view
 */
@Composable
fun TwoActionsBanner(
    mainText: String,
    leftActionText: String,
    rightActionText: String,
    leftActionClicked: () -> Unit,
    rightActionClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = mainText,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.body2,
            color = MegaTheme.colors.textPrimary
        )
        Row(Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            TextMegaButton(
                text = leftActionText,
                onClick = leftActionClicked,
                modifier = Modifier.padding(end = 8.dp)
            )

            TextMegaButton(
                text = rightActionText,
                onClick = rightActionClicked,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@CombinedThemePreviews
@Composable
internal fun PreviewTwoActionsBanner() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TwoActionsBanner(
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