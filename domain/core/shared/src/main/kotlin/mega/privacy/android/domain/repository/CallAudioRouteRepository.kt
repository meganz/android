package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.AudioRoute

/**
 * Routes call audio to an output device and reports the route.
 *
 * The repository only executes routing requests. The choice of device is domain policy and the
 * caller makes it. Implementations select the thread that each platform API needs, so callers can
 * call every member from any thread.
 */
interface CallAudioRouteRepository {

    /**
     * Monitors the call audio route.
     *
     * The flow is state, not a stream of events: a new collector immediately receives the latest
     * [AudioRoute], and the flow emits again only when the route changes. When no session is
     * active, the route is `AudioRoute(AudioDevice.None, emptySet())`. The flow does not complete
     * when a session ends.
     */
    fun monitorAudioRoute(): Flow<AudioRoute>

    /**
     * Starts a call audio session, so that the platform routes call audio and the available
     * devices are reported.
     *
     * The implementation routes to [preferredDevice] as soon as that device is available. If the
     * device is not available, the implementation keeps the route that the platform chooses until
     * [select] is called. Calling this function while a session is active does not restart the
     * session. It only replaces the preferred device.
     *
     * @param preferredDevice The device to use first. Must not be [AudioDevice.None].
     */
    suspend fun startSession(preferredDevice: AudioDevice)

    /**
     * Routes call audio to [device].
     *
     * Does nothing when no session is active or when [device] is not in [AudioRoute.available],
     * because the available devices can change between the read and this call.
     */
    suspend fun select(device: AudioDevice)

    /**
     * Ends the call audio session and restores the audio state that was active before
     * [startSession]. Does nothing when no session is active.
     */
    suspend fun endSession()
}
