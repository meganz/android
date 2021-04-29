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
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.*

class AbstractMeetingOnBoardingViewModel @ViewModelInject constructor(
    private val abstractMeetingOnBoardingRepository: AbstractMeetingOnBoardingRepository
) : ViewModel() {
    // Avatar
    private val _avatarLiveData = MutableLiveData<Bitmap>()
    val avatarLiveData: LiveData<Bitmap> = _avatarLiveData

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
        _avatarLiveData.value = abstractMeetingOnBoardingRepository.getDefaultAvatar()
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
                    it.first -> _avatarLiveData.value = it.second
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
                    else -> {
                        showDefaultAvatar()
                    }
                }
            }
        }
    }

    /**
     *  Select the video device to be used in calls
     *
     *  @param bFrontCamera true: front camera / false: back camera
     */
    fun setChatVideoInDevice(bFrontCamera: Boolean, listener: MegaChatRequestListenerInterface?) {
        // Always try to start the call using the front camera
        var cameraDevice = VideoCaptureUtils.getFrontCamera()
        if(!bFrontCamera) {
            cameraDevice = VideoCaptureUtils.getBackCamera()
        }
        if (cameraDevice != null) {
            abstractMeetingOnBoardingRepository.setChatVideoInDevice(cameraDevice, listener)
        }
    }

    fun activateLocalVideo(chatId: Long, listener: MeetingVideoListener) {
       abstractMeetingOnBoardingRepository.activateLocalVideo(chatId, listener)
    }

    fun activateRemoteVideo(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MeetingVideoListener
    ) {
        abstractMeetingOnBoardingRepository.activateRemoteVideo(chatId, clientId, hiRes, listener)
    }

    fun closeLocalVideo(chatId: Long, listener: MeetingVideoListener) {
        abstractMeetingOnBoardingRepository.closeLocalVideo(chatId, listener)
    }

    fun closeRemoteVideo(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MeetingVideoListener
    ) {
        abstractMeetingOnBoardingRepository.closeRemoteVideo(chatId, clientId, hiRes, listener)
    }

    fun getChatRoom(chatId: Long): MegaChatRoom {
        return abstractMeetingOnBoardingRepository.getChatRoom(chatId)
    }

    fun getChatCall(chatId: Long): MegaChatCall {
        return abstractMeetingOnBoardingRepository.getChatCall(chatId)
    }

    fun isMe(peerId: Long): Boolean {
        return abstractMeetingOnBoardingRepository.isMe(peerId)
    }

}