package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomTimestampMapper
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.meeting.waitingroom.IsValidWaitingRoomUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaChatError
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes


/**
 * Waiting room view model
 *
 * @property monitorConnectivityUseCase
 * @property getScheduledMeetingUseCase
 * @property timestampMapper
 * @property getMyAvatarFileUseCase
 * @property getMyAvatarColorUseCase
 * @property getUserFullNameUseCase
 * @property isValidWaitingRoomUseCase
 */
@HiltViewModel
class WaitingRoomViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getScheduledMeetingUseCase: GetScheduledMeetingByChat,
    private val timestampMapper: ChatRoomTimestampMapper,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val isValidWaitingRoomUseCase: IsValidWaitingRoomUseCase,
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val getChatCall: GetChatCall,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
) : ViewModel() {

    companion object {
        private const val WAITING_ROOM_TIMEOUT = 10
    }

    private val _state = MutableStateFlow(WaitingRoomState())
    val state: StateFlow<WaitingRoomState> = _state

    init {
        monitorCallUpdates()
        monitorMeetingUpdates()
        retrieveMyAvatar()
        startCountdownTimer()
    }

    /**
     * Load Meeting details
     *
     * @param chatId        Chat Room Id
     */
    fun loadMeetingDetails(chatId: Long) {
        _state.update { it.copy(chatId = chatId) }

        retrieveMeetingDetails(chatId)
        retrieveCallDetails(chatId)
    }

    /**
     * Retrieve current Chat Room Scheduled Meeting details
     *
     * @param chatId    Chat Room Id
     */
    private fun retrieveMeetingDetails(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                require(isValidWaitingRoom(chatId)) { "Not a Waiting Room" }
                requireNotNull(getScheduledMeetingUseCase(chatId)?.firstOrNull())
            }.onSuccess { meeting ->
                _state.update {
                    it.copy(
                        schedId = meeting.schedId,
                        title = meeting.title,
                        formattedTimestamp = meeting.getFormattedTimestamp(),
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update {
                    it.copy(finish = true)
                }
            }
        }
    }

    /**
     * Monitor current call updates
     */
    private fun monitorCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdates()
                .filter { call ->
                    call.chatId == _state.value.chatId
                            && (call.changes?.contains(ChatCallChanges.Status) ?: false ||
                            call.changes?.contains(ChatCallChanges.WaitingRoomAllow) ?: false ||
                            call.changes?.contains(ChatCallChanges.WaitingRoomDeny) ?: false)
                }
                .collectLatest { it.updateUiState() }
        }

    /**
     * Monitor current call updates
     */
    private fun monitorMeetingUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingUpdates()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { meeting ->
                    _state.update {
                        it.copy(
                            title = meeting.title,
                            formattedTimestamp = meeting.getFormattedTimestamp(),
                        )
                    }
                }
        }

    /**
     * Retrieve my current avatar
     */
    private fun retrieveMyAvatar() {
        viewModelScope.launch {
            runCatching {
                val avatar = async { getMyAvatarFileUseCase(false)?.absolutePath }
                val color = async { getMyAvatarColorUseCase() }
                val fullName = async { getUserFullNameUseCase(false) }
                ChatAvatarItem(
                    uri = avatar.await(),
                    color = color.await(),
                    placeholderText = fullName.await(),
                )
            }.onSuccess { avatar ->
                _state.update {
                    it.copy(
                        avatar = avatar
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    private fun retrieveCallDetails(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getChatCall(chatId)
            }.onSuccess { call ->
                call?.updateUiState()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Start countdown timer to leave the screen after [WAITING_ROOM_TIMEOUT] minutes.
     */
    private fun startCountdownTimer() {
        viewModelScope.launch {
            delay(WAITING_ROOM_TIMEOUT.minutes)
            _state.update { it.copy(finish = true) }
        }
    }

    /**
     * Checks that current Chat Room is a valid Waiting Room
     *
     * @param chatId    Chat Room Id to check
     * @return          true if is a Waiting Room, false otherwise
     */
    private suspend fun isValidWaitingRoom(chatId: Long): Boolean =
        runCatching { isValidWaitingRoomUseCase(chatId) }.getOrNull() ?: false

    /**
     * Get formatted Waiting Room timestamp
     */
    private fun ChatScheduledMeeting.getFormattedTimestamp(): String =
        timestampMapper.getWaitingRoomTimeFormatted(startDateTime!!, endDateTime!!)

    /**
     * Update UI state based on current [ChatCall]
     */
    private fun ChatCall.updateUiState() {
        if (this.shouldAnswer()) {
            answerChatCall()
        }

        _state.update {
            it.copy(
                callStarted = this.hasStarted(),
                joinCall = this.shouldJoin(),
                finish = changes?.contains(ChatCallChanges.WaitingRoomDeny)
                    ?: false // TODO Show Dialog
            )
        }
    }

    /**
     * Check if [ChatCall] should be joined
     */
    private fun ChatCall.shouldJoin(): Boolean =
        changes?.contains(ChatCallChanges.WaitingRoomAllow) ?: false || status == ChatCallStatus.Joining
                || status == ChatCallStatus.InProgress

    /**
     * Check if [ChatCall] should be answered
     */
    private fun ChatCall.shouldAnswer(): Boolean =
        status == ChatCallStatus.WaitingRoom || status == ChatCallStatus.UserNoPresent

    /**
     * Check if [ChatCall] has started
     */
    private fun ChatCall.hasStarted(): Boolean =
        when (status) {
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.UserNoPresent,
            ChatCallStatus.Connecting,
            ChatCallStatus.Joining,
            ChatCallStatus.InProgress,
            -> true

            else -> false
        }

    private fun answerChatCall() {
        viewModelScope.launch {
            runCatching {
                val state = state.value
                answerChatCallUseCase(state.chatId, state.cameraEnabled, state.micEnabled)
            }.onFailure { error ->
                if (error is MegaException && error.errorCode == MegaChatError.ERROR_EXIST) {
                    // Already requested, do nothing.
                } else {
                    Timber.e(error)
                }
            }
        }
    }

    /**
     * On mic enabled
     *
     * @param enable    true to enable microphone, false otherwise
     */
    fun onMicEnabled(enable: Boolean) {
        _state.update {
            it.copy(
                micEnabled = enable
            )
        }
    }

    /**
     * On camera enabled
     *
     * @param enable    true to enable camera, false otherwise
     */
    fun onCameraEnabled(enable: Boolean) {
        _state.update {
            it.copy(
                cameraEnabled = enable
            )
        }
    }

    /**
     * On speaker enabled
     *
     * @param enable    true to enable speaker, false otherwise
     */
    fun onSpeakerEnabled(enable: Boolean) {
        _state.update {
            it.copy(
                speakerEnabled = enable
            )
        }
    }
}
