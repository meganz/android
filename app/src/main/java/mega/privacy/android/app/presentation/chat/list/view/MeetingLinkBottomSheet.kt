package mega.privacy.android.app.presentation.chat.list.view

import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedWithoutBackgroundMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.mobile.analytics.event.ShareLinkDialogEvent

/**
 * Contact info bottom sheet
 *
 * @param modalSheetState
 * @param coroutineScope
 * @param onSendLinkToChat
 * @param onShareLink
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun MeetingLinkBottomSheet(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    onSendLinkToChat: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheet(
        modalSheetState = modalSheetState,
        sheetBody = {
            MeetingLinkView(modifier = modifier, onSendLinkToChat = {
                coroutineScope.launch {
                    modalSheetState.hide()
                    onSendLinkToChat()
                }
            }, onShareLink = {
                coroutineScope.launch {
                    modalSheetState.hide()
                    onShareLink()
                }
            })
        },
    )
}

@Composable
internal fun MeetingLinkView(
    onSendLinkToChat: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(ShareLinkDialogEvent)
    }
    Column(
        modifier = modifier
            .padding(vertical = 20.dp)
            .testTag("meeting_list:share_link"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(IconR.drawable.ic_meeting_share_link),
            contentDescription = "Empty placeholder",
            modifier = Modifier.size(80.dp),
        )
        MegaText(
            text = stringResource(sharedR.string.meetings_share_link_bottom_sheet_title),
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.W500),
            textColor = TextColor.Primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 50.dp, end = 50.dp)
        )

        MegaText(
            text = stringResource(R.string.scheduled_meetings_share_meeting_link_panel_title),
            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.W500),
            textColor = TextColor.Secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 50.dp, end = 50.dp)
        )

        OutlinedWithoutBackgroundMegaButton(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            text = stringResource(sharedR.string.meetings_share_link_bottom_sheet_button_send_link_chat),
            onClick = onSendLinkToChat,
            rounded = false,
            enabled = true,
            iconId = null
        )

        RaisedDefaultMegaButton(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = stringResource(sharedR.string.meetings_share_link_bottom_sheet_button_share_link),
            onClick = onShareLink,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MeetingLinkViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MeetingLinkView(
            onSendLinkToChat = {},
            onShareLink = {},
        )
    }
}
