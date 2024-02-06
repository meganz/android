package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.core.ui.controls.chat.messages.file.FileMessageView
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage

/**
 * View for chat message with node attachmnent
 */
@Composable
fun NodeAttachmentMessageView(
    message: NodeAttachmentMessage,
    chatId: Long,
    viewModel: NodeAttachmentMessageViewModel = hiltViewModel()
) {
    val uiState by viewModel.getOrPutUiStateFlow(message, chatId).collectAsStateWithLifecycle()
    FileMessageView(
        isMe = message.isMine,
        fileTypeResId = uiState.fileTypeResId,
        previewUri = uiState.previewUri?.toUri(),
        loadProgress = uiState.loadProgress?.floatValue,
        fileName = uiState.fileName,
        fileSize = uiState.fileSize,
        duration = uiState.duration
    )
}