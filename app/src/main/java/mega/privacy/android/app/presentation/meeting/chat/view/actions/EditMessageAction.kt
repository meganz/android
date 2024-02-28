package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.EditBottomSheetOption
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage

internal class EditMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction() {

    override fun appliesTo(messages: Set<TypedMessage>): Boolean =
        messages.size == 1 && messages.first().let { it.isEditable && it !is LocationMessage }

    override fun bottomSheetItem(onClick: () -> Unit): @Composable () -> Unit = {
        EditBottomSheetOption {
            onClick()
        }
    }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        chatViewModel.onEditMessage(messages.first())
        onHandled()
    }
}