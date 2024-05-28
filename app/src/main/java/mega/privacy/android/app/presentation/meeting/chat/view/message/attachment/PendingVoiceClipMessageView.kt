package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.CoreVoiceClipMessageView

/**
 * View for pending voice clips (not send to SDK yet)
 */
@Composable
fun PendingVoiceClipMessageView(
    message: PendingVoiceClipMessage,
    modifier: Modifier = Modifier,
    viewModel: PendingAttachmentMessageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.updateAndGetUiStateFlow(message).collectAsStateWithLifecycle()

    CoreVoiceClipMessageView(
        isMe = message.isMine,
        timestamp = uiState.duration ?: "",
        modifier = modifier,
        exists = true,
        loadProgress = uiState.progress?.floatValue ?: 0f,
        interactionEnabled = false,
        showPausedTransfersWarning = uiState.areTransfersPaused,
    )
}