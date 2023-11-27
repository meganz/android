package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Return to call banner
 *
 * @param text Text to show.
 * @param modifier [Modifier]
 */
@Composable
fun ReturnToCallBanner(
    text: String,
    onBannerClicked: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .background(MegaTheme.colors.button.primary)
        .clickable { onBannerClicked() },
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2
            .copy(color = MegaTheme.colors.background.pageBackground),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(TEST_TAG_RETURN_TO_CALL)
    )
}

@CombinedThemePreviews
@Composable
private fun ReturnToCallBannerPreview() {
    AndroidThemeForPreviews {
        ReturnToCallBanner(
            text = "Return to call",
            onBannerClicked = {},
        )
    }
}

internal const val TEST_TAG_RETURN_TO_CALL = "chat_view:return_to_call"