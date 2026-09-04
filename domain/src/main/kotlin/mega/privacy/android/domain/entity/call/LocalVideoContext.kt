package mega.privacy.android.domain.entity.call

/**
 * Immutable snapshot of everything the in-call local-video decision logic reads.
 *
 * @property callInProgress The call currently in progress, or null if there is none.
 * @property isVideoEnabled True if local video is currently enabled for the in-progress call's chat
 *                          (the per-chat video status).
 * @property isScreenOn True if the screen is currently on.
 * @property isFrontCameraInUse True if the front camera is the video device currently in use.
 * @property isDisablingLocalVideo True if local video is currently being disabled.
 * @property isInTemporaryState True if the video is in a temporary (screen-lock / proximity) state.
 */
data class LocalVideoContext(
    val callInProgress: CallInProgress?,
    val isVideoEnabled: Boolean,
    val isScreenOn: Boolean,
    val isFrontCameraInUse: Boolean,
    val isDisablingLocalVideo: Boolean,
    val isInTemporaryState: Boolean,
) {

    /**
     * Minimal view of the in-progress call that the decision logic reads.
     *
     * @property chatId Id of the chat the call belongs to.
     * @property status [ChatCallStatus] of the call, or null if unknown.
     * @property hasLocalVideo True if the call currently has local video enabled.
     */
    data class CallInProgress(
        val chatId: Long,
        val status: ChatCallStatus?,
        val hasLocalVideo: Boolean,
    )
}
