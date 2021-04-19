package mega.privacy.android.app.meeting.activity

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringResourcesUtils.getString

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
    private var storageGranted: Boolean = false
    private val _storagePermissionCheck: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val storagePermissionCheck: LiveData<Boolean> = _storagePermissionCheck

    /**
     * Response of clicking mic fab
     *
     * @param bOn true: turn on; off: turn off
     */
    fun clickMic(bOn: Boolean) {
        if (!recordAudioGranted) {
            _recordAudioPermissionCheck.value = true
            return
        }
        if (meetingActivityRepository.switchMic(bOn)) {
            _micLiveData.value = bOn
            when (bOn) {
                true -> tips.value = getString(
                    R.string.general_mic_mute,
                    "unmute"
                )
                false -> tips.value = getString(
                    R.string.general_mic_mute,
                    "mute"
                )
            }
        }
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
        if (meetingActivityRepository.switchCamera(bOn)) {
            _cameraLiveData.value = bOn
            when (bOn) {
                true -> tips.value = getString(
                    R.string.general_camera_disable,
                    "enable"
                )
                false -> tips.value = getString(
                    R.string.general_camera_disable,
                    "disable"
                )
            }
        }
    }

    /**
     * Response of clicking Speaker Fab
     *
     * @param bOn true: switch to speaker; false: switch to headphone
     */
    fun clickSpeaker(bOn: Boolean) {
        if (meetingActivityRepository.switchSpeaker(bOn)) {
            _speakerLiveData.value = bOn
            when (bOn) {
                true -> tips.value = getString(
                    R.string.general_speaker_headphone,
                    "Speaker"
                )
                false -> tips.value = getString(
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

    /**
     * Set storage permission
     *
     * @param storagePermission true: the permission is granted
     */
    fun setStoragePermission(storagePermission: Boolean) {
        storageGranted = storagePermission
    }
}