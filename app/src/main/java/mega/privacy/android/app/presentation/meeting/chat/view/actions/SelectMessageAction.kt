package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.mobile.analytics.event.ChatConversationSelectActionMenuItemEvent

internal class SelectMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.general_select,
    icon = mega.privacy.android.icon.pack.R.drawable.ic_check_circle_medium_thin_outline,
    testTag = "action_select",
    group = MessageActionGroup.Select,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.size == 1
            && messages.first() !is ManagementMessage
            && messages.first() !is InvalidMetaMessage
            && messages.first() !is InvalidMessage

    override fun toolbarItem(
        messages: Set<TypedMessage>,
        onClick: () -> Unit,
    ): MenuActionWithClick? = null

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        Analytics.tracker.trackEvent(ChatConversationSelectActionMenuItemEvent)
        chatViewModel.onEnableSelectMode()
        onHandled()
    }
}