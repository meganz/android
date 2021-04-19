package mega.privacy.android.app.meeting.activity

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface

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
        MutableLiveData<Boolean>().apply { value = false }
    private val _cameraLiveData: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>().apply { value = false }
    private val _speakerLiveData: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>().apply { value = true }

    val micLiveData: LiveData<Boolean> = _micLiveData
    val cameraLiveData: LiveData<Boolean> = _cameraLiveData
    val speakerLiveData: LiveData<Boolean> = _speakerLiveData

    // Permissions
    private var cameraGranted: Boolean = false
    private val _cameraPermissionCheck: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val cameraPermissionCheck: LiveData<Boolean> = _cameraPermissionCheck
    private var recordAudioGranted: Boolean = false
    private val _recordAudioPermissionCheck: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>(false)
    val recordAudioPermissionCheck: LiveData<Boolean> = _recordAudioPermissionCheck


    // Receive information about requests.
    val listener = object : MegaChatRequestListenerInterface {
        override fun onRequestStart(api: MegaChatApiJava?, request: MegaChatRequest?) {

        }

        override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest?) {

        }

        override fun onRequestFinish(
            api: MegaChatApiJava?,
            request: MegaChatRequest?,
            e: MegaChatError?
        ) {
            when (request?.type) {
                MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE -> {
                    _cameraLiveData.value = request.flag
                    LogUtil.logDebug("open video: $_cameraLiveData.value")
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
                    LogUtil.logDebug("open Mic: $_micLiveData.value")
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

        override fun onRequestTemporaryError(
            api: MegaChatApiJava?,
            request: MegaChatRequest?,
            e: MegaChatError?
        ) {

        }
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
     *
     * @param bOn true: switch to speaker; false: switch to headphone
     */
    fun clickSpeaker(bOn: Boolean) {
        if (meetingActivityRepository.switchSpeaker(bOn)) {
            _speakerLiveData.value = bOn
            tips.value = when (bOn) {
                true -> getString(
                    R.string.general_speaker_headphone,
                    "Speaker"
                )
                false -> getString(
                    R.string.general_speaker_headphone,
                    "Headphone"
                )
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