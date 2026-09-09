package mega.privacy.android.app.meeting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mega.privacy.android.app.meeting.gateway.CameraInUseGateway
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.domain.entity.call.LocalVideoAction
import mega.privacy.android.domain.entity.call.LocalVideoContext
import mega.privacy.android.domain.entity.call.LocalVideoTrigger
import mega.privacy.android.domain.entity.environment.ScreenLockState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.call.GetChatCallInProgress
import mega.privacy.android.domain.usecase.call.ResolveLocalVideoActionUseCase
import mega.privacy.android.domain.usecase.environment.MonitorScreenLockStateUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastLocalVideoChangedDueToProximitySensorUseCase
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableVideoUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the in-call local-video state machine that decides whether local video should be enabled or
 * disabled in response to screen-lock and proximity-sensor events.
 *
 * It holds the per-chat video status and the screen-lock / temporary-state flags, collects the
 * screen lock state via [MonitorScreenLockStateUseCase] while monitoring is active, and applies the
 * [LocalVideoAction]s resolved by the pure [ResolveLocalVideoActionUseCase]. It has no dependency on
 * `ChatManagement`, which breaks the previous audio-manager dependency cycle.
 */
@Singleton
class CallLocalVideoController @Inject constructor(
    private val resolveLocalVideoActionUseCase: ResolveLocalVideoActionUseCase,
    private val getChatCallInProgress: GetChatCallInProgress,
    private val cameraInUseGateway: CameraInUseGateway,
    private val enableOrDisableVideoUseCase: EnableOrDisableVideoUseCase,
    private val broadcastLocalVideoChangedDueToProximitySensorUseCase: BroadcastLocalVideoChangedDueToProximitySensorUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val monitorScreenLockStateUseCase: MonitorScreenLockStateUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private val hashMapVideo = HashMap<Long, Boolean>()

    /**
     * Boolean indicating whether the video is in a temporary (screen-lock / proximity) state.
     */
    var isInTemporaryState = false

    /**
     * Boolean indicating whether the local video is being disabled.
     */
    var isDisablingLocalVideo = false

    private var isScreenOn = true
    private var isMonitoring = false
    private var monitoringJob: Job? = null

    /**
     * Get video status
     *
     * @param chatId The chat ID
     * @return True if video is enabled or false otherwise
     */
    fun getVideoStatus(chatId: Long) =
        if (hashMapVideo.containsKey(chatId) && hashMapVideo[chatId] != null) {
            hashMapVideo[chatId]!!
        } else {
            setVideoStatus(chatId, false)
            false
        }

    /**
     * Set video status
     *
     * @param chatId The chat ID
     * @param videoStatus True if video is enabled or false otherwise
     */
    fun setVideoStatus(chatId: Long, videoStatus: Boolean) {
        hashMapVideo[chatId] = videoStatus
    }

    /**
     * Remove the video status stored for a chat.
     *
     * @param chatId The chat ID
     */
    fun removeVideoStatus(chatId: Long) {
        hashMapVideo.remove(chatId)
    }

    /**
     * Start monitoring the screen lock state, resolving and applying the local-video actions for
     * each screen-lock change while a call is in progress.
     */
    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        monitoringJob = applicationScope.launch {
            monitorScreenLockStateUseCase().collect { screenLockState ->
                val trigger = when (screenLockState) {
                    ScreenLockState.Locked -> LocalVideoTrigger.ScreenOff
                    ScreenLockState.Unlocked -> LocalVideoTrigger.UserPresent
                }
                resolveAndApply(trigger)
            }
        }
    }

    /**
     * Stop monitoring the screen lock state.
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        monitoringJob?.cancel()
        monitoringJob = null
        isMonitoring = false
    }

    /**
     * Method for checking when there are changes in the proximity sensor
     *
     * @param isNear True, if the device is close to the ear. False, if it is far away
     */
    fun onProximityChanged(isNear: Boolean) {
        val trigger =
            if (isNear) LocalVideoTrigger.ProximityNear else LocalVideoTrigger.ProximityFar
        applicationScope.launch {
            resolveAndApply(trigger)
        }
    }

    private suspend fun resolveAndApply(trigger: LocalVideoTrigger) {
        val call = getChatCallInProgress()
        val context = LocalVideoContext(
            callInProgress = call?.let {
                LocalVideoContext.CallInProgress(
                    chatId = it.chatId,
                    status = it.status,
                    hasLocalVideo = it.hasLocalVideo,
                )
            },
            isVideoEnabled = call?.let { getVideoStatus(it.chatId) } ?: false,
            isScreenOn = isScreenOn,
            isFrontCameraInUse = cameraInUseGateway.isFrontCameraInUse(),
            isDisablingLocalVideo = isDisablingLocalVideo,
            isInTemporaryState = isInTemporaryState,
        )
        resolveLocalVideoActionUseCase(trigger, context).forEach { action ->
            when (action) {
                LocalVideoAction.MuteMic -> rtcAudioManagerGateway.muteOrUnMute(true)
                is LocalVideoAction.SetScreenOn -> isScreenOn = action.isScreenOn
                is LocalVideoAction.SetTemporaryState -> isInTemporaryState = action.isInTemporaryState
                is LocalVideoAction.SetDisablingLocalVideo ->
                    isDisablingLocalVideo = action.isDisablingLocalVideo

                is LocalVideoAction.EnableVideo -> enableDisableVideo(action.chatId, enable = true)
                is LocalVideoAction.DisableVideo -> enableDisableVideo(action.chatId, enable = false)
                is LocalVideoAction.BroadcastLocalVideoChanged ->
                    broadcastChangesInLocalVideoDueToProximitySensor(action.shouldVideoBeEnabled)
            }
        }
    }

    private fun enableDisableVideo(chatId: Long, enable: Boolean) {
        applicationScope.launch {
            runCatching {
                enableOrDisableVideoUseCase(chatId, enable)
            }.onFailure { exception ->
                Timber.e(exception.message)
            }
        }
    }

    private fun broadcastChangesInLocalVideoDueToProximitySensor(shouldVideoBeEnabled: Boolean) {
        applicationScope.launch {
            broadcastLocalVideoChangedDueToProximitySensorUseCase(shouldVideoBeEnabled)
        }
    }
}
