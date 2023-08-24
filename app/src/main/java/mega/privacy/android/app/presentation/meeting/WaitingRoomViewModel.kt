package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.meeting.waitingroom.IsValidWaitingRoomUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import javax.inject.Inject


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
) : ViewModel() {

    private val _state = MutableStateFlow(WaitingRoomState())
    val state: StateFlow<WaitingRoomState> = _state

    init {
        monitorCallUpdates()
        monitorMeetingUpdates()
        retrieveMyAvatar()
    }

    /**
     * Load Meeting details
     *
     * @param chatId        Chat Room Id
     */
    fun loadMeetingDetails(chatId: Long) {
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
                        chatId = chatId,
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
     * Retrieve current Chat room call details
     *
     * @param chatId    Chat Room Id
     */
    private fun retrieveCallDetails(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getChatCall(chatId)
            }.onSuccess { call ->
                _state.update {
                    it.copy(
                        hasStarted = call?.hasStarted() ?: false
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Monitor current call updates
     */
    private fun monitorCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdates()
                .filter { it.changes == ChatCallChanges.Status && it.chatId == _state.value.chatId }
                .collectLatest { call ->
                    _state.update {
                        it.copy(
                            hasStarted = call.hasStarted()
                        )
                    }
                }
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
     * Check if [ChatCall] has started
     */
    private fun ChatCall.hasStarted(): Boolean =
        status == ChatCallStatus.UserNoPresent || status == ChatCallStatus.Connecting
                || status == ChatCallStatus.Joining || status == ChatCallStatus.InProgress

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
