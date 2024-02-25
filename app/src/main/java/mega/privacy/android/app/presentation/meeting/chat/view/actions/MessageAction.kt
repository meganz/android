package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Message action
 */
interface MessageAction {
    /**
     * Applies to
     *
     * @param messages
     * @return
     */
    fun appliesTo(messages: Set<TypedMessage>): Boolean

    /**
     * In column
     *
     * @param messages
     * @param hideBottomSheet
     * @return
     */
    fun bottomSheetMenuItem(
        messages: Set<TypedMessage>,
        hideBottomSheet: () -> Unit,
    ): @Composable () -> Unit
}
