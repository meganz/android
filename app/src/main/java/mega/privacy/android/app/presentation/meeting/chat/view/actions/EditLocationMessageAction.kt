package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.ChatLocationView
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.EditBottomSheetOption
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage

internal class EditLocationMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction {

    override fun appliesTo(messages: Set<TypedMessage>): Boolean =
        messages.size == 1 && messages.first().let { it.isEditable && it is LocationMessage }

    override fun bottomSheetMenuItem(
        messages: Set<TypedMessage>,
        context: Context,
        hideBottomSheet: () -> Unit,
    ): @Composable () -> Unit = {
        var showLocationView by rememberSaveable { mutableStateOf(false) }

        EditBottomSheetOption {
            showLocationView = true
        }

        if (showLocationView) {
            val uiState by chatViewModel.state.collectAsStateWithLifecycle()
            ChatLocationView(
                isGeolocationEnabled = uiState.isGeolocationEnabled,
                onEnableGeolocation = { chatViewModel.onEnableGeolocation() },
                onSendLocationMessage = { chatViewModel.sendLocationMessage(it) },
                onDismissView = {
                    showLocationView = false
                    hideBottomSheet()
                },
                msgId = messages.first().msgId
            )
        }
    }
}