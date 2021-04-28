package mega.privacy.android.app.meeting.activity

import android.util.Pair
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.EditChatRoomNameListener
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.listeners.DisableAudioVideoCallListener
import mega.privacy.android.app.meeting.listeners.OpenVideoDeviceListener
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

/**
 * It's very common that two or more fragments in Meeting activity need to communicate with each other.
 * These fragments can share a ViewModel using their activity scope to handle this communication.
 * MeetingActivityViewModel shares state of Mic, Camera and Speaker for all Fragments
 */

class MeetingActivityViewModel @ViewModelInject constructor(
    private val meetingActivityRepository: MeetingActivityRepository
) : ViewModel(), OpenVideoDeviceListener.OnOpenVideoDeviceCallback,
    DisableAudioVideoCallListener.OnDisableAudioVideoCallback,
    EditChatRoomNameListener.OnEditedChatRoomNameCallback {

    var tips: MutableLiveData<String> = MutableLiveData<String>()

    // OnOffFab
    private val _micLiveData: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>().apply {
            value = false }
    private val _cameraLiveData: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>().apply { value = false }
    private val _speakerLiveData: MutableLiveData<AppRTCAudioManager.AudioDevice> =
        MutableLiveData<AppRTCAudioManager.AudioDevice>().apply {
            value = if (MegaApplication.getInstance().audioManager == null) {
                AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
            } else {
                MegaApplication.getInstance().audioManager!!.selectedAudioDevice
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

    // ChatRoom
    private val _chatRoomLiveData: MutableLiveData<MegaChatRoom> = MutableLiveData<MegaChatRoom>()
    val chatRoomLiveData: LiveData<MegaChatRoom> = _chatRoomLiveData

    // Meeting
    private val _callLiveData: MutableLiveData<MegaChatCall> =
        MutableLiveData<MegaChatCall>().apply {
            value = null }
    val callLiveData: LiveData<MegaChatCall> = _callLiveData

    // title of meeting
    private val _meetingNameLiveData: MutableLiveData<String> =
        MutableLiveData<String>().apply {
            value = meetingActivityRepository.getInitialMeetingName()}

    val meetingNameLiveData: LiveData<String> = _meetingNameLiveData

    // subtitle of meeting
    private val _meetingSubtitleLiveData: MutableLiveData<String> = MutableLiveData<String>()
    val meetingSubtitleLiveData: LiveData<String> = _meetingSubtitleLiveData

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

    private val updateCallObserver =
        androidx.lifecycle.Observer<MegaChatCall> {
            _callLiveData.value = it
        }

    private val sessionStatusObserver =
        androidx.lifecycle.Observer<Pair<Long, MegaChatSession>> {
            //As the session has been established, I am no longer in the Request sent state
            _meetingSubtitleLiveData.value = "Duration 00:00"
        }

    init {
        LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean::class.java)
            .observeForever(notificationNetworkStateObserver)

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .observeForever(audioOutputStateObserver)

        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .observeForever(updateCallObserver)

        LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE)
            .observeForever(sessionStatusObserver as Observer<Any>)
    }

    fun updateChatAndCall(chatId: Long){
        _chatRoomLiveData.value = meetingActivityRepository.getChatRoom(chatId)
        _callLiveData.value = meetingActivityRepository.getMeeting(chatId)
    }

    fun isOneToOneCall(): Boolean {
        if (_chatRoomLiveData.value != null && !_chatRoomLiveData.value!!.isGroup) {
            return true
        }
        return false
    }

    fun isRequestSent(): Boolean {
        if (_callLiveData.value != null &&
            _callLiveData.value!!.callId != MEGACHAT_INVALID_HANDLE &&
            MegaApplication.isRequestSent(_callLiveData.value!!.callId)
        ) {
            return true
        }
        return false
    }

    /**
     * Method for determining whether to display the camera switching icon.
     *
     * @return True, if it is. False, if not.
     */
    fun isNecessaryToShowSwapCameraOption(): Boolean {
        if (_callLiveData.value == null) {
            return this._cameraLiveData.value == true
        } else {
            if (_callLiveData.value!!.hasLocalVideo() && !_callLiveData.value!!.isOnHold) {
                return true
            }

            return false
        }
    }

    fun startMeeting(listener: MegaChatRequestListenerInterface){
        if (_chatRoomLiveData.value != null && _chatRoomLiveData.value!!.chatId != MEGACHAT_INVALID_HANDLE) {
            //The chat exists
            meetingActivityRepository.startMeeting(_chatRoomLiveData.value!!.chatId, listener)
        } else {
            //The chat doesn't exists
           // It is necessary to create the public chat
        }
    }

    fun setTitleChat(newTitle: String) {
        meetingActivityRepository.setTitleChatRoom(
            _chatRoomLiveData.value!!.chatId,
            newTitle,
            EditChatRoomNameListener(MegaApplication.getInstance(), this)
        )
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .removeObserver(audioOutputStateObserver)

        // Remove observer on network state
        LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean::class.java)
            .removeObserver(notificationNetworkStateObserver)

        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .removeObserver(updateCallObserver)

        LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE)
            .removeObserver(sessionStatusObserver as Observer<Any>)
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

        when {
            _chatRoomLiveData.value != null && _chatRoomLiveData.value!!.chatId != MEGACHAT_INVALID_HANDLE -> {
                meetingActivityRepository.switchMic(
                    _chatRoomLiveData.value!!.chatId,
                    bOn,
                    DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
                )
            }
            else -> {
                //The chat is not yet created or the call is not yet established
                _micLiveData.value = bOn
                logDebug("open Mic: $_micLiveData.value")
                tips.value = when (bOn) {
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
        if(_chatRoomLiveData.value != null && _chatRoomLiveData.value!!.chatId != MEGACHAT_INVALID_HANDLE){
            meetingActivityRepository.switchCamera(
                _chatRoomLiveData.value!!.chatId,
                bOn,
                DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
            )
        }else{
            //The chat is not yet created or the call is not yet established
            meetingActivityRepository.switchCameraBeforeStartMeeting(
                bOn,
                OpenVideoDeviceListener(MegaApplication.getInstance(), this)
            )
        }
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

    override fun onVideoDeviceOpened(isEnable: Boolean) {
        _cameraLiveData.value = isEnable
        logDebug("open video: $_cameraLiveData.value")
        tips.value = when (isEnable) {
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

    override fun onDisableAudioVideo(chatId: Long, typeChange: Int, isEnable: Boolean) {
        when (typeChange) {
            MegaChatRequest.AUDIO -> {
                _micLiveData.value = isEnable
                logDebug("open Mic: $_micLiveData.value")
                tips.value = when (isEnable) {
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
            else -> {
                _cameraLiveData.value = isEnable
                logDebug("open video: $_cameraLiveData.value")
                tips.value = when (isEnable) {
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
        }
    }

    override fun onEditedChatRoomName(chatId: Long, name: String) {
        if (chatId == _chatRoomLiveData.value!!.chatId) {
            _meetingNameLiveData.value = name
        }
    }
}