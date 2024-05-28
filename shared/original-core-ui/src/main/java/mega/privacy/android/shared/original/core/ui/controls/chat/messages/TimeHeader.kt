package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4


/**
 * Time header
 *
 * @param timeString Time string
 * @param displayAsMine Display as mine
 * @param userName User name
 * @param modifier
 */
@Composable
fun TimeHeader(
    timeString: String,
    displayAsMine: Boolean,
    userName: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = if (displayAsMine) modifier
            .fillMaxWidth()
            .padding(end = 16.dp) else modifier
            .fillMaxWidth()
            .padding(start = 48.dp),
        horizontalArrangement = if (displayAsMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        userName?.let {
            Text(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag(TEST_TAG_TIME_HEADER_USER_NAME),
                text = it,
                style = MaterialTheme.typography.caption,
                color = MegaOriginalTheme.colors.text.primary
            )
        }
        Text(
            modifier = Modifier.testTag(TEST_TAG_TIME_HEADER_TIME),
            text = timeString,
            style = MaterialTheme.typography.body4,
            color = MegaOriginalTheme.colors.text.secondary
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewTimeHeader(
    @PreviewParameter(BooleanProvider::class) isMine: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TimeHeader(
            timeString = "Friday, Nov 14, 2023",
            displayAsMine = isMine,
            userName = "John Doe"
        )
    }
}

internal const val TEST_TAG_TIME_HEADER_USER_NAME = "time_header:user_name"
internal const val TEST_TAG_TIME_HEADER_TIME = "time_header:time"