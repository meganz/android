package mega.privacy.android.app.presentation.meeting.chat.model.messages.actions

import androidx.compose.runtime.Composable

/**
 * Message bottom sheet action.
 *
 * @property view The Composable view to be displayed in the bottom sheet.
 * @property group [MessageActionGroup].
 */
data class MessageBottomSheetAction(
    val view: @Composable () -> Unit,
    val group: MessageActionGroup,
)
