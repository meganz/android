/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package mega.privacy.android.app.main.megachat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Looper
import android.os.Vibrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.OnProximitySensorListener
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.VideoCaptureUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.BluetoothStates
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.meeting.BroadcastAudioOutputUseCase
import org.webrtc.ThreadUtils
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * AppRTCAudioManager manages all audio related parts of the AppRTC demo.
 */
class AppRTCAudioManager @Inject constructor(
    private val apprtcContext: Context,
    private var isSpeakerOn: Boolean,
    type: Int,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val broadcastAudioOutputUseCase: BroadcastAudioOutputUseCase,
) {


    /**
     * Proximity sensor listener
     */
    var proximitySensorListener: OnProximitySensorListener? = null

    /**
     * Type audio manager
     */
    var typeAudioManager = type

    // Handles all tasks related to Bluetooth headset devices.
    private var bluetoothManager: AppRTCBluetoothManager? = null
    private val audioManager: AudioManager?
    private var amState = AudioManagerState.UNINITIALIZED
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var savedIsSpeakerPhoneOn = false
    private var savedIsMicrophoneMute = false
    private var hasWiredHeadset = false
    private var isTemporary = false
    private var isIncomingSound = false
    private var previousVolume = 0

    // Default audio device; speaker phone for video calls or earpiece for audio
    // only calls.
    private var defaultAudioDevice: AudioDevice? = null

    /**
     * Selected audio device
     * Contains the currently selected audio device.
     * This device is changed automatically using a certain scheme where e.g.
     * a wired headset "wins" over speaker phone. It is also possible for a
     * user to explicitly select a device (and overridden any predefined scheme).
     * See |userSelectedAudioDevice| for details.
     */
    var selectedAudioDevice: AudioDevice? = null
        private set

    // Contains the user-selected audio device which overrides the predefined
    // selection scheme.
    // Add support for explicit selection based on choice by userSelectedAudioDevice.
    private var userSelectedAudioDevice = AudioDevice.None

    // Proximity sensor object. It measures the proximity of an object in cm
    // relative to the view screen of a device and can therefore be used to
    // assist device switching (close to ear <=> use headset earpiece if
    // available, far from ear <=> use speaker phone).
    private var proximitySensor: AppRTCProximitySensor? = null

    // Contains a list of available audio devices. A Set collection is used to
    // avoid duplicate elements.
    private var audioDevices: MutableSet<AudioDevice?>? = HashSet()

    // Broadcast receiver for wired headset intent broadcasts.
    private val wiredHeadsetReceiver = WiredHeadsetReceiver()

    // Callback method for changes in audio focus.
    private var audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    init {
        check(Thread.currentThread() == Looper.getMainLooper().thread) { "Not on main thread!" }
        startBluetooth()
        audioManager = apprtcContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        start(isSpeakerOn)
        Timber.d("Default audio device is $defaultAudioDevice")
        AppRTCUtils.logDeviceInfo(TAG)
    }

    private fun startBluetooth() {
        if (bluetoothManager == null) {
            Timber.d("Starting bluetooth")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (hasPermissions(apprtcContext, Manifest.permission.BLUETOOTH_CONNECT)) {
                    bluetoothManager =
                        AppRTCBluetoothManager.create(apprtcContext, this)
                }
            } else {
                bluetoothManager =
                    AppRTCBluetoothManager.create(apprtcContext, this)
            }
        }
    }

    private fun stopBluetooth() {
        if (bluetoothManager == null) return

        Timber.d("Stopping bluetooth")
        bluetoothManager?.stop()
        bluetoothManager = null
    }


    private fun registerProximitySensor(): Boolean {
        // Create and initialize the proximity sensor.
        // Note that, the sensor will not be active until start() has been called.
        //This method will be called each time a state change is detected.
        if (proximitySensor != null) return false
        Timber.d("Registering proximity sensor")
        proximitySensor =
            AppRTCProximitySensor.create(apprtcContext) { onProximitySensorChangedState() }
        return true
    }


    /**
     * Get Audio Manager
     */
    fun getAudioManager() = audioManager

    /**
     * Start proximity sensor
     *
     * @return
     */
    fun startProximitySensor(): Boolean {
        if (registerProximitySensor()) {
            Timber.d("Starting proximity sensor")
            proximitySensor?.start()
            return true
        }
        return false
    }

    /**
     * This method is called when the proximity sensor reports a state change,
     * e.g. from "NEAR to FAR" or from "FAR to NEAR".
     */
    private fun onProximitySensorChangedState() {
        val isNear = proximitySensor?.sensorReportsNearState() ?: false
        if (isNear) {
            proximitySensor?.turnOffScreen()
            Timber.d("Screen off")

            if (VideoCaptureUtils.isFrontCameraInUse()) {
                // Sensor reports that a "handset is being held up to a person's ear", or "something is covering the light sensor".
                if (apprtcContext is MegaApplication && isSpeakerOn &&
                    (bluetoothManager?.bluetoothState != BluetoothStates.SCOConnected)
                ) {
                    Timber.d("Disabling the speakerphone and selecting Earpiece")
                    selectAudioDevice(AudioDevice.Earpiece, true)
                }
            }
        } else {
            // Sensor reports that a "handset is removed from a person's ear", or "the light sensor is no longer covered".
            proximitySensor?.turnOnScreen()
            Timber.d("Screen on")
            if (apprtcContext is MegaApplication && isSpeakerOn &&
                (bluetoothManager?.bluetoothState != BluetoothStates.SCOConnected)
            ) {
                Timber.d("Enabling the speakerphone: ")
                selectAudioDevice(AudioDevice.SpeakerPhone, true)
            }
        }
        proximitySensorListener?.needToUpdate(isNear)
    }


    private fun setValues() {
        if ((typeAudioManager != Constants.AUDIO_MANAGER_CALL_RINGING
                    && typeAudioManager != Constants.AUDIO_MANAGER_CALL_OUTGOING)
            || bluetoothManager?.bluetoothState == BluetoothStates.HeadsetAvailable
            || bluetoothManager?.bluetoothState == BluetoothStates.SCOConnecting
        ) {
            return
        }
        setAudioManagerValues()
    }

    private fun setAudioManagerValues() {
        Timber.d("Updating values of Chat Audio Manager...")
        if (typeAudioManager == Constants.AUDIO_MANAGER_CALL_OUTGOING) {
            Timber.d("If there was also an incoming call (stop incoming call sound)")
            stopAudioSignals()
            outgoingCallSound()
        } else if (typeAudioManager == Constants.AUDIO_MANAGER_CALL_RINGING) {
            Timber.d("If there is another incoming call (stop the sound of the previous incoming call)")
            stopAudioSignals()
            incomingCallSound()
            checkVibration()
        }
    }

    private fun outgoingCallSound() {
        if (audioManager == null || mediaPlayer?.isPlaying == true) return
        audioManager.setStreamVolume(
            AudioManager.STREAM_VOICE_CALL, audioManager.getStreamVolume(
                AudioManager.STREAM_VOICE_CALL
            ), 0
        )
        val res = getInstance().baseContext.resources
        val afd = res.openRawResourceFd(R.raw.outgoing_voice_video_call)
        if (mediaPlayer != null) {
            stopSound()
        }
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
            isLooping = true
        }
        try {
            mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            Timber.d("Preparing mediaPlayer")
            mediaPlayer?.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e(e, "Error preparing mediaPlayer")
            return
        }
        Timber.d("Start outgoing call sound")
        mediaPlayer?.start()
        isIncomingSound = false
    }

    private fun incomingCallSound() {
        if (audioManager == null || mediaPlayer?.isPlaying == true) return
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        if (mediaPlayer != null) {
            stopSound()
        }
        mediaPlayer = MediaPlayer()
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_RING, audioManager.getStreamVolume(
                    AudioManager.STREAM_RING
                ), 0
            )
        }
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
        )
        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_RING)
        mediaPlayer?.isLooping = true
        try {
            mediaPlayer?.setDataSource(getInstance().baseContext, ringtoneUri)
            Timber.d("Preparing mediaPlayer")
            mediaPlayer?.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e(e, "Error preparing mediaPlayer")
            return
        }
        Timber.d("Start incoming call sound")
        mediaPlayer?.start()
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        isIncomingSound = true
    }

    private fun checkVibration() {
        if (audioManager == null) return
        Timber.d(
            "Ringer mode: ${audioManager.ringerMode}, Stream volume: ${
                audioManager.getStreamVolume(
                    AudioManager.STREAM_RING
                )
            }, Voice call volume: ${audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)}",
        )
        if (CallUtil.participatingInACall()) {
            if (vibrator == null || vibrator?.hasVibrator() == false) return
            stopVibration()
            return
        }
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> stopVibration()
            AudioManager.RINGER_MODE_VIBRATE -> startVibration()
            AudioManager.RINGER_MODE_NORMAL -> if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0 && audioManager.isStreamMute(
                    AudioManager.STREAM_RING
                )
            ) {
                stopVibration()
            } else {
                startVibration()
            }
        }
    }

    private fun startVibration() {
        if (vibrator == null) {
            vibrator = apprtcContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator == null || vibrator?.hasVibrator() == false) return
        val pattern = longArrayOf(0, 1000, 500, 500, 1000)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        Timber.d("Vibration begins")
        vibrator?.vibrate(pattern, 0, audioAttributes)
    }

    /**
     * Method of checking whether the volume has been raised or lowered using the keys on the device.
     *
     * @param newVolume The new volume detected.
     */
    fun checkVolume(newVolume: Int) {
        if (newVolume < previousVolume) {
            muteOrUnMuteIncomingCall(true)
        } else if (newVolume > previousVolume && isPlayingIncomingCall) {
            muteOrUnMuteIncomingCall(false)
        }
        previousVolume = newVolume
    }

    /**
     * Method to know if an incoming call is ringing
     *
     * @return True, if it's ringing. False, if not.
     */
    private val isPlayingIncomingCall: Boolean
        get() = mediaPlayer?.isPlaying == true && isIncomingSound && audioManager != null

    /**
     * Method to mute or un-mute an incoming call.
     */
    fun muteOrUnMuteIncomingCall(isNecessaryMute: Boolean) {
        if (audioManager == null || audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT || isNecessaryMute && !isPlayingIncomingCall) {
            return
        }
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            if (isNecessaryMute) {
                stopVibration()
            } else {
                startVibration()
            }
            return
        }
        if (isNecessaryMute && !audioManager.isStreamMute(AudioManager.STREAM_RING)) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
            stopVibration()
            if (CallUtil.participatingInACall()) {
                getInstance().removeRTCAudioManagerRingIn()
            }
        } else if (!isNecessaryMute && audioManager.isStreamMute(AudioManager.STREAM_RING)) {
            adjustStreamVolume()
        }
    }

    private fun adjustStreamVolume() {
        audioManager?.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
        checkVibration()
    }

    /**
     * Method for stopping sound and vibration.
     */
    fun stopAudioSignals() {
        stopSound()
        stopVibration()
    }

    private fun stopSound() {
        try {
            if (mediaPlayer != null) {
                Timber.d("Stopping sound...")
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
                muteOrUnMuteIncomingCall(false)
            }
        } catch (e: Exception) {
            Timber.w(e, "Exception stopping player")
        }
    }

    private fun stopVibration() {
        try {
            if (vibrator == null || vibrator?.hasVibrator() == true) {
                Timber.d("Canceling vibration...")
                vibrator?.cancel()
                vibrator = null
            }
        } catch (e: Exception) {
            Timber.w(e, "Exception canceling vibrator")
        }
    }

    /**
     * Unregister proximity sensor
     *
     */
    fun unregisterProximitySensor() {
        if (proximitySensor == null) return
        Timber.d("Stopping proximity sensor")
        proximitySensor?.stop()
        proximitySensor = null
    }

    private fun speakerElements(isOn: Boolean) {
        setDefaultAudioDevice(if (isOn) AudioDevice.SpeakerPhone else AudioDevice.Earpiece)
        isSpeakerOn = isOn
    }

    /**
     * Update speaker status
     *
     * @param speakerStatus
     * @param type
     */
    fun updateSpeakerStatus(speakerStatus: Boolean, type: Int) {
        typeAudioManager = type
        Timber.d("Speaker status is $speakerStatus")
        selectAudioDevice(
            if (speakerStatus) AudioDevice.SpeakerPhone else AudioDevice.Earpiece,
            false
        )
    }

    private fun start(statusSpeaker: Boolean) {
        ThreadUtils.checkIsOnMainThread()
        if (amState == AudioManagerState.RUNNING) {
            Timber.e("AudioManager is already active")
            return
        }
        Timber.d("AudioManager starts... ")
        amState = AudioManagerState.RUNNING

        // Store current audio state so we can restore it when stop() is called.
        savedIsSpeakerPhoneOn = audioManager?.isSpeakerphoneOn ?: false
        savedIsMicrophoneMute = audioManager?.isMicrophoneMute ?: false
        hasWiredHeadset = hasWiredHeadset()

        // Create an AudioManager.OnAudioFocusChangeListener instance.
        // Called on the listener to notify if the audio focus for this listener has been changed.
// The |focusChange| value indicates whether the focus was gained, whether the focus was lost,
// and whether that loss is transient, or whether the new focus holder will hold it for an
// unknown amount of time.
// logging for now.
        audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange: Int ->
            val typeOfChange: String
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    typeOfChange = "AUDIOFOCUS_GAIN"
                    startBluetooth()
                }

                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                    typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT"
                    startBluetooth()
                }

                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> {
                    typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE"
                    startBluetooth()
                }

                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                    typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK"
                    startBluetooth()
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    typeOfChange = "AUDIOFOCUS_LOSS"
                    stopBluetooth()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT"
                    stopBluetooth()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK"
                    stopBluetooth()
                }

                else -> typeOfChange = "AUDIOFOCUS_INVALID"
            }
            Timber.d("Audio focus change $typeOfChange")
        }
        val typeStream: Int
        val typeFocus: Int
        when (typeAudioManager) {
            Constants.AUDIO_MANAGER_PLAY_VOICE_CLIP -> {
                typeStream = ChatUtil.STREAM_MUSIC_DEFAULT
                typeFocus = ChatUtil.AUDIOFOCUS_DEFAULT
            }

            Constants.AUDIO_MANAGER_CALL_RINGING -> {
                typeStream = AudioManager.STREAM_RING
                typeFocus = ChatUtil.AUDIOFOCUS_DEFAULT
            }

            else -> {
                typeStream = AudioManager.STREAM_VOICE_CALL
                typeFocus = AudioManager.AUDIOFOCUS_GAIN
            }
        }
        val result =
            audioManager?.requestAudioFocus(audioFocusChangeListener, typeStream, typeFocus)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Timber.d("Audio focus request granted for VOICE_CALL streams")
        } else {
            Timber.e("Audio focus request failed")
        }
        if (typeAudioManager != Constants.AUDIO_MANAGER_PLAY_VOICE_CLIP && typeAudioManager != Constants.AUDIO_MANAGER_CALL_RINGING) {
            Timber.d("Mode communication")
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        } else {
            Timber.d("Mode normal")
            audioManager?.mode = AudioManager.MODE_NORMAL
        }

        // Always disable microphone mute during a WebRTC call.
        setMicrophoneMute(false)

        // Set initial device states.
        userSelectedAudioDevice =
            if (typeAudioManager == Constants.AUDIO_MANAGER_PLAY_VOICE_CLIP || typeAudioManager == Constants.AUDIO_MANAGER_CREATING_JOINING_MEETING) {
                AudioDevice.SpeakerPhone
            } else {
                AudioDevice.None
            }
        selectedAudioDevice = AudioDevice.None
        audioDevices?.clear()

        // Do initial selection of audio device. This setting can later be changed
        // either by adding/removing a BT or wired headset or by covering/uncovering
        // the proximity sensor.
        speakerElements(statusSpeaker)

        // Register receiver for broadcast intents related to adding/removing a
        // wired headset.
        registerReceiver(wiredHeadsetReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        Timber.d("AudioManager started")
    }

    /**
     * Stop
     *
     */
    fun stop() {
        Timber.d("Stopping audio manager")
        ThreadUtils.checkIsOnMainThread()
        if (amState != AudioManagerState.RUNNING) {
            Timber.e("Trying to stop AudioManager in incorrect state: $amState")
            return
        }
        typeAudioManager = Constants.INVALID_CALL_STATUS
        amState = AudioManagerState.UNINITIALIZED
        unregisterReceiver(wiredHeadsetReceiver)
        stopAudioSignals()
        stopBluetooth()

        // Restore previously stored audio states.
        setSpeakerphoneOn(savedIsSpeakerPhoneOn)
        setMicrophoneMute(savedIsMicrophoneMute)
        audioManager?.mode = AudioManager.MODE_NORMAL

        // Abandon audio focus. Gives the previous focus owner, if any, focus.
        if (audioFocusChangeListener != null) audioManager?.abandonAudioFocus(
            audioFocusChangeListener
        )
        audioFocusChangeListener = null
        Timber.d("Abandoned audio focus for VOICE_CALL streams")
        unregisterProximitySensor()
        Timber.d("AudioManager stopped")
    }

    /**
     * Changes selection of the currently active audio device.
     */
    private fun setAudioDeviceInternal(device: AudioDevice?) {
        Timber.d("Selected audio device internal is $device")
        val availableDevice = getDeviceAvailable(device) ?: return
        Timber.d("Audio device internal finally selected is $availableDevice")
        assert(audioDevices?.contains(availableDevice) == true) { "Expected audio devices to contain $availableDevice" }
        when (availableDevice) {
            AudioDevice.SpeakerPhone -> {
                if (typeAudioManager == Constants.AUDIO_MANAGER_PLAY_VOICE_CLIP) {
                    audioManager?.mode = AudioManager.MODE_NORMAL
                }
                isSpeakerOn = true
                setSpeakerphoneOn(true)
            }

            AudioDevice.Earpiece -> {
                if (!isTemporary) {
                    isSpeakerOn = false
                }
                if (typeAudioManager == Constants.AUDIO_MANAGER_PLAY_VOICE_CLIP) {
                    audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                }
                setSpeakerphoneOn(false)
            }

            AudioDevice.WiredHeadset, AudioDevice.Bluetooth -> {
                isSpeakerOn = false
                setSpeakerphoneOn(false)
            }

            else -> {
                Timber.e("Invalid audio device selection: $availableDevice")
                return
            }
        }
        if (selectedAudioDevice != availableDevice) {
            selectedAudioDevice = availableDevice
            Timber.d("New audio device selected is $selectedAudioDevice")
            val newAudioDevice = selectedAudioDevice
            newAudioDevice?.let {
                applicationScope.launch {
                    runCatching {
                        broadcastAudioOutputUseCase(it)
                    }.onFailure { exception ->
                        Timber.e(exception.message)
                    }.onSuccess {
                        Timber.d("Audio output has changed")
                    }
                }
            }
            setValues()
        }
    }

    /**
     * Changes default audio device.
     */
    private fun setDefaultAudioDevice(defaultDevice: AudioDevice) {
        ThreadUtils.checkIsOnMainThread()
        when (defaultDevice) {
            AudioDevice.SpeakerPhone -> defaultAudioDevice = AudioDevice.SpeakerPhone
            AudioDevice.Earpiece -> defaultAudioDevice = if (hasEarpiece()) {
                defaultDevice
            } else {
                AudioDevice.SpeakerPhone
            }

            else -> Timber.e("Invalid default audio device selection: $defaultDevice")
        }
        Timber.d("Set default audio device is $defaultAudioDevice")
        updateAudioDeviceState()
    }

    /**
     * Method to know if an AudioDevice is available on the device
     *
     * @param device AudioDevice
     * @return True, if the array of available audioDevices contains that device. False, otherwise.
     */
    private fun isDeviceAvailable(device: AudioDevice?): Boolean {
        return audioDevices != null && audioDevices!!.contains(device)
    }

    private fun getDeviceAvailable(device: AudioDevice?): AudioDevice? {
        if (isDeviceAvailable(device)) return device
        var updatedDevice = AudioDevice.WiredHeadset
        if (isDeviceAvailable(updatedDevice)) return updatedDevice
        updatedDevice = AudioDevice.Bluetooth
        if (isDeviceAvailable(updatedDevice)) return updatedDevice
        updatedDevice = AudioDevice.SpeakerPhone
        if (isDeviceAvailable(updatedDevice)) return updatedDevice
        Timber.e("Can not select $updatedDevice, from available $audioDevices")
        return null
    }

    /**
     * Changes selection of the currently active audio device.
     */
    fun selectAudioDevice(device: AudioDevice?, temporary: Boolean) {
        check(Thread.currentThread() == Looper.getMainLooper().thread) { "Not on main thread!" }
        Timber.d("Selected audio device is $device")
        val availableDevice = getDeviceAvailable(device) ?: return
        Timber.d("Audio device finally selected is $availableDevice")
        isTemporary = temporary
        userSelectedAudioDevice = availableDevice
        updateAudioDeviceState()
    }

    /**
     * Helper method for receiver registration.
     */
    private fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        apprtcContext.registerReceiver(receiver, filter)
    }

    /**
     * Helper method for de-registration of an existing receiver.
     */
    private fun unregisterReceiver(receiver: BroadcastReceiver) {
        apprtcContext.unregisterReceiver(receiver)
    }

    /**
     * Sets the speaker phone mode.
     */
    private fun setSpeakerphoneOn(on: Boolean) {
        if (audioManager == null) return
        val wasOn = audioManager.isSpeakerphoneOn
        if (wasOn == on) {
            return
        }
        audioManager.isSpeakerphoneOn = on
    }

    /**
     * Sets the microphone mute state.
     */
    private fun setMicrophoneMute(on: Boolean) {
        if (audioManager == null) return
        val wasMuted = audioManager.isMicrophoneMute

        if (wasMuted == on) {
            return
        }
        Timber.d("Is microphone mute $on")
        audioManager.isMicrophoneMute = on
    }

    /**
     * Gets the current earpiece state.
     */
    private fun hasEarpiece(): Boolean =
        apprtcContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    /**
     * Checks whether a wired headset is connected or not.
     * This is not a valid indication that audio playback is actually over
     * the wired headset as audio routing depends on other conditions. We
     * only use it as an early indicator (during initialization) of an attached
     * wired headset.
     */
    @Deprecated("")
    private fun hasWiredHeadset(): Boolean {
        val devices = audioManager?.getDevices(
            AudioManager.GET_DEVICES_INPUTS or AudioManager.GET_DEVICES_OUTPUTS
        )
        devices?.forEach { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                    Timber.d("Found wired headset")
                    return true
                }

                AudioDeviceInfo.TYPE_USB_DEVICE -> {
                    Timber.d("USB audio device")
                    return true
                }

                else -> {}
            }
        }

        return false
    }

    /**
     * Change user selected audio device for headphone
     *
     * @param device [AudioDevice]
     */
    fun changeUserSelectedAudioDeviceForHeadphone(device: AudioDevice) {
        userSelectedAudioDevice = device
    }

    /**
     * Updates list of possible audio devices and make new device selection.
     */
    fun updateAudioDeviceState() {
        startBluetooth()
        ThreadUtils.checkIsOnMainThread()
        if (bluetoothManager != null) {
            Timber.d("Update audio device state. Wired headset $hasWiredHeadset, Bluetooth ${bluetoothManager?.bluetoothState}")
        }
        Timber.d("Device status:. available $audioDevices, selected $selectedAudioDevice, user selected $userSelectedAudioDevice")
        // Check if any Bluetooth headset is connected. The internal BT state will
        // change accordingly.
        if (bluetoothManager?.bluetoothState == BluetoothStates.HeadsetAvailable
            || bluetoothManager?.bluetoothState == BluetoothStates.HeadsetUnavailable
            || bluetoothManager?.bluetoothState == BluetoothStates.SCODisconnecting
        ) {
            bluetoothManager?.updateDevice()
        }
        // Update the set of available audio devices.
        val newAudioDevices: MutableSet<AudioDevice?> = HashSet()
        if (bluetoothManager?.bluetoothState == BluetoothStates.SCOConnected
            || bluetoothManager?.bluetoothState == BluetoothStates.SCOConnecting
            || bluetoothManager?.bluetoothState == BluetoothStates.HeadsetAvailable
        ) {
            newAudioDevices.add(AudioDevice.Bluetooth)
        }

        if (hasWiredHeadset) {
            // If a wired headset is connected, then it is the only possible option.
            newAudioDevices.add(AudioDevice.WiredHeadset)
        }

        // No wired headset, hence the audio-device list can contain speaker
        // phone (on a tablet), or speaker phone and earpiece (on mobile phone).
        newAudioDevices.add(AudioDevice.SpeakerPhone)
        if (hasEarpiece()) {
            newAudioDevices.add(AudioDevice.Earpiece)
        }
        var audioDeviceSetUpdated: Boolean
        // Store state which is set to true if the device list has changed.
        if (audioDevices == newAudioDevices) {
            //Equals
            audioDeviceSetUpdated = false
        } else {
            audioDevices?.clear()
            // Update the existing audio device set.
            audioDevices = newAudioDevices
            audioDeviceSetUpdated = true
        }
        if (bluetoothManager?.bluetoothState == BluetoothStates.HeadsetUnavailable && userSelectedAudioDevice == AudioDevice.Bluetooth) {
            userSelectedAudioDevice = AudioDevice.Earpiece
        }

        when {
            userSelectedAudioDevice == AudioDevice.None -> userSelectedAudioDevice = when {
                isSpeakerOn -> AudioDevice.SpeakerPhone
                hasWiredHeadset -> AudioDevice.WiredHeadset
                bluetoothManager != null && bluetoothManager?.bluetoothState == BluetoothStates.HeadsetAvailable -> AudioDevice.Bluetooth
                else -> AudioDevice.Earpiece
            }

            hasWiredHeadset && userSelectedAudioDevice != AudioDevice.SpeakerPhone -> userSelectedAudioDevice =
                AudioDevice.WiredHeadset

            bluetoothManager?.bluetoothState == BluetoothStates.HeadsetAvailable && userSelectedAudioDevice != AudioDevice.SpeakerPhone -> userSelectedAudioDevice =
                AudioDevice.Bluetooth

            userSelectedAudioDevice != AudioDevice.SpeakerPhone -> userSelectedAudioDevice =
                AudioDevice.Earpiece

        }

        Timber.d("Selected audio device $userSelectedAudioDevice")

        // Need to start Bluetooth if it is available and user either selected it explicitly or
        // user did not select any output device.
        val needBluetoothAudioStart =
            bluetoothManager?.bluetoothState == BluetoothStates.HeadsetAvailable
                    && (userSelectedAudioDevice == AudioDevice.Earpiece
                    || userSelectedAudioDevice == AudioDevice.Bluetooth)

        // Need to stop Bluetooth audio if user selected different device and
        // Bluetooth SCO connection is established or in the process.
        val needBluetoothAudioStop =
            (bluetoothManager?.bluetoothState == BluetoothStates.SCOConnected
                    || bluetoothManager?.bluetoothState == BluetoothStates.SCOConnecting)
                    && (userSelectedAudioDevice != AudioDevice.Earpiece
                    && userSelectedAudioDevice != AudioDevice.Bluetooth)

        if (bluetoothManager?.bluetoothState in listOf(
                BluetoothStates.HeadsetAvailable,
                BluetoothStates.SCOConnecting,
                BluetoothStates.SCOConnected
            )
        ) {
            Timber.d(
                "Need Bluetooth audio. Start $needBluetoothAudioStart. Stop $needBluetoothAudioStop. Bluetooth state ${bluetoothManager?.bluetoothState}"
            )
        }

        // Start or stop Bluetooth SCO connection given states set earlier.
        if (needBluetoothAudioStop) {
            bluetoothManager?.stopScoAudio()
            bluetoothManager?.updateDevice()
        }
        if (needBluetoothAudioStart && !needBluetoothAudioStop) {
            // Attempt to start Bluetooth SCO audio (takes a few second to start).
            if (bluetoothManager?.startScoAudio() == false) {
                // Remove BLUETOOTH from list of available devices since SCO failed.
                audioDevices?.remove(AudioDevice.Bluetooth)
                audioDeviceSetUpdated = true
            }
        }
        updateAudioDevice(audioDeviceSetUpdated)
        Timber.d("Updated audio device state")
    }

    private fun updateAudioDevice(audioDeviceSetUpdated: Boolean) {
        val newAudioDevice =
            when {
                bluetoothManager?.bluetoothState == BluetoothStates.SCOConnected -> if (userSelectedAudioDevice == AudioDevice.SpeakerPhone) {
                    AudioDevice.SpeakerPhone
                } else {
                    AudioDevice.Bluetooth
                }

                hasWiredHeadset -> if (userSelectedAudioDevice == AudioDevice.SpeakerPhone) {

                    AudioDevice.SpeakerPhone
                } else {

                    AudioDevice.WiredHeadset
                }

                userSelectedAudioDevice == AudioDevice.None -> if (typeAudioManager == Constants.AUDIO_MANAGER_CALL_RINGING) {
                    AudioDevice.SpeakerPhone
                } else {
                    defaultAudioDevice
                }

                else -> userSelectedAudioDevice

            }
        defaultAudioDevice = newAudioDevice
        Timber.d("Default audio device $newAudioDevice")

        // Switch to new device but only if there has been any changes.
        if (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated) {
            // Do the required device switch.
            setAudioDeviceInternal(newAudioDevice)
            Timber.d("New device status: available $audioDevices, selected $newAudioDevice")
        }
        Timber.d("Updated audio device state")
    }


    /**
     * AudioManager state.
     */
    enum class AudioManagerState {
        /**
         * Uninitialized
         */
        UNINITIALIZED,

        /**
         * Running
         */
        RUNNING
    }

    /* Receiver which handles changes in wired headset availability. */
    private inner class WiredHeadsetReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra("state", unplugged)
            val microphone = intent.getIntExtra("microphone", noMic)
            val name = intent.getStringExtra("name")
            Timber.d("WiredHeadsetReceiver state $state, microphone $microphone, name $name")

            hasWiredHeadset = state == plugged
            if (state == plugged) {
                changeUserSelectedAudioDeviceForHeadphone(AudioDevice.WiredHeadset)
            }
            updateAudioDeviceState()
        }

        private val unplugged = 0
        private val plugged = 1
        private val noMic = 0
    }

    companion object {
        private const val TAG = "AppRTCAudioManager"

        /**
         * Construction.
         */
        @JvmStatic
        fun create(
            context: Context,
            isSpeakerOn: Boolean,
            type: Int,
            applicationScope: CoroutineScope,
            outputAudioUseCase: BroadcastAudioOutputUseCase,
        ): AppRTCAudioManager {
            return AppRTCAudioManager(
                context,
                isSpeakerOn,
                type,
                applicationScope,
                outputAudioUseCase
            )
        }
    }
}