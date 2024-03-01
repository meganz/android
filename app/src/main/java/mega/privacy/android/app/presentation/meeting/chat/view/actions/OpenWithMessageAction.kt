package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.CanNotOpenFileDialog
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageViewModel
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import timber.log.Timber

internal class OpenWithMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.external_play,
    icon = mega.privacy.android.icon.pack.R.drawable.ic_menu_open_with,
    testTag = "open_with",
) {
    override fun appliesTo(messages: Set<TypedMessage>) =
        messages.size == 1 && messages.first() is NodeAttachmentMessage

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        var showDownloadDialog by rememberSaveable {
            mutableStateOf(false)
        }
        val viewModel: NodeAttachmentMessageViewModel = hiltViewModel()
        val message = messages.first() as NodeAttachmentMessage
        val context = LocalContext.current
        LaunchedEffect(messages.size) {
            runCatching {
                viewModel.getChatNodeContentUri(message)
            }.onSuccess { contentUri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    viewModel.applyNodeContentUri(
                        intent = this,
                        content = contentUri,
                        mimeType = message.fileNode.type.mimeType,
                        isSupported = false
                    )
                }
                runCatching {
                    context.startActivity(intent)
                    onHandled()
                }.onFailure {
                    showDownloadDialog = true
                }
            }.onFailure {
                Timber.e(it, "Failed to get content uri")
            }
        }

        if (showDownloadDialog) {
            CanNotOpenFileDialog(
                onDownloadClick = {
                    chatViewModel.onDownloadForPreviewChatNode(message.fileNode)
                    showDownloadDialog = false
                    onHandled()
                },
                onDismiss = {
                    showDownloadDialog = false
                    onHandled()
                }
            )
        }
    }
}