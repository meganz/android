package mega.privacy.android.app.meeting

import android.os.Handler
import android.os.Looper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the call audio-manager lifecycle: registers the screen receiver, creates or
 * updates the RTC audio manager on the main thread, and routes proximity changes to
 * [ChatManagement].
 */
@Singleton
class RtcAudioManagerController @Inject constructor(
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val chatManagement: ChatManagement,
) {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Register the screen receiver and, on the main thread, create or update the audio manager.
     *
     * @param isSpeakerOn Whether the speaker should be on.
     * @param type The audio manager type.
     */
    fun createOrUpdateAudioManager(isSpeakerOn: Boolean, type: Int) {
        Timber.d("Create or update audio manager, type is %s", type)
        chatManagement.registerScreenReceiver()
        mainHandler.post {
            rtcAudioManagerGateway.createOrUpdateAudioManager(isSpeakerOn, type)
        }
    }

    /**
     * Remove the incoming call audio manager.
     */
    fun removeRTCAudioManagerRingIn() = rtcAudioManagerGateway.removeRTCAudioManagerRingIn()

    /**
     * Activate the proximity sensor, routing proximity changes to [ChatManagement].
     */
    fun startProximitySensor() = rtcAudioManagerGateway.startProximitySensor { isNear ->
        chatManagement.controlProximitySensor(isNear)
    }
}

/**
 * Entry point to resolve [RtcAudioManagerController] in classes that cannot use constructor or
 * field injection (hand-instantiated legacy objects and static utils).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RtcAudioManagerControllerEntryPoint {

    /**
     * The [RtcAudioManagerController] instance.
     */
    fun rtcAudioManagerController(): RtcAudioManagerController
}
