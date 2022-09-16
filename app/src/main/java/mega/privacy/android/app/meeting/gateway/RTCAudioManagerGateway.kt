package mega.privacy.android.app.meeting.gateway

import mega.privacy.android.app.interfaces.OnProximitySensorListener
import mega.privacy.android.app.main.megachat.AppRTCAudioManager

/**
 * Rtc audio manager gateway
 */
interface RTCAudioManagerGateway {
    /**
     * Is an incoming call ringing
     *
     * @return
     */
    val isAnIncomingCallRinging: Boolean

    /**
     * Mute or unmute
     *
     * @param mute
     */
    fun muteOrUnMute(mute: Boolean)

    /**
     * Audio manager
     */
    val audioManager: AppRTCAudioManager?

    /**
     * Create or update audio manager
     *
     * @param isSpeakerOn
     * @param type
     */
    fun createOrUpdateAudioManager(isSpeakerOn: Boolean, type: Int)

    /**
     * Remove the incoming call AppRTCAudioManager.
     */
    fun removeRTCAudioManagerRingIn()

    /**
     * Remove the ongoing call AppRTCAudioManager.
     */
    fun removeRTCAudioManager()

    /**
     * Method for updating the call status of the Speaker status .
     *
     * @param isSpeakerOn If the speaker is on.
     * @param typeStatus  type AudioManager.
     */
    fun updateSpeakerStatus(isSpeakerOn: Boolean, typeStatus: Int)

    /**
     * Activate the proximity sensor.
     */
    fun startProximitySensor(listener: OnProximitySensorListener)

    /**
     * Deactivates the proximity sensor
     */
    fun unregisterProximitySensor()

    /**
     * Method for stopping the sound of incoming or outgoing calls.
     */
    fun stopSounds()

    /**
     * Method for updating the call status of the Audio Manger.
     *
     * @param callStatus Call status.
     */
    fun updateRTCAudioMangerTypeStatus(callStatus: Int)
}