package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.model.MenuActionWithClick
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Message action
 *
 * @property text
 * @property icon
 * @property testTag
 * @property group
 */
abstract class MessageAction(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    private val testTag: String,
    val group: MessageActionGroup,
) {

    /**
     * Bottom sheet item test tag
     */
    val bottomSheetItemTestTag = "chat_message_options_sheet:$testTag"

    /**
     * Toolbar menu item test tag
     */
    val toolbarMenuItemTestTag = "chat_message_toolbar:$testTag"

    /**
     * Applies to send error
     */
    protected open val appliesToSendError = false

    /**
     * Applies to
     *
     * @param messages
     * @return true if the action should be displayed for the selected set of messages
     */
    fun appliesTo(messages: Set<TypedMessage>) =
        (appliesToSendError || messages.none { it.isSendError() }) && shouldDisplayFor(messages)

    /**
     * Should display for
     *
     * @param messages
     * @return true if the action should be displayed for the selected set of messages excluding
     * checking for send errors
     */
    protected abstract fun shouldDisplayFor(messages: Set<TypedMessage>): Boolean

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
        setAction: ((@Composable () -> Unit)?) -> Unit,
    ): @Composable () -> Unit = bottomSheetItem(messages.first()) {
        setAction {
            trackTriggerEvent(source = TriggerSource.BottomSheet)
            OnTrigger(messages = messages) {
                setAction(null)
            }
        }
        hideBottomSheet()
    }

    /**
     * Toolbar menu item with click
     *
     * @param messages
     * @param exitSelectMode
     */
    fun toolbarMenuItemWithClick(
        messages: Set<TypedMessage>,
        exitSelectMode: () -> Unit,
        setAction: ((@Composable () -> Unit)?) -> Unit,
    ): MenuActionWithClick? = toolbarItem(messages) {
        setAction {
            trackTriggerEvent(source = TriggerSource.Toolbar)
            OnTrigger(messages = messages) {
                setAction(null)
            }
        }
        exitSelectMode()
    }

    private fun toolbarMenuItem() = object : MenuActionWithIcon {
        @Composable
        override fun getIconPainter() = painterResource(id = icon)

        @Composable
        override fun getDescription() = stringResource(id = text)

        override val testTag: String = toolbarMenuItemTestTag
    }

    /**
     * Bottom sheet item
     *
     * @param message
     * @param onClick
     * @return the bottom sheet item
     */
    protected open fun bottomSheetItem(
        message: TypedMessage,
        onClick: () -> Unit,
    ): @Composable () -> Unit = {
        MenuActionListTile(
            text = stringResource(id = text),
            icon = painterResource(id = icon),
            modifier = Modifier
                .testTag(bottomSheetItemTestTag)
                .clickable { onClick() },
            isDestructive = isBottomSheetItemDestructive(),
            dividerType = null,
        )
    }


    /**
     * Is bottom sheet item destructive.
     */
    protected open fun isBottomSheetItemDestructive(): Boolean = false

    /**
     * Toolbar item
     *
     * @param messages
     * @param onClick
     * @return the toolbar item if any
     */
    protected open fun toolbarItem(
        messages: Set<TypedMessage>,
        onClick: () -> Unit,
    ): MenuActionWithClick? =
        MenuActionWithClick(
            menuAction = toolbarMenuItem(),
            onClick = onClick
        )

    /**
     * On trigger - Action to perform on click
     *
     * @param messages
     */
    @Composable
    abstract fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit)

    /**
     * Track [OnTrigger] - Analytics purposes
     * Should be called before we call the [OnTrigger] method
     *
     * @param source [TriggerSource]
     */
    open fun trackTriggerEvent(source: TriggerSource) {}

    /**
     * An interface to differentiate the trigger source of a message action
     */
    sealed interface TriggerSource {

        /**
         * Indicates that bottom sheet is the trigger source of a message action
         */
        data object BottomSheet : TriggerSource

        /**
         * Indicates that toolbar is the trigger source of a message action
         */
        data object Toolbar : TriggerSource
    }
}
