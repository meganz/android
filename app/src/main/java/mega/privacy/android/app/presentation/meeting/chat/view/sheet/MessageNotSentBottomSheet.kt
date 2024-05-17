package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageBottomSheetAction
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionHeader

/**
 * Bottom sheet for not sent
 */
@Composable
fun MessageNotSentBottomSheet(
    actions: List<MessageBottomSheetAction>,
    areTransfersPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_SEND_ERROR_PANEL)
    ) {
        MenuActionHeader(
            modifier = Modifier.testTag(TEST_TAG_SEND_ERROR_HEADER),
            text = stringResource(
                if (areTransfersPaused) R.string.attachment_uploading_state_paused
                else R.string.title_message_not_sent_options
            )
        )
        var group = if (actions.isNotEmpty()) actions.first().group else null
        actions.forEach {
            if (group != it.group) {
                MegaDivider(dividerType = DividerType.BigStartPadding)
                group = it.group
            }
            it.view()
        }
    }

}

internal const val TEST_TAG_SEND_ERROR_PANEL = "chat_view:send_error_sheet"
internal const val TEST_TAG_SEND_ERROR_HEADER = "chat_view:send_error_header"