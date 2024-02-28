package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile

/**
 * Message action
 *
 * @property text
 * @property icon
 * @property testTag
 */
abstract class MessageAction(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    private val testTag: String,
) {
    private val pending = mutableSetOf<TypedMessage>()

    /**
     * Bottom sheet item test tag
     */
    val bottomSheetItemTestTag = "chat_message_options_sheet:$testTag"

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
    ): @Composable () -> Unit = bottomSheetItem {
        pending.addAll(messages)
        hideBottomSheet()
    }

    /**
     * Bottom sheet item
     *
     * @param onClick
     * @return the bottom sheet item
     */
    protected open fun bottomSheetItem(onClick: () -> Unit): @Composable () -> Unit = {
        MenuActionListTile(
            text = stringResource(id = text),
            icon = painterResource(id = icon),
            modifier = Modifier
                .testTag(bottomSheetItemTestTag)
                .clickable { onClick() }
        )
    }

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
