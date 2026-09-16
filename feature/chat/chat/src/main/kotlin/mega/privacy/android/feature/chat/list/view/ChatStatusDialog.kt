package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Presence picker offered from the chat list top app bar.
 *
 * The presence is published as soon as an option is picked, matching the legacy dialog, so the
 * only button is the one that backs out without changing anything.
 *
 * @param chatStatus Presence the user is currently publishing, used to preselect an option; a
 * transient connection state preselects nothing.
 * @param onStatusSelected Called with the presence the user picked.
 * @param onDismiss Called when the dialog is dismissed.
 */
@Composable
internal fun ChatStatusDialog(
    chatStatus: ChatStatus?,
    onStatusSelected: (UserChatStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = SelectableStatuses.mapIndexed { index, status ->
        BasicDialogRadioOption(ordinal = index, text = stringResource(status.labelId()))
    }.toImmutableList()

    BasicRadioDialog(
        modifier = Modifier.testTag(CHAT_STATUS_DIALOG_TAG),
        title = SpannableText(stringResource(sharedR.string.status_label)),
        options = options,
        selectedOption = chatStatus?.toUserChatStatus()
            ?.let { SelectableStatuses.indexOf(it) }
            ?.takeIf { it >= 0 }
            ?.let { options[it] },
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(sharedR.string.general_dialog_cancel_button),
                onClick = onDismiss,
            ),
        ),
        onOptionSelected = { option ->
            onStatusSelected(SelectableStatuses[option.ordinal])
            onDismiss()
        },
        onDismissRequest = onDismiss,
    )
}

private val SelectableStatuses = listOf(
    UserChatStatus.Online,
    UserChatStatus.Away,
    UserChatStatus.Busy,
    UserChatStatus.Offline,
)

private fun UserChatStatus.labelId(): Int = when (this) {
    UserChatStatus.Online -> sharedR.string.online_status
    UserChatStatus.Away -> sharedR.string.away_status
    UserChatStatus.Busy -> sharedR.string.busy_status
    UserChatStatus.Offline -> sharedR.string.offline_status
    UserChatStatus.Invalid -> sharedR.string.offline_status
}

private fun ChatStatus.toUserChatStatus(): UserChatStatus? = when (this) {
    ChatStatus.Online -> UserChatStatus.Online
    ChatStatus.Away -> UserChatStatus.Away
    ChatStatus.Busy -> UserChatStatus.Busy
    ChatStatus.Offline -> UserChatStatus.Offline
    ChatStatus.NoNetworkConnection, ChatStatus.Reconnecting, ChatStatus.Connecting -> null
}

internal const val CHAT_STATUS_DIALOG_TAG = "chat_status_dialog:dialog"

@CombinedThemePreviews
@Composable
private fun ChatStatusDialogPreview() {
    AndroidThemeForPreviews {
        ChatStatusDialog(
            chatStatus = ChatStatus.Online,
            onStatusSelected = {},
            onDismiss = {},
        )
    }
}
