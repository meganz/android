package mega.privacy.android.app.presentation.meeting.chat.model

/**
 * Send state
 *
 * @constructor Create empty Send state
 */
enum class SendState {
    /**
     * None
     */
    None,

    /**
     * Sending
     */
    Sending,

    /**
     * Send error
     */
    SendError,

    /**
     * Permanently failed
     */
    PermanentlyFailed,
}
