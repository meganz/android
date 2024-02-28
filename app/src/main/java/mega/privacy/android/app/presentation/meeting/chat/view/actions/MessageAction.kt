package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Message action
 */
abstract class MessageAction {
    /**
     * Applies to
     *
     * @param messages
     * @return
     */
    abstract fun appliesTo(messages: Set<TypedMessage>): Boolean

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
    ): @Composable () -> Unit {
        return bottomSheetItem {
            pending.addAll(messages)
            hideBottomSheet()
        }
    }

    /**
     * Bottom sheet item
     *
     * @param onClick
     * @return the bottom sheet item
     */
    abstract fun bottomSheetItem(onClick: () -> Unit): @Composable () -> Unit

    private val pending = mutableSetOf<TypedMessage>()

    /**
     * On trigger - Action to perform on click
     *
     * @param messages
     */
    @Composable
    abstract fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit)


    /**
     * Call if triggered
     */
    @Composable
    fun CallIfTriggered() {
        if (pending.isNotEmpty()) {
            OnTrigger(pending) { pending.clear() }
        }
    }
}
