package mega.privacy.android.app.meeting.activity

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EVENT_AUDIO_OUTPUT_CHANGE
import mega.privacy.android.app.utils.Constants.EVENT_NETWORK_CHANGE
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.showSnackbar
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest

/**
 * It's very common that two or more fragments in Meeting activity need to communicate with each other.
 * These fragments can share a ViewModel using their activity scope to handle this communication.
 * MeetingActivityViewModel shares state of Mic, Camera and Speaker for all Fragments
 */

class MeetingActivityViewModel @ViewModelInject constructor(
    private val meetingActivityRepository: MeetingActivityRepository
) : ViewModel() {

    var tips: MutableLiveData<String> = MutableLiveData<String>()

    // OnOffFab
    private val _micLiveData: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>().apply {
            value = false }
    private val _cameraLiveData: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>().apply { value = false }
    private val _speakerLiveData: MutableLiveData<AppRTCAudioManager.AudioDevice> =
        MutableLiveData<AppRTCAudioManager.AudioDevice>().apply {
            if (MegaApplication.getInstance().audioManager == null) {
                value = AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
            } else {
                value = MegaApplication.getInstance().audioManager!!.selectedAudioDevice
            }
        }

    val micLiveData: LiveData<Boolean> = _micLiveData
    val cameraLiveData: LiveData<Boolean> = _cameraLiveData
    val speakerLiveData = _speakerLiveData

    // Permissions
    private var cameraGranted: Boolean = false
    private val _cameraPermissionCheck: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val cameraPermissionCheck: LiveData<Boolean> = _cameraPermissionCheck
    private var recordAudioGranted: Boolean = false
    private val _recordAudioPermissionCheck: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>(false)
    val recordAudioPermissionCheck: LiveData<Boolean> = _recordAudioPermissionCheck

    // Network State
    private val _notificationNetworkState = MutableLiveData<Boolean>()

    // Observe this property to get online/offline notification. true: online / false: offline
    val notificationNetworkState: LiveData<Boolean> = _notificationNetworkState

    private val notificationNetworkStateObserver = androidx.lifecycle.Observer<Boolean> {
        _notificationNetworkState.value = it
    }

    private val audioOutputStateObserver =
        androidx.lifecycle.Observer<AppRTCAudioManager.AudioDevice> {
            if(speakerLiveData.value != it){
                speakerLiveData.value = it
                when (it) {
                    AppRTCAudioManager.AudioDevice.EARPIECE -> {
                        tips.value = getString(R.string.speaker_off, "Speaker")
                    }
                    AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                        tips.value = getString(R.string.general_speaker_headphone, "Speaker")
                    }
                    else -> {
                        tips.value = getString(R.string.general_speaker_headphone, "Headphone")
                    }
                }
            }
        }

    // Receive information about requests.
    val listener = object : ChatBaseListener(
        MegaApplication.getInstance()
    ) {
        override fun onRequestFinish(
            api: MegaChatApiJava,
            request: MegaChatRequest,
            e: MegaChatError
        ) {
            when (request.type) {
                MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE -> {
                    _cameraLiveData.value = request.flag
                    logDebug("open video: $_cameraLiveData.value")
                    tips.value = when (request.flag) {
                        true -> getString(
                            R.string.general_camera_disable,
                            "enable"
                        )
                        false -> getString(
                            R.string.general_camera_disable,
                            "disable"
                        )
                    }
                }

                MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL -> {
                    _micLiveData.value = request.flag
                    logDebug("open Mic: $_micLiveData.value")
                    tips.value = when (request.flag) {
                        true -> getString(
                            R.string.general_mic_mute,
                            "unmute"
                        )
                        false -> getString(
                            R.string.general_mic_mute,
                            "mute"
                        )
                    }
                }
            }
        }
    }

    init {
        LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean::class.java)
            .observeForever(notificationNetworkStateObserver)

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .observeForever(audioOutputStateObserver)
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .removeObserver(audioOutputStateObserver)

        // Remove observer on network state
        LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean::class.java)
            .removeObserver(notificationNetworkStateObserver)
    }

    /**
     * Response of clicking mic fab
     *
     * @param bOn true: turn on; false: turn off
     */
    fun clickMic(bOn: Boolean) {
        if (!recordAudioGranted) {
            _recordAudioPermissionCheck.value = true
            return
        }
        meetingActivityRepository.switchMic(bOn, listener)
    }

    /**
     * Response of clicking camera Fab
     *
     * @param bOn true: turn on; off: turn off
     */
    fun clickCamera(bOn: Boolean) {
        if (!cameraGranted) {
            _cameraPermissionCheck.value = true
            return
        }
        meetingActivityRepository.switchCamera(bOn, listener)
    }

    /**
     * Response of clicking Speaker Fab
     */
    fun clickSpeaker() {
        when (_speakerLiveData.value) {
            AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                meetingActivityRepository.switchSpeaker(AppRTCAudioManager.AudioDevice.EARPIECE)
            }
            else -> {
                meetingActivityRepository.switchSpeaker(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)
            }
        }
    }

    /**
     * Set camera permission
     *
     * @param cameraPermission true: the permission is granted
     */
    fun setCameraPermission(cameraPermission: Boolean) {
        cameraGranted = cameraPermission
    }

    /**
     * Set record & audio permission
     *
     * @param recordAudioPermission true: the permission is granted
     */
    fun setRecordAudioPermission(recordAudioPermission: Boolean) {
        recordAudioGranted = recordAudioPermission
    }
}