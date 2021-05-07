package mega.privacy.android.app.meeting.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.listeners.InviteToChatRoomListener
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.listeners.DisableAudioVideoCallListener
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.meeting.listeners.OpenVideoDeviceListener
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil
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

    // Avatar
    private val _avatarLiveData = MutableLiveData<Bitmap>()
    val avatarLiveData: LiveData<Bitmap> = _avatarLiveData

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

    private val _currentChatId: MutableLiveData<Long> = MutableLiveData<Long>(MEGACHAT_INVALID_HANDLE)
    val currentChatId: LiveData<Long> = _currentChatId

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
            updateChatRoomId(it)
            createChatLink(it)
        }

    private val linkRecoveredObserver =
    Observer<android.util.Pair<Long, String>> { chatAndLink ->
        _currentChatId.value?.let {
                if (chatAndLink.first == it) {
                    _meetingLinkLiveData.value = chatAndLink.second
                }
            }
        }

    private val titleMeetingChangeObserver =
        Observer<MegaChatRoom> { chatRoom ->
            meetingActivityRepository.getChatRoom(_currentChatId.value!!)?.let {
                if (it.chatId == chatRoom.chatId) {
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

        // Show the default avatar (the Alphabet avatar) above all, then load the actual avatar
        showDefaultAvatar().invokeOnCompletion {
            loadAvatar(true)
        }
    }

    /**
     * Show the default avatar (the Alphabet avatar)
     */
    private fun showDefaultAvatar() = viewModelScope.launch {
        _avatarLiveData.value = meetingActivityRepository.getDefaultAvatar()
    }

    /**
     * Generate and show the round avatar based on the actual avatar stored in the cache folder.
     * Try to retrieve the avatar from the server if it has not been cached.
     * Showing the default avatar if the retrieve failed
     */
    private fun loadAvatar(retry: Boolean = false) {
        viewModelScope.launch {
            meetingActivityRepository.loadAvatar()?.also {
                when {
                    it.first -> _avatarLiveData.value = it.second
                    retry -> meetingActivityRepository.createAvatar(object :
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
     * Method for creating a chat link
     *
     * @param chatId chat ID
     */
    fun createChatLink(chatId: Long) {
        //The chat doesn't exist
        meetingActivityRepository.createChatLink(
            chatId,
            CreateGroupChatWithPublicLink()
        )
    }

    /**
     * Method for update the chatRoom ID
     *
     * @param chatId chat ID
     */
    fun updateChatRoomId(chatId: Long) {
        _currentChatId.value = chatId
    }

    /**
     * Method to know if the chat exists
     *
     * @return True, if it exists. False, otherwise
     */
    fun isChatCreated(): Boolean {
        _currentChatId.value?.let {
            if(it != MEGACHAT_INVALID_HANDLE)
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

        _currentChatId.value?.let {
            if(it != MEGACHAT_INVALID_HANDLE){
                meetingActivityRepository.switchMic(
                    it,
                    bOn,
                    DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
                )
                return
            }
        }

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

        _currentChatId.value?.let {
            if(it != MEGACHAT_INVALID_HANDLE){
                meetingActivityRepository.switchCamera(
                    it,
                    bOn,
                    DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
                )
                return
            }
        }

        //The chat is not yet created or the call is not yet established
        meetingActivityRepository.switchCameraBeforeStartMeeting(
            bOn,
            OpenVideoDeviceListener(MegaApplication.getInstance(), this)
        )
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
     * Method of remove the local video
     *
     * @param chatId chatId
     * @param listener MeetingVideoListener
     */
    fun removeLocalVideo(chatId: Long, listener: MeetingVideoListener?) {
        if (listener == null)
            return

        meetingActivityRepository.removeLocalVideo(chatId, listener)
    }

    /**
     *  Select the video device to be used in calls
     *
     *  @param listener Receive information about requests.
     */
    fun setChatVideoInDevice(listener: MegaChatRequestListenerInterface?) {
        // Always try to start the video using the front camera
        val cameraDevice = VideoCaptureUtils.getFrontCamera()
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

    override fun onVideoDeviceOpened(isVideoOn: Boolean) {
        logDebug("onVideoDeviceOpened:: isEnable = $isVideoOn")
        _cameraLiveData.value = isVideoOn
        logDebug("open video: $_cameraLiveData.value")
        tips.value = when (isVideoOn) {
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

    fun inviteToChat(context: Context, requestCode: Int, resultCode: Int, intent: Intent?) {
        logDebug("Result Code: $resultCode")
        if (intent == null) {
            LogUtil.logWarning("Intent is null")
            return
        }
        if (requestCode == Constants.REQUEST_ADD_PARTICIPANTS && resultCode == BaseActivity.RESULT_OK) {
            logDebug("Participants successfully added")
            val contactsData: List<String>? =
                intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS)
            if (contactsData != null) {
                currentChatId.value?.let {
                    InviteToChatRoomListener(context).inviteToChat(it, contactsData)
                }
            }
        } else {
            LogUtil.logError("Error adding participants")
        }

    }
}