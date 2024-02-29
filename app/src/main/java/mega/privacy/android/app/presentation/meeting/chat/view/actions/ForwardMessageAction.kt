package mega.privacy.android.app.presentation.meeting.chat.view.actions

import mega.privacy.android.core.R as CoreResources
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openChatPicker
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage

internal class ForwardMessageAction(
    private val chatViewModel: ChatViewModel,
    private val launchChatPicker: (Context, Long, ActivityResultLauncher<Intent>) -> Unit = ::openChatPicker,
) : MessageAction(
    text = R.string.forward_menu_item,
    icon = CoreResources.drawable.ic_arrow_corner_right,
    testTag = "action_forward",
) {
    override fun appliesTo(messages: Set<TypedMessage>) = messages.isNotEmpty() &&
            messages.none { it is ManagementMessage || it is InvalidMessage || it is InvalidMetaMessage }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        val chatPickerLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                result.data?.let {
                    val chatHandles = it.getLongArrayExtra(Constants.SELECTED_CHATS)?.toList()
                    val contactHandles =
                        it.getLongArrayExtra(Constants.SELECTED_USERS)?.toList()
                    chatViewModel.onForwardMessages(messages, chatHandles, contactHandles)
                }
                onHandled()
            }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            launchChatPicker(
                context,
                messages.first().chatId,
                chatPickerLauncher
            )
        }
    }

}