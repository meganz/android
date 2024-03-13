package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.app.presentation.meeting.chat.view.ChatLocationView
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.mobile.analytics.event.ChatConversationEditActionMenuItemEvent

internal class EditLocationMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.title_edit_profile_info,
    icon = R.drawable.ic_pen_2_medium_regular_outline,
    testTag = "action_edit",
    group = MessageActionGroup.Modify,
) {

    override fun shouldDisplayFor(messages: Set<TypedMessage>): Boolean =
        messages.size == 1 && messages.first().let { it.isEditable && it is LocationMessage }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        Analytics.tracker.trackEvent(ChatConversationEditActionMenuItemEvent)
        val uiState by chatViewModel.state.collectAsStateWithLifecycle()
        ChatLocationView(
            isGeolocationEnabled = uiState.isGeolocationEnabled,
            onEnableGeolocation = { chatViewModel.onEnableGeolocation() },
            onSendLocationMessage = { chatViewModel.sendLocationMessage(it) },
            onDismissView = onHandled,
            msgId = messages.first().msgId
        )
    }
}