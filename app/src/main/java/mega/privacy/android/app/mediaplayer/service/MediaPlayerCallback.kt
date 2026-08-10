package mega.privacy.android.app.mediaplayer.service

import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * For callback from Player.Listener
 */
interface MediaPlayerCallback {
    /**
     * Callback from onMediaItemTransition
     *
     * @param handle MegaNode handle
     * @param isUpdateName true is update the name, otherwise is false
     */
    fun onMediaItemTransitionCallback(handle: String?, isUpdateName: Boolean)

    /**
     * Callback from onShuffleModeEnabledChanged
     *
     * @param shuffleModeEnabled true is shuffle mode enable, otherwise is false
     */
    fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean)

    /**
     * Callback from onRepeatModeChanged
     *
     * @param repeatToggleMode RepeatToggleMode
     */
    fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode)

    /**
     * Callback from onPlayWhenReadyChanged
     *
     * @param playWhenReady true is play when ready, otherwise is false
     */
    fun onPlayWhenReadyChangedCallback(
        playWhenReady: Boolean,
        reason: Int,
    )

    /**
     * Callback from onPlaybackStateChanged
     *
     * @param state playback state
     */
    fun onPlaybackStateChangedCallback(state: Int)

    /**
     * Callback from onPlayerError
     *
     * @param errorCode the ExoPlayer error code from [androidx.media3.common.PlaybackException]
     */
    fun onPlayerErrorCallback(errorCode: Int)

    /**
     * Callback from onVideoSizeChanged
     *
     * @param videoWidth video width
     * @param videoHeight video height
     */
    fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int)

    /**
     * Callback when the media contains a video track but no video renderer is active,
     * indicating a silent decoder failure (e.g. hardware decoder exceeds capabilities).
     */
    fun onVideoNotRenderedCallback()
}
