package mega.privacy.android.app.presentation.meeting.list.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.theme.extensions.grey_012_white_012
import mega.privacy.android.core.ui.theme.extensions.grey_087_white_087

/**
 * Meeting header item view
 *
 * @param modifier
 * @param text
 * @param showDivider
 */
@Composable
fun MeetingHeaderItemView(
    modifier: Modifier = Modifier,
    text: String,
    showDivider: Boolean = false,
) {
    if (showDivider) {
        Divider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colors.grey_012_white_012,
            thickness = 1.dp
        )
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .height(36.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = text,
            color = MaterialTheme.colors.grey_087_white_087,
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "PreviewMeetingHeaderItemView")
@Composable
private fun PreviewMeetingHeaderItemView() {
    MeetingHeaderItemView(text = "Monday, 23 May")
}
