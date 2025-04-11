package mega.privacy.android.app.presentation.videoplayer.model

/**
 * The entity regarding playback position state
 */
enum class PlaybackPositionStatus {
    /**
     * Resume state
     */
    Resume,

    /**
     * Restart state
     */
    Restart,

    /**
     * Dialog showing
     */
    DialogShowing,

    /**
     * Initial
     */
    Initial
}