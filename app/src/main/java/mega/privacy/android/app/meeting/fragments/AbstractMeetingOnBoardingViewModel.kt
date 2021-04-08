package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class AbstractMeetingOnBoardingViewModel @ViewModelInject constructor(
    private val abstractMeetingOnBoardingRepository: AbstractMeetingOnBoardingRepository
) : ViewModel() {
    // Avatar
    private val _avatar = MutableLiveData<Bitmap>()
    val avatar: LiveData<Bitmap> = _avatar

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

    init {
        // Show the default avatar (the Alphabet avatar) above all, then load the actual avatar
        showDefaultAvatar().invokeOnCompletion {
            loadAvatar(true)
        }
    }

    /**
     * Show the default avatar (the Alphabet avatar)
     */
    private fun showDefaultAvatar() = viewModelScope.launch {
        _avatar.value = abstractMeetingOnBoardingRepository.getDefaultAvatar()
    }

    /**
     * Generate and show the round avatar based on the actual avatar stored in the cache folder.
     * Try to retrieve the avatar from the server if it has not been cached.
     * Showing the default avatar if the retrieve failed
     */
    private fun loadAvatar(retry: Boolean = false) {
        viewModelScope.launch {
            abstractMeetingOnBoardingRepository.loadAvatar()?.also {
                when {
                    it.first -> _avatar.value = it.second
                    retry -> abstractMeetingOnBoardingRepository.createAvatar(object :
                        BaseListener(MegaApplication.getInstance()) {
                        override fun onRequestFinish(
                            api: MegaApiJava,
                            request: MegaRequest,
                            e: MegaError
                        ) {
                            if (request.type == MegaRequest.TYPE_GET_ATTR_USER
                                && request.paramType == MegaApiJava.USER_ATTR_AVATAR
                                && e.errorCode == MegaError.API_OK
                            ) {
                                loadAvatar()
                            } else {
                                showDefaultAvatar()
                            }
                        }
                    })
                    else -> showDefaultAvatar()
                }
            }
        }
    }

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
        if (abstractMeetingOnBoardingRepository.switchMic(bOn)) {
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
        if (abstractMeetingOnBoardingRepository.switchCamera(bOn)) {
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
        if (abstractMeetingOnBoardingRepository.switchSpeaker(bOn)) {
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