package mega.privacy.android.app.meeting.activity

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.work.Operation
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.EditChatRoomNameListener
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.lollipop.megachat.calls.IndividualCallListener
import mega.privacy.android.app.meeting.listeners.DisableAudioVideoCallListener
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.meeting.listeners.OpenVideoDeviceListener
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.VideoCaptureUtils
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
    DisableAudioVideoCallListener.OnDisableAudioVideoCallback {

    var tips: MutableLiveData<String> = MutableLiveData<String>()

    // OnOffFab
    private val _micLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val _cameraLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
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
    val speakerLiveData: LiveData<AppRTCAudioManager.AudioDevice> = _speakerLiveData

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

    private val notificationNetworkStateObserver = Observer<Boolean> {
        _notificationNetworkState.value = it
    }

    // ChatRoom
    private val _chatRoomLiveData: MutableLiveData<MegaChatRoom?> = MutableLiveData<MegaChatRoom?>()
    val chatRoomLiveData: LiveData<MegaChatRoom?> = _chatRoomLiveData

    // Name of meeting
    private val _meetingNameLiveData: MutableLiveData<String> = MutableLiveData<String>()
    val meetingNameLiveData: LiveData<String> = _meetingNameLiveData

    // Link of meeting
    private val _meetingLinkLiveData: MutableLiveData<String> = MutableLiveData<String>()
    val meetingLinkLiveData: LiveData<String> = _meetingLinkLiveData

    private val audioOutputStateObserver =
        Observer<AppRTCAudioManager.AudioDevice> {
            if (_speakerLiveData.value != it) {
                _speakerLiveData.value = it
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

    private val meetingCreatedObserver =
        Observer<Long> {
            updateChatRoom(it)
            createChatLink(it)
        }

    private val linkRecoveredObserver =
        Observer<android.util.Pair<Long, String>> { chatAndLink ->
            _chatRoomLiveData.value?.let {
                if (chatAndLink.first == it.chatId) {
                    _meetingLinkLiveData.value = chatAndLink.second
                }
            }
        }

    private val titleMeetingChangeObserver =
        Observer<MegaChatRoom> {
            _chatRoomLiveData.value?.let {
                if (_chatRoomLiveData.value?.chatId == it.chatId) {
                    _meetingNameLiveData.value = getTitleChat(it)
                }
            }
        }

    init {
        LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean::class.java)
            .observeForever(notificationNetworkStateObserver)

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .observeForever(audioOutputStateObserver)

        LiveEventBus.get(EVENT_CHAT_TITLE_CHANGE, MegaChatRoom::class.java)
            .observeForever(titleMeetingChangeObserver)

        LiveEventBus.get(EVENT_MEETING_CREATED, Long::class.java)
            .observeForever(meetingCreatedObserver)

        LiveEventBus.get(EVENT_LINK_RECOVERED)
            .observeForever(linkRecoveredObserver as Observer<Any>)
    }

    /**
     * Method for creating a chat link
     *
     * @param chatId chat ID
     */
    fun createChatLink(chatId: Long) {
        meetingActivityRepository.createChatLink(chatId, CreateGroupChatWithPublicLink())
    }

    /**
     * Method for update the chatRoomLiveData
     *
     * @param chatId chat ID
     */
    fun updateChatRoom(chatId: Long) {
        _chatRoomLiveData.value = meetingActivityRepository.getChatRoom(chatId)
    }

    /**
     * Method to know if the chat exists
     *
     * @return True, if it exists. False, otherwise
     */
    fun isChatCreated(): Boolean {
        _chatRoomLiveData.value?.let {
            return true
        }
        return false
    }

    /**
     * Method to initiate the call with the microphone on
     */
    fun micInitiallyOn() {
        _micLiveData.value = true
    }

    /**
     * Method to initiate the call with the camera on
     */
    fun camInitiallyOn() {
        _cameraLiveData.value = true
    }

    /**
     * Method for setting a name for the meeting
     *
     * @param name The name
     */
    fun setMeetingsName(name: String) {
        _meetingNameLiveData.value = name
    }

    /**
     * Method for setting a title for the meeting
     *
     * @return The name
     */
    fun getMeetingName(): String? {
        return _meetingNameLiveData.value
    }

    /**
     * Response of clicking mic fab
     *
     * @param bOn true: turn on; false: turn off
     */
    fun clickMic(bOn: Boolean) {
        /**
         * check audio permission
         * if haven't been granted, ask for the permission and return
         */
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
        /**
         * check camera permission
         * if haven't been granted, ask for the permission and return
         */
        if (!cameraGranted) {
            _cameraPermissionCheck.value = true
            return
        }

        if (_chatRoomLiveData.value != null && _chatRoomLiveData.value!!.chatId != MEGACHAT_INVALID_HANDLE) {
            meetingActivityRepository.switchCamera(
                _chatRoomLiveData.value!!.chatId,
                bOn,
                DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
            )
        } else {
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

    /**
     * Method of obtaining the video
     *
     * @param chatId chatId
     * @param listener MeetingVideoListener
     */
    fun addLocalVideo(chatId: Long, listener: MeetingVideoListener?) {
        if (listener == null)
            return

        meetingActivityRepository.addLocalVideo(chatId, listener)
    }

    /**
     * Method of obtaining the local video
     *
     * @param chatId chatId
     * @param clientId client ID
     * @param hiRes If it's has High resolution
     * @param listener MeetingVideoListener
     */
    fun addRemoteVideo(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MeetingVideoListener
    ) {
        if (listener == null)
            return

        meetingActivityRepository.addRemoteVideo(chatId, clientId, hiRes, listener)
    }

    /**
     * Method of remove the local video
     *
     * @param chatId chatId
     * @param listener MeetingVideoListener
     */
    fun removeLocalVideo(chatId: Long, listener: MeetingVideoListener) {
        meetingActivityRepository.removeLocalVideo(chatId, listener)
    }

    fun removeRemoteVideo(
        chatId: Long,
        clientId: Long,
        hiRes: Boolean,
        listener: MeetingVideoListener
    ) {
        meetingActivityRepository.removeRemoteVideo(chatId, clientId, hiRes, listener)
    }

    fun requestHiResVideo(chatId: Long, clientId: Long, listener: MegaChatRequestListenerInterface){
        meetingActivityRepository.requestHiResVideo(chatId, clientId, listener)
    }

    fun stopHiResVideo(chatId: Long, clientId: Long, listener: MegaChatRequestListenerInterface){
        meetingActivityRepository.stopHiResVideo(chatId, clientId, listener)
    }

    fun requestLowResVideo(chatId: Long, clientId: MegaHandleList, listener: MegaChatRequestListenerInterface){
        meetingActivityRepository.requestLowResVideo(chatId, clientId, listener)
    }

    fun stopLowResVideo(chatId: Long, clientId: MegaHandleList, listener: MegaChatRequestListenerInterface){
        meetingActivityRepository.stopLowResVideo(chatId, clientId, listener)
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
            meetingActivityRepository.setChatVideoInDevice(cameraDevice, listener)
        }
    }

    fun releaseVideoDevice() {
        meetingActivityRepository.switchCameraBeforeStartMeeting(
            false,
            OpenVideoDeviceListener(MegaApplication.getInstance(), this)
        )
    }

    override fun onVideoDeviceOpened(isEnable: Boolean) {
        logDebug("onVideoDeviceOpened:: isEnable = $isEnable")
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
            MegaChatRequest.VIDEO -> {
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

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .removeObserver(audioOutputStateObserver)

        // Remove observer on network state
        LiveEventBus.get(EVENT_NETWORK_CHANGE, Boolean::class.java)
            .removeObserver(notificationNetworkStateObserver)

        LiveEventBus.get(
            EVENT_CHAT_TITLE_CHANGE, MegaChatRoom::class.java
        ).removeObserver(titleMeetingChangeObserver)

        LiveEventBus.get(EVENT_MEETING_CREATED, Long::class.java)
            .removeObserver(meetingCreatedObserver)

        LiveEventBus.get(EVENT_LINK_RECOVERED)
            .removeObserver(linkRecoveredObserver as Observer<Any>)
    }
}