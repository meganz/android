package mega.privacy.android.app.presentation.meeting.chat.view.actions


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.CopyBottomSheetOption
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage

internal class CopyMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction {
    override fun appliesTo(messages: Set<TypedMessage>) =
        messages.all { it is NormalMessage || it is RichPreviewMessage || it is LocationMessage }

    override fun bottomSheetMenuItem(
        messages: Set<TypedMessage>,
        hideBottomSheet: () -> Unit,
    ): @Composable () -> Unit = {
        val context = LocalContext.current
        CopyBottomSheetOption(
            onClick = {
                val text = messages.joinToString(separator = "\n") { getMessageContent(it) }
                copyToClipboard(context, text)
                hideBottomSheet()
            }
        )
    }

    private fun getMessageContent(message: TypedMessage) = when (message) {
        is NormalMessage -> message.content
        is LocationMessage -> message.textMessage
        is RichPreviewMessage -> message.textMessage
        else -> ""
    }

    fun copyToClipboard(context: Context, text: String?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(Constants.COPIED_TEXT_LABEL, text)
        clipboard.setPrimaryClip(clip)
    }
}