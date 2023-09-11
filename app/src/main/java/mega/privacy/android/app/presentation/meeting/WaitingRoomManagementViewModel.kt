package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.mapper.GetPluralStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.chat.GetMessageSenderNameUseCase
import mega.privacy.android.domain.usecase.meeting.AllowUsersJoinCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallInProgress
import mega.privacy.android.domain.usecase.meeting.KickUsersFromCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import timber.log.Timber
import javax.inject.Inject

/**
 * Waiting room management view model
 *
 * @property monitorChatCallUpdates                 [MonitorChatCallUpdates]
 * @property getMessageSenderNameUseCase            [GetMessageSenderNameUseCase]
 * @property getScheduledMeetingByChat              [GetScheduledMeetingByChat]
 * @property getChatCallInProgress                  [GetChatCallInProgress]
 * @property monitorScheduledMeetingUpdates         [MonitorScheduledMeetingUpdates]
 * @property allowUsersJoinCallUseCase              [AllowUsersJoinCallUseCase]
 * @property getPluralStringFromStringResMapper     [GetPluralStringFromStringResMapper]
 * @property kickUsersFromCallUseCase               [KickUsersFromCallUseCase]
 * @property state                                  Current view state as [WaitingRoomManagementState]
 */
@HiltViewModel
class WaitingRoomManagementViewModel @Inject constructor(
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val getMessageSenderNameUseCase: GetMessageSenderNameUseCase,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val getChatCallInProgress: GetChatCallInProgress,
    private val allowUsersJoinCallUseCase: AllowUsersJoinCallUseCase,
    private val kickUsersFromCallUseCase: KickUsersFromCallUseCase,
    private val getPluralStringFromStringResMapper: GetPluralStringFromStringResMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(WaitingRoomManagementState())

    /**
     * State flow
     */
    val state: StateFlow<WaitingRoomManagementState> = _state

    init {
        getCall()
        startMonitoringChatCallUpdates()
        startMonitoringScheduledMeetingUpdates()
    }

    /**
     * Set chat id of the call opened
     *
     * @param chatId
     */
    fun setChatIdCallOpened(chatId: Long) = _state.update { state ->
        state.copy(
            chatIdOfCallOpened = chatId
        )
    }

    private fun getCall() =
        viewModelScope.launch {
            runCatching {
                getChatCallInProgress()
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { call ->
                call?.let {
                    it.waitingRoom?.apply {
                        peers?.let { peers ->
                            checkWaitingRoomParticipants(peers, chatId = call.chatId)
                        }
                    }
                }
            }
        }

    /**
     * Get chat call updates
     */
    private fun startMonitoringChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdates()
                .collectLatest { call ->
                    call.changes?.apply {
                        if (contains(ChatCallChanges.WaitingRoomUsersEntered)) {
                            Timber.d("Users entered or left in waiting room")
                            call.waitingRoom?.apply {
                                peers?.let {
                                    checkWaitingRoomParticipants(it, chatId = call.chatId)
                                }
                            }
                        }

                        if (contains(ChatCallChanges.WaitingRoomUsersLeave)) {
                            Timber.d("Users entered or left in waiting room")
                            call.waitingRoom?.apply {
                                resetValues(call.chatId, this.peers)
                            }
                        }
                    }
                }
        }


    /**
     * Get scheduled meeting updates
     */
    private fun startMonitoringScheduledMeetingUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingUpdates().collectLatest { scheduledMeetReceived ->

                if (scheduledMeetReceived.chatId != state.value.chatId)
                    return@collectLatest

                scheduledMeetReceived.changes?.let { changes ->
                    Timber.d("Monitor scheduled meeting updated, changes $changes")
                    changes.forEach {
                        when (it) {
                            ScheduledMeetingChanges.Title ->
                                _state.update { state ->
                                    state.copy(
                                        scheduledMeetingTitle = scheduledMeetReceived.title ?: ""
                                    )
                                }

                            else -> {}
                        }
                    }
                }
            }
        }

    /**
     * Initialize state values
     *
     * @param chatId    Chat id
     * @param peers     List of peers in the waiting room
     */
    private fun resetValues(chatId: Long, peers: List<Long>?) =
        _state.update { state ->
            state.copy(
                chatId = chatId,
                usersInWaitingRoom = peers ?: emptyList(),
                nameOfTheOnlyUserInTheWaitingRoom = "",
                scheduledMeetingTitle = "",
                showParticipantsInWaitingRoomDialog = false
            )
        }

    /**
     * Check waiting room participants
     *
     * @param users List of users in the waiting room
     * @param chatId Chat id
     */
    private fun checkWaitingRoomParticipants(users: List<Long>, chatId: Long) {
        if (users.isEmpty()) {
            resetValues(chatId, users)
            return
        }

        getScheduledMeetingTitle(chatId, users)
    }

    /**
     * Get scheduled meeting
     *
     * @param chatId Chat id.
     */
    private fun getScheduledMeetingTitle(chatId: Long, users: List<Long>) =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                setShowParticipantsInWaitingRoomDialogConsumed()
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList
                    ?.firstOrNull { !it.isCanceled && it.parentSchedId == -1L }
                    ?.let { schedMeet ->
                        val title = schedMeet.title ?: ""
                        _state.update { state ->
                            state.copy(
                                scheduledMeetingTitle = title,
                                chatId = chatId,
                                usersInWaitingRoom = users,
                            )
                        }

                        if (users.size == 1) {
                            getNameOfUserInWaitingRoom(users.first(), chatId)
                        } else {
                            setShowParticipantsInWaitingRoomDialog()
                        }
                    }
            }
        }

    /**
     * Get user name to show the participant in waiting room dialog
     *
     * @param handle    User handle
     * @param chatId    Chat id
     */
    private fun getNameOfUserInWaitingRoom(handle: Long, chatId: Long) =
        viewModelScope.launch {
            runCatching {
                getMessageSenderNameUseCase(handle, chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                setShowParticipantsInWaitingRoomDialogConsumed()
            }.onSuccess { name ->
                name?.let {
                    _state.update { state ->
                        state.copy(
                            nameOfTheOnlyUserInTheWaitingRoom = it,
                        )
                    }

                    setShowParticipantsInWaitingRoomDialog()
                }
            }
        }

    /**
     * Sets showParticipantsInWaitingRoomDialog as consumed.
     */
    fun setShowParticipantsInWaitingRoomDialogConsumed() = _state.update { state ->
        state.copy(showParticipantsInWaitingRoomDialog = false)
    }

    /**
     * Sets showParticipantsInWaitingRoomDialog.
     */
    private fun setShowParticipantsInWaitingRoomDialog() = _state.update { state ->
        state.copy(showParticipantsInWaitingRoomDialog = true)
    }

    /**
     * Sets showDenyParticipantDialog as consumed.
     */
    fun setShowDenyParticipantDialogConsumed() = _state.update { state ->
        state.copy(showDenyParticipantDialog = false)
    }

    /**
     * Sets showDenyParticipantDialog.
     */
    private fun setShowDenyParticipantDialog() = _state.update { state ->
        state.copy(showDenyParticipantDialog = true)
    }

    /**
     * Sets showSnackbar as consumed.
     */
    fun onConsumeSnackBarMessageEvent() = _state.update { state ->
        state.copy(snackbarString = null)
    }

    /**
     * Admit users to waiting room
     */
    fun admitUsersClick() {
        setShowParticipantsInWaitingRoomDialogConsumed()
        setShowDenyParticipantDialogConsumed()

        if (state.value.usersInWaitingRoom.isEmpty())
            return

        viewModelScope.launch {
            val numberOfUsers = state.value.usersInWaitingRoom.size
            runCatching {
                allowUsersJoinCallUseCase(
                    state.value.chatId,
                    state.value.usersInWaitingRoom,
                    numberOfUsers > 1
                )
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                Timber.d("Users admitted to the call")
                if (state.value.chatIdOfCallOpened == -1L) {
                    _state.update { state ->
                        state.copy(
                            snackbarString = getPluralStringFromStringResMapper(
                                R.plurals.meetings_waiting_room_snackbar_adding_participants_to_call_success,
                                numberOfUsers, numberOfUsers
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Deny users option selected
     */
    fun denyUsersClick() {
        setShowParticipantsInWaitingRoomDialogConsumed()
        setShowDenyParticipantDialog()
    }

    /**
     * Deny user/users to waiting room
     */
    fun denyEntryClick() {
        if (state.value.usersInWaitingRoom.isEmpty())
            return

        viewModelScope.launch {
            runCatching {
                kickUsersFromCallUseCase(state.value.chatId, state.value.usersInWaitingRoom)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                Timber.d("Users kicked off the call")
            }
        }
    }
}