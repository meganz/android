package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.core.ui.model.MenuActionWithClick
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage

internal class SelectMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.general_select,
    icon = R.drawable.ic_check_circle_medium_regular_outline,
    testTag = "action_select"
) {
    override fun appliesTo(messages: Set<TypedMessage>) = messages.size == 1
            && messages.first() !is ManagementMessage && messages.first() !is InvalidMetaMessage
            && messages.first() !is InvalidMessage

    override fun toolbarItem(
        messages: Set<TypedMessage>,
        onClick: () -> Unit,
    ): MenuActionWithClick? = null

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        chatViewModel.onEnableSelectMode()
        onHandled()
    }
}