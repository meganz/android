package mega.privacy.android.app.presentation.meeting.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.yellow_100_yellow_700

/**
 * Show waiting room warning dialog
 */
@Composable
fun WaitingRoomWarningDialog(
    onLearnMoreClicked: () -> Unit,
    onCloseClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag(WAITING_ROOM_WARNING_DIALOG_TAG)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.yellow_100_yellow_700)
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = 62.dp
                ),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MegaSpannedText(
                    modifier = Modifier
                        .testTag(LEARN_MORE_TAG)
                        .clickable { onLearnMoreClicked() }
                        .padding(top = 14.dp, bottom = 14.dp, start = 16.dp)
                        .weight(1f),
                    value = stringResource(id = R.string.meetings_schedule_meeting_waiting_room_warning),
                    baseStyle = MaterialTheme.typography.subtitle1.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colors.onPrimary
                    ),
                    styles = mapOf(SpanIndicator('A') to SpanStyle(textDecoration = TextDecoration.Underline)),
                )
            }

            Row(
                modifier = Modifier
                    .padding(start = 17.dp, end = 17.dp, top = 14.dp)
                    .testTag(CLOSE_DIALOG_TAG)
                    .clickable { onCloseClicked() }
            ) {
                Icon(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopEnd),
                    imageVector = ImageVector.vectorResource(id = R.drawable.chat_tool_remove_ic),
                    contentDescription = "Close waiting room warning",
                    tint = MaterialTheme.colors.onPrimary
                )
            }
        }
    }

}

internal const val WAITING_ROOM_WARNING_DIALOG_TAG =
    "waiting_room_warning_dialog:waiting_room_warning"
internal const val LEARN_MORE_TAG = "waiting_room_warning_dialog:learn_more"
internal const val CLOSE_DIALOG_TAG = "waiting_room_warning_dialog:close_dialog"

/**
 * Waiting Room Warning Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewWaitingRoomWarningDialog")
@Composable
fun PreviewWaitingRoomWarningDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        WaitingRoomWarningDialog(
            onLearnMoreClicked = {},
            onCloseClicked = {}
        )
    }
}