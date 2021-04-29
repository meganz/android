package mega.privacy.android.app.meeting.fragments

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.*

class AbstractMeetingOnBoardingViewModel @ViewModelInject constructor(
    private val abstractMeetingOnBoardingRepository: AbstractMeetingOnBoardingRepository
) : ViewModel() {
    // Avatar
    private val _avatar = MutableLiveData<Bitmap>()
    val avatar: LiveData<Bitmap> = _avatar

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
     *  Select the video device to be used in calls
     *
     *  @param listener Receive information about requests.
     */
    fun setChatVideoInDevice(listener: MegaChatRequestListenerInterface?) {
        // Always try to start the video using the front camera
        var cameraDevice = VideoCaptureUtils.getFrontCamera()
        if (cameraDevice != null) {
            abstractMeetingOnBoardingRepository.setChatVideoInDevice(cameraDevice, listener)
        }
    }

}