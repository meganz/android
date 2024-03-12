package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage

/**
 * UI state for voice clip message
 *
 * @property isPlaying True if the voice clip is playing.
 * @property playProgress The progress of playing.
 * @property loadProgress The progress of loading.
 * @property timestamp The timestamp text of the voice clip.
 * @property voiceClipMessage The voice clip message.
 */
data class VoiceClipMessageUiState(
    val isPlaying: Boolean = false,
    val playProgress: Progress? = null,
    val loadProgress: Progress? = Progress(0f),
    val timestamp: String? = null,
    val voiceClipMessage: VoiceClipMessage? = null,
)