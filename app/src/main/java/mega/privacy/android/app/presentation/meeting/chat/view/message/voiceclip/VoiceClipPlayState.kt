package mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip

/**
 * Play state of voice clip
 */
sealed interface VoiceClipPlayState {

    /**
     * player is in idle state
     */
    data object Idle : VoiceClipPlayState

    /**
     * Player is prepared and ready to start playing
     */
    data object Prepared : VoiceClipPlayState

    /**
     * Voice clip is playing
     * @property pos position in milliseconds
     */
    data class Playing(val pos: Int) : VoiceClipPlayState

    /**
     * Error
     *
     * @property error details of the error
     */
    data class Error(val error: Exception) : VoiceClipPlayState

    /**
     * A voice clip play is completed.
     */
    data object Completed : VoiceClipPlayState
}