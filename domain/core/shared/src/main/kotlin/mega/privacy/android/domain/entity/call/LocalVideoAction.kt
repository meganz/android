package mega.privacy.android.domain.entity.call

/**
 * An action to be applied by the caller as a result of resolving the in-call local-video logic.
 *
 * The decision logic is pure and mutates nothing: it returns an ordered list of these actions and
 * the caller is responsible for applying them (muting the mic, updating flags, enabling/disabling
 * video and broadcasting changes).
 */
sealed interface LocalVideoAction {

    /**
     * Mute the microphone.
     */
    data object MuteMic : LocalVideoAction

    /**
     * Update the screen-on flag.
     *
     * @property isScreenOn New value for the screen-on flag.
     */
    data class SetScreenOn(val isScreenOn: Boolean) : LocalVideoAction

    /**
     * Update the temporary-state flag.
     *
     * @property isInTemporaryState New value for the temporary-state flag.
     */
    data class SetTemporaryState(val isInTemporaryState: Boolean) : LocalVideoAction

    /**
     * Update the disabling-local-video flag.
     *
     * @property isDisablingLocalVideo New value for the disabling-local-video flag.
     */
    data class SetDisablingLocalVideo(val isDisablingLocalVideo: Boolean) : LocalVideoAction

    /**
     * Enable local video for the given chat.
     *
     * @property chatId Id of the chat whose local video should be enabled.
     */
    data class EnableVideo(val chatId: Long) : LocalVideoAction

    /**
     * Disable local video for the given chat.
     *
     * @property chatId Id of the chat whose local video should be disabled.
     */
    data class DisableVideo(val chatId: Long) : LocalVideoAction

    /**
     * Broadcast that the local video changed due to the proximity sensor.
     *
     * @property shouldVideoBeEnabled True if video should be enabled, false if it should be disabled.
     */
    data class BroadcastLocalVideoChanged(val shouldVideoBeEnabled: Boolean) : LocalVideoAction
}
