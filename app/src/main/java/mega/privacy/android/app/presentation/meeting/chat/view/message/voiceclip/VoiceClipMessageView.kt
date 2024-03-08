package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.core.ui.controls.chat.messages.CoreVoiceClipMessageView
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage


@Composable
fun VoiceClipMessageView(
    message: VoiceClipMessage,
    onLongClick: (VoiceClipMessage) -> Unit,
    viewModel: VoiceClipMessageViewModel = hiltViewModel(),
    interactionEnabled: Boolean = true,
) {
    with(message) {
        val uiState by viewModel.getUiStateFlow(msgId).collectAsStateWithLifecycle()

        LaunchedEffect(msgId) {
            viewModel.addVoiceClip(message = message)
        }

        CoreVoiceClipMessageView(
            isMe = isMine,
            timestamp = uiState.timestamp,
            exists = uiState.voiceClipMessage?.exists ?: exists,
            loadProgress = uiState.loadProgress?.floatValue,
            playProgress = uiState.playProgress?.floatValue,
            isPlaying = uiState.isPlaying,
            onPlayClicked = { viewModel.onPlayOrPauseClicked(msgId) },
            onLongClick = { onLongClick(message) },
            interactionEnabled = interactionEnabled,
        )
    }
}