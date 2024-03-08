package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import mega.privacy.android.core.ui.controls.chat.messages.file.FileMessageView
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage

/**
 * View for chat message with node attachment
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeAttachmentMessageView(
    message: NodeAttachmentMessage,
    modifier: Modifier = Modifier,
    uiState: AttachmentMessageUiState,
) {
    FileMessageView(
        isMe = message.isMine,
        fileTypeResId = uiState.fileTypeResId,
        previewUri = uiState.previewUri?.toUri(),
        loadProgress = uiState.loadProgress?.floatValue,
        fileName = uiState.fileName,
        fileSize = uiState.fileSize,
        duration = uiState.duration,
        modifier = modifier,
    )
}
