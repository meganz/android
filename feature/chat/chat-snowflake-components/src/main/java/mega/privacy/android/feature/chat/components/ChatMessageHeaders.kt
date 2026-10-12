package mega.privacy.android.feature.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * Centred header that separates the chat messages of different days.
 *
 * @param dateString Formatted date, for example "Friday, Nov 14, 2023".
 * @param modifier [Modifier]
 */
@Composable
fun DateHeader(
    dateString: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        MegaText(
            text = dateString,
            textColor = TextColor.Secondary,
            style = AppTheme.typography.titleSmall,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 16.dp)
                .align(Alignment.Center)
                .testTag(DATE_HEADER_TAG),
        )
    }
}

/**
 * Header shown above a group of chat messages, with the optional sender name and the time.
 *
 * Own messages align the header to the end. Other messages align it to the start, after the
 * avatar column.
 *
 * @param timeString Formatted time of the message.
 * @param displayAsMine True to align the header as an own message.
 * @param userName Name of the sender, or null to show the time only.
 * @param modifier [Modifier]
 */
@Composable
fun TimeHeader(
    timeString: String,
    displayAsMine: Boolean,
    userName: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (displayAsMine) 0.dp else 48.dp,
                end = if (displayAsMine) 16.dp else 0.dp,
            ),
        horizontalArrangement = if (displayAsMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        userName?.let {
            MegaText(
                text = it,
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag(TIME_HEADER_USER_NAME_TAG),
            )
        }
        MegaText(
            text = timeString,
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodySmall,
            modifier = Modifier.testTag(TIME_HEADER_TIME_TAG),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DateHeaderPreview() {
    AndroidThemeForPreviews {
        DateHeader(dateString = "Friday, Nov 14, 2023")
    }
}

@CombinedThemePreviews
@Composable
private fun TimeHeaderPreview(
    @PreviewParameter(BooleanProvider::class) isMine: Boolean,
) {
    AndroidThemeForPreviews {
        TimeHeader(
            timeString = "10:15",
            displayAsMine = isMine,
            userName = "John Doe",
        )
    }
}

/**
 * Test tag for the date header text.
 */
const val DATE_HEADER_TAG = "date_header:date"

/**
 * Test tag for the sender name in the time header.
 */
const val TIME_HEADER_USER_NAME_TAG = "time_header:user_name"

/**
 * Test tag for the time in the time header.
 */
const val TIME_HEADER_TIME_TAG = "time_header:time"
