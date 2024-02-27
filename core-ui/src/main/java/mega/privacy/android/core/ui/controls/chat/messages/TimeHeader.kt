package mega.privacy.android.core.ui.controls.chat.messages

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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4

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
                modifier = Modifier.padding(end = 4.dp),
                text = it,
                style = MaterialTheme.typography.caption,
                color = MegaTheme.colors.text.primary
            )
        }
        Text(
            text = timeString,
            style = MaterialTheme.typography.body4,
            color = MegaTheme.colors.text.secondary
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewTimeHeader(
    @PreviewParameter(BooleanProvider::class) isMine: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TimeHeader(
            timeString = "Friday, Nov 14, 2023",
            displayAsMine = isMine,
            userName = "John Doe"
        )
    }
}