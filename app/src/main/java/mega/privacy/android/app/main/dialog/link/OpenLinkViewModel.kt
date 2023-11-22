package mega.privacy.android.app.main.dialog.link

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.main.dialog.link.OpenLinkDialogFragment.Companion.IS_CHAT_SCREEN
import mega.privacy.android.app.main.dialog.link.OpenLinkDialogFragment.Companion.IS_JOIN_MEETING
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class OpenLinkViewModel @Inject constructor(
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase,
    private val getChatLinkContentUseCase: GetChatLinkContentUseCase,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val chatManagement: ChatManagement,
    private val passcodeManagement: PasscodeManagement,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val isChatScreen = savedStateHandle.get<Boolean>(IS_CHAT_SCREEN) ?: false
    private val isJoinMeeting = savedStateHandle.get<Boolean>(IS_JOIN_MEETING) ?: false

    private val _state = MutableStateFlow(OpenLinkUiState())
    val state = _state.asStateFlow()

    fun onLinkChanged(link: String) {
        savedStateHandle[CURRENT_INPUT_LINK] = link
        _state.update { state ->
            state.copy(
                linkType = null,
                checkLinkResult = null,
                submittedLink = null
            )
        }
    }

    val inputLink: String
        get() = savedStateHandle.get<String>(CURRENT_INPUT_LINK).orEmpty()

    fun openLink(link: String) {
        _state.update { state -> state.copy(submittedLink = link) }
        if (link.isNotEmpty()) {
            val linkType = _state.value.linkType
            if (linkType == RegexPatternType.CONTACT_LINK) {
                openContactLink(link)
            } else if ((isChatScreen || isJoinMeeting) || linkType == RegexPatternType.CHAT_LINK) {
                openChatOrMeetingLink(link)
            } else {
                getLinkType(link)
            }
        }
    }

    private fun openChatOrMeetingLink(link: String) {
        viewModelScope.launch {
            val result = runCatching { getChatLinkContentUseCase(link) }
                .onFailure {
                    Timber.e(it)
                }
            _state.update { state -> state.copy(checkLinkResult = result) }
        }
    }

    fun openContactLink(link: String) {
        viewModelScope.launch {
            runCatching {
                getHandleFromContactLinkUseCase(link)
            }.onSuccess { handle ->
                _state.update { state -> state.copy(openContactLinkHandle = handle) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getLinkType(link: String) {
        val linkType = getUrlRegexPatternTypeUseCase(link)
        _state.update { state -> state.copy(linkType = linkType) }
    }

    /**
     * Start or answer a meeting with waiting room as a host
     *
     * @param chatId   Chat ID
     */
    fun startOrAnswerMeetingWithWaitingRoomAsHost(chatId: Long) {
        applicationScope.launch {
            runCatching {
                val call = getChatCallUseCase(chatId)
                val scheduledMeetingStatus = when (call?.status) {
                    ChatCallStatus.UserNoPresent -> ScheduledMeetingStatus.NotJoined(call.duration)

                    ChatCallStatus.Connecting,
                    ChatCallStatus.Joining,
                    ChatCallStatus.InProgress,
                    -> ScheduledMeetingStatus.Joined(call.duration)

                    else -> ScheduledMeetingStatus.NotStarted
                }
                if (scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted) {
                    runCatching {
                        getScheduledMeetingByChat(chatId)
                    }.onSuccess { scheduledMeetingList ->
                        scheduledMeetingList?.first()?.schedId?.let { schedId ->
                            startSchedMeetingWithWaitingRoom(
                                chatId = chatId, schedIdWr = schedId
                            )
                        }
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }
                } else {
                    answerCall(chatId = chatId)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Start scheduled meeting with waiting room
     *
     * @param chatId    Chat ID
     * @param schedIdWr Scheduled meeting ID
     */
    private fun startSchedMeetingWithWaitingRoom(chatId: Long, schedIdWr: Long) =
        applicationScope.launch {
            Timber.d("Start scheduled meeting with waiting room")
            runCatching {
                startMeetingInWaitingRoomChatUseCase(
                    chatId = chatId,
                    schedIdWr = schedIdWr,
                    enabledVideo = false,
                    enabledAudio = true
                )
            }.onSuccess { call ->
                call?.let {
                    call.chatId.takeIf { it != INVALID_HANDLE }?.let {
                        Timber.d("Meeting started")
                        openCall(call)
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Answer call
     *
     * @param chatId    Chat Id.
     */
    private fun answerCall(chatId: Long) {
        chatManagement.addJoiningCallChatId(chatId)

        applicationScope.launch {
            Timber.d("Answer call")
            runCatching {
                setChatVideoInDeviceUseCase()
                answerChatCallUseCase(chatId = chatId, video = false, audio = true)
            }.onSuccess { call ->
                call?.apply {
                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    CallUtil.clearIncomingCallNotification(callId)
                    openCall(call)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Open call
     *
     * @param call  [ChatCall]
     */
    private fun openCall(call: ChatCall) {
        chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
        chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
        CallUtil.openMeetingInProgress(
            MegaApplication.getInstance().applicationContext,
            call.chatId,
            true,
            passcodeManagement
        )
    }

    companion object {
        // handle case process recreate we need to save to SavedStateHandle
        const val CURRENT_INPUT_LINK = "CURRENT_INPUT_LINK"
        private const val INVALID_HANDLE = -1L
    }
}