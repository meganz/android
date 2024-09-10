package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageBottomSheetAction
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.AddReactionsSheetItem
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Bottom sheet for chat message options.
 */
@Composable
fun MessageOptionsBottomSheet(
    onReactionClicked: (String) -> Unit,
    onMoreReactionsClicked: (Long) -> Unit,
    actions: List<MessageBottomSheetAction>,
    modifier: Modifier = Modifier,
    messageId: Long,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_MESSAGE_OPTIONS_PANEL)
    ) {
        AddReactionsSheetItem(
            onReactionClicked = {
                onReactionClicked(it)
            },
            onMoreReactionsClicked = { onMoreReactionsClicked(messageId) },
            modifier = Modifier.padding(8.dp),
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

@CombinedThemePreviews
@Composable
private fun MessageOptionsBottomSheetPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MessageOptionsBottomSheet(
            onReactionClicked = {},
            onMoreReactionsClicked = {},
            actions = listOf(),
            messageId = -1L,
        )
    }
}

internal const val TEST_TAG_MESSAGE_OPTIONS_PANEL = "chat_view:message_options_panel"