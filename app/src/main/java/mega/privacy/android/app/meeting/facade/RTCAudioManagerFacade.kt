package mega.privacy.android.app.meeting.facade

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.interfaces.OnProximitySensorListener
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.utils.Constants
import timber.log.Timber
import javax.inject.Inject

/**
 * Rtc audio manager facade
 * implementation of [RTCAudioManagerGateway]
 *
 * @property application
 */
class RTCAudioManagerFacade @Inject constructor(
    private val application: Application,
) : RTCAudioManagerGateway {
    private var rtcAudioManager: AppRTCAudioManager? = null
    private var rtcAudioManagerRingInCall: AppRTCAudioManager? = null

    /**
     * Broadcast for controlling changes in the volume.
     */
    private val volumeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val audioManager = rtcAudioManagerRingInCall ?: return
            if (action == Constants.VOLUME_CHANGED_ACTION) {
                val type = intent.extras?.getInt(Constants.EXTRA_VOLUME_STREAM_TYPE)
                if (type != AudioManager.STREAM_RING) return
                val newVolume = intent.extras?.getInt(Constants.EXTRA_VOLUME_STREAM_VALUE)
                if (newVolume != null && newVolume != Constants.INVALID_VOLUME) {
                    audioManager.checkVolume(newVolume)
                }
            }
        }
    }

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == action) {
                muteOrUnMute(true)
            }
        }
    }

    /**
     * Create or update audio manager
     *
     * @param isSpeakerOn
     * @param type
     */
    @SuppressLint("WrongConstant")
    override fun createOrUpdateAudioManager(isSpeakerOn: Boolean, type: Int) {
        Timber.d("Create or update audio manager, type is %s", type)
        if (type == Constants.AUDIO_MANAGER_CALL_RINGING) {
            if (rtcAudioManagerRingInCall != null) {
                removeRTCAudioManagerRingIn()
            }
            ContextCompat.registerReceiver(
                application, volumeReceiver,
                IntentFilter(Constants.VOLUME_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED
            )
            application.registerReceiver(
                becomingNoisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            )
            Timber.d("Creating RTC Audio Manager (ringing mode)")
            rtcAudioManagerRingInCall =
                AppRTCAudioManager.create(application, false, Constants.AUDIO_MANAGER_CALL_RINGING)
        } else {
            rtcAudioManager?.apply {
                typeAudioManager = type
                return
            }
            Timber.d("Creating RTC Audio Manager (%d mode)", type)
            removeRTCAudioManagerRingIn()
            rtcAudioManager = AppRTCAudioManager.create(application, isSpeakerOn, type)
            if (type != Constants.AUDIO_MANAGER_CREATING_JOINING_MEETING) {
                MegaApplication.getInstance().startProximitySensor()
            }
        }
    }

    /**
     * Remove the incoming call AppRTCAudioManager.
     */
    override fun removeRTCAudioManagerRingIn() {
        if (rtcAudioManagerRingInCall == null) return
        try {
            Timber.d("Removing RTC Audio Manager")
            rtcAudioManagerRingInCall?.stop()
            rtcAudioManagerRingInCall = null
            application.unregisterReceiver(volumeReceiver)
            application.unregisterReceiver(becomingNoisyReceiver)
        } catch (e: Exception) {
            Timber.e(e, "Exception stopping speaker audio manager")
        }
    }

    /**
     * Remove the ongoing call AppRTCAudioManager.
     */
    override fun removeRTCAudioManager() {
        try {
            Timber.d("Removing RTC Audio Manager")
            rtcAudioManager?.stop()
            rtcAudioManager = null
        } catch (e: java.lang.Exception) {
            Timber.e(e, "Exception stopping speaker audio manager")
        }
    }

    /**
     * Activate the proximity sensor.
     */
    override fun startProximitySensor(listener: OnProximitySensorListener) {
        rtcAudioManager?.apply {
            if (startProximitySensor()) {
                setOnProximitySensorListener(listener)
            }
        }
    }

    override fun unregisterProximitySensor() {
        rtcAudioManager?.unregisterProximitySensor()
    }

    override fun muteOrUnMute(mute: Boolean) {
        rtcAudioManagerRingInCall?.muteOrUnmuteIncomingCall(mute)
    }

    override fun updateSpeakerStatus(isSpeakerOn: Boolean, typeStatus: Int) {
        rtcAudioManager?.updateSpeakerStatus(isSpeakerOn, typeStatus)
    }

    override fun stopSounds() {
        rtcAudioManager?.stopAudioSignals()
        rtcAudioManagerRingInCall?.stopAudioSignals()
    }

    override fun updateRTCAudioMangerTypeStatus(callStatus: Int) {
        removeRTCAudioManagerRingIn()
        stopSounds()
        rtcAudioManager?.typeAudioManager = callStatus
    }

    override val isAnIncomingCallRinging: Boolean
        get() = rtcAudioManagerRingInCall != null

    override val audioManager: AppRTCAudioManager?
        get() = rtcAudioManager
}