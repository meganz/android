package mega.privacy.android.app.mediaplayer.service

import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode

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
     * Callback from onIsPlayingChanged
     *
     * @param isPlaying true is playing, otherwise is false
     */
    fun onIsPlayingChangedCallback(isPlaying: Boolean)

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
    fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean)

    /**
     * Callback from onPlaybackStateChanged
     *
     * @param state playback state
     */
    fun onPlaybackStateChangedCallback(state: Int)

    /**
     * Callback from onPlayerError
     */
    fun onPlayerErrorCallback()
}