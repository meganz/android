package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.ChatLocationView
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.EditBottomSheetOption
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage

internal class EditLocationMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction() {

    override fun appliesTo(messages: Set<TypedMessage>): Boolean =
        messages.size == 1 && messages.first().let { it.isEditable && it is LocationMessage }

    override fun bottomSheetItem(onClick: () -> Unit): @Composable () -> Unit = {
        EditBottomSheetOption {
            onClick()
        }
    }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
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