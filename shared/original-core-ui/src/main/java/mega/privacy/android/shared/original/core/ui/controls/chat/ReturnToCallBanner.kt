package mega.privacy.android.shared.original.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.meetings.CallChronometer
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempThemeForPreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import kotlin.time.Duration

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
    duration: Duration? = null,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .background(MegaOriginalTheme.colors.button.primary)
        .clickable { onBannerClicked() },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
) {
    val textStyle =
        MaterialTheme.typography.body2.copy(color = MegaOriginalTheme.colors.text.inverse)
    Text(
        text = text,
        style = textStyle,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(TEST_TAG_RETURN_TO_CALL)
    )
    duration?.let {
        CallChronometer(
            modifier = Modifier.testTag(TEST_TAG_RETURN_TO_CALL_CHRONOMETER),
            duration = it,
            textStyle = textStyle
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ReturnToCallBannerPreview() {
    OriginalTempThemeForPreviews {
        ReturnToCallBanner(
            text = "Return to call",
            onBannerClicked = {},
        )
    }
}

/**
 * Test tag for ReturnToCallBanner
 */
const val TEST_TAG_RETURN_TO_CALL = "chat_view:return_to_call"

/**
 * Test tag for ReturnToCallBanner chronometer
 */
const val TEST_TAG_RETURN_TO_CALL_CHRONOMETER = "chat_view:return_to_call_chronometer"