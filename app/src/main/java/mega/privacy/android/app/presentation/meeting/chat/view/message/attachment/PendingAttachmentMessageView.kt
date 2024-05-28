package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.file.FileMessageView

/**
 * View for pending messages (not send to SDK yet)
 */
@Composable
fun PendingAttachmentMessageView(
    message: PendingFileAttachmentMessage,
    modifier: Modifier = Modifier,
    viewModel: PendingAttachmentMessageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.updateAndGetUiStateFlow(message).collectAsStateWithLifecycle()
    FileMessageView(
        isMe = message.isMine,
        fileTypeResId = uiState.fileTypeResId,
        previewUri = uiState.previewUri?.toUri(),
        loadProgress = uiState.progress?.floatValue ?: 0f,
        fileName = uiState.fileName,
        fileSize = uiState.fileSize,
        duration = uiState.duration,
        showPausedTransfersWarning = uiState.areTransfersPaused,
        modifier = modifier,
    )
}