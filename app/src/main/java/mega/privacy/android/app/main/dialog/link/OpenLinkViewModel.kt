package mega.privacy.android.app.main.dialog.link

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import mega.privacy.android.domain.usecase.chat.link.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import timber.log.Timber

@HiltViewModel(assistedFactory = OpenLinkViewModel.Factory::class)
internal class OpenLinkViewModel @AssistedInject constructor(
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase,
    private val getChatLinkContentUseCase: GetChatLinkContentUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val chatManagement: ChatManagement,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Assisted(ASSISTED_IS_CHAT_SCREEN) private val isChatScreen: Boolean,
    @Assisted(ASSISTED_IS_JOIN_MEETING) private val isJoinMeeting: Boolean,
) : ViewModel() {

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

    /**
     * Resolves a successful [ChatLinkContent] and emits the matching navigation event.
     *
     * Only called from the Compose dialog. The legacy [OpenLinkDialogFragment]
     * performs the chat-room lookup itself, so it must not invoke this method.
     */
    fun handleChatLinkContent(content: ChatLinkContent) {
        if (content.link.isEmpty()) return
        viewModelScope.launch {
            when (content) {
                is ChatLinkContent.MeetingLink -> {
                    Timber.d("It's a meeting link")
                    runCatching { getChatRoomUseCase(content.chatHandle) }
                        .onSuccess { chatRoom ->
                            when {
                                chatRoom == null -> Unit
                                chatRoom.isMeeting && chatRoom.isWaitingRoom
                                        && chatRoom.ownPrivilege == ChatRoomPermission.Moderator -> {
                                    startOrAnswerMeetingWithWaitingRoomAsHost(content.chatHandle)
                                    _state.update { it.copy(dismissEvent = triggered) }
                                }

                                else -> _state.update {
                                    it.copy(joinMeetingEvent = triggered(content))
                                }
                            }
                        }
                        .onFailure { Timber.e(it) }
                }

                is ChatLinkContent.ChatLink -> {
                    Timber.d("It's a chat link")
                    _state.update { it.copy(openChatEvent = triggered(content)) }
                }
            }
        }
    }

    fun onJoinMeetingEventConsumed() {
        _state.update { it.copy(joinMeetingEvent = consumed()) }
    }

    fun onOpenChatEventConsumed() {
        _state.update { it.copy(openChatEvent = consumed()) }
    }

    fun onDismissEventConsumed() {
        _state.update { it.copy(dismissEvent = consumed) }
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
                        getScheduledMeetingByChatUseCase(chatId)
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
    private fun startSchedMeetingWithWaitingRoom(
        chatId: Long,
        schedIdWr: Long,
    ) =
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
                        openCall(call = call)
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
                    openCall(call = call)
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
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted(ASSISTED_IS_CHAT_SCREEN) isChatScreen: Boolean,
            @Assisted(ASSISTED_IS_JOIN_MEETING) isJoinMeeting: Boolean,
        ): OpenLinkViewModel
    }

    companion object {
        // handle case process recreate we need to save to SavedStateHandle
        const val CURRENT_INPUT_LINK = "CURRENT_INPUT_LINK"
        private const val INVALID_HANDLE = -1L
        private const val ASSISTED_IS_CHAT_SCREEN = "isChatScreen"
        private const val ASSISTED_IS_JOIN_MEETING = "isJoinMeeting"
    }
}