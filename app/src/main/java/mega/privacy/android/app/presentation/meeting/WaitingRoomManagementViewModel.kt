package mega.privacy.android.app.presentation.meeting

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.CallSoundsController
import mega.privacy.android.app.presentation.mapper.GetPluralStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
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
 * @property getStringFromStringResMapper           [GetStringFromStringResMapper]
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
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(WaitingRoomManagementState())
    private val soundsController = CallSoundsController()


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
            }.onSuccess { chatCall ->
                chatCall?.let { call ->
                    call.waitingRoom?.apply {
                        peers?.let { peersInWaitingRoom ->
                            val peersNonHost = peersInWaitingRoom.filterNot {
                                call.moderators?.contains(it) ?: false
                            }

                            setTemporaryUsersList(users = peersNonHost)
                            checkWaitingRoomParticipants(chatId = call.chatId)
                        }
                    }
                }
            }
        }

    /**
     * Set temporary users list
     *
     * @param users Users list
     */
    private fun setTemporaryUsersList(users: List<Long>) = _state.update { state ->
        state.copy(
            temporaryUsersInWaitingRoomList = users
        )
    }

    /**
     * Get chat call updates
     */
    private fun startMonitoringChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdates()
                .collectLatest { call ->
                    call.changes?.apply {
                        if (contains(ChatCallChanges.Status)) {
                            if (call.chatId == state.value.chatId &&
                                (call.status == ChatCallStatus.UserNoPresent ||
                                        call.status == ChatCallStatus.Destroyed)
                            ) {
                                setShowParticipantsInWaitingRoomDialogConsumed()
                                setShowDenyParticipantDialogConsumed()
                            }
                        }
                        if (contains(ChatCallChanges.WaitingRoomUsersEntered)) {
                            call.waitingRoom?.apply {
                                Timber.d("Users entered in waiting room")

                                peers?.let { peersInWaitingRoom ->
                                    val positionUpdateHandler = Handler(Looper.getMainLooper())
                                    val peersNonHost = peersInWaitingRoom.filterNot {
                                        call.moderators?.contains(it) ?: false
                                    }

                                    setTemporaryUsersList(users = peersNonHost)
                                    positionUpdateHandler.postDelayed({
                                        checkWaitingRoomParticipants(
                                            chatId = call.chatId,
                                        )
                                    }, 1000)
                                }
                            }
                        }

                        if (contains(ChatCallChanges.WaitingRoomUsersLeave)) {
                            Timber.d("Users left in waiting room")
                            call.waitingRoom?.apply {
                                peers?.let { peersInWaitingRoom ->
                                    val peersNonHost = peersInWaitingRoom.filterNot {
                                        call.moderators?.contains(it) ?: false
                                    }
                                    setTemporaryUsersList(users = peersNonHost)
                                    checkWaitingRoomParticipants(
                                        chatId = call.chatId,
                                    )

                                }
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
     * Check waiting room participants
     *
     * @param chatId Chat id
     */
    private fun checkWaitingRoomParticipants(chatId: Long) {
        when {
            _state.value.temporaryUsersInWaitingRoomList.isEmpty() -> {
                setShowDenyParticipantDialogConsumed()
                setShowParticipantsInWaitingRoomDialogConsumed()
                _state.update { state ->
                    state.copy(
                        chatId = chatId,
                        usersInWaitingRoom = emptyList(),
                        nameOfTheFirstUserInTheWaitingRoom = "",
                        nameOfTheSecondUserInTheWaitingRoom = "",
                        scheduledMeetingTitle = "",
                    )
                }
            }

            else -> {
                updateUsers(chatId, _state.value.temporaryUsersInWaitingRoomList)
                getScheduledMeetingTitle(chatId)
            }
        }
    }

    /**
     * Update users in waiting room
     *
     * @param chatId    Chat id
     * @param users     List of users
     */
    private fun updateUsers(chatId: Long, users: List<Long>) {
        _state.update { state ->
            state.copy(
                usersInWaitingRoom = users,
            )
        }

        when (users.size) {
            1, 2 -> {
                getNameOfUserInWaitingRoom(users[0], chatId, true, users.size == 1)
                if (users.size == 2) {
                    getNameOfUserInWaitingRoom(
                        users[1], chatId,
                        isFirstUser = false,
                        shouldShowDialog = true
                    )
                }
            }

            else -> {
                setShowParticipantsInWaitingRoomDialog(shouldItSound = true)
            }
        }
    }

    /**
     * Get scheduled meeting
     *
     * @param chatId Chat id.
     */
    private fun getScheduledMeetingTitle(chatId: Long) =
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
                            )
                        }
                    }
            }
        }

    /**
     * Get user name to show the participant in waiting room dialog
     *
     * @param handle                User handle
     * @param chatId                Chat id
     * @param isFirstUser           True, is first user, false, is second user.
     * @param shouldShowDialog      True, show dialog. False, otherwise.
     */
    private fun getNameOfUserInWaitingRoom(
        handle: Long,
        chatId: Long,
        isFirstUser: Boolean,
        shouldShowDialog: Boolean,

        ) =
        viewModelScope.launch {
            runCatching {
                getMessageSenderNameUseCase(handle, chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                setShowParticipantsInWaitingRoomDialogConsumed()
            }.onSuccess { name ->
                name?.let {
                    _state.update { state ->
                        if (isFirstUser) {
                            state.copy(
                                nameOfTheFirstUserInTheWaitingRoom = it,
                            )
                        } else {
                            state.copy(
                                nameOfTheSecondUserInTheWaitingRoom = it,
                            )
                        }
                    }
                    if (shouldShowDialog) {
                        setShowParticipantsInWaitingRoomDialog(shouldItSound = true)
                    }
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
     *
     * @param shouldItSound True, if should sound, false if not.
     */
    private fun setShowParticipantsInWaitingRoomDialog(shouldItSound: Boolean) =
        _state.update { state ->
            if (!state.showParticipantsInWaitingRoomDialog && shouldItSound) {
                soundsController.playSound(CallSoundType.WAITING_ROOM_USERS_ENTERED)
            }
            state.copy(
                showParticipantsInWaitingRoomDialog = true,
                showDenyParticipantDialog = false
            )
        }

    /**
     * Sets showDenyParticipantDialog as consumed.
     */
    private fun setShowDenyParticipantDialogConsumed() = _state.update { state ->
        state.copy(showDenyParticipantDialog = false)
    }

    /**
     * Sets showDenyParticipantDialog.
     */
    private fun setShowDenyParticipantDialog() = _state.update { state ->
        state.copy(showDenyParticipantDialog = true, showParticipantsInWaitingRoomDialog = false)
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
                _state.update { state ->
                    state.copy(
                        snackbarString =
                        when {
                            numberOfUsers == 1 && state.nameOfTheFirstUserInTheWaitingRoom.isNotEmpty() ->
                                getStringFromStringResMapper(
                                    R.string.meeting_call_screen_one_participant_joined_call,
                                    state.nameOfTheFirstUserInTheWaitingRoom
                                )

                            numberOfUsers == 2 && state.nameOfTheFirstUserInTheWaitingRoom.isNotEmpty() && state.nameOfTheSecondUserInTheWaitingRoom.isNotEmpty() -> {
                                getStringFromStringResMapper(
                                    R.string.meeting_call_screen_two_participants_joined_call,
                                    state.nameOfTheFirstUserInTheWaitingRoom,
                                    state.nameOfTheSecondUserInTheWaitingRoom
                                )
                            }

                            else -> {
                                getPluralStringFromStringResMapper(
                                    R.plurals.meeting_call_screen_more_than_two_participants_joined_call,
                                    numberOfUsers,
                                    state.nameOfTheFirstUserInTheWaitingRoom,
                                    (numberOfUsers - 1)
                                )
                            }
                        }
                    )
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
     * See waiting room option selected
     */
    fun seeWaitingRoomClick() {
        setShowParticipantsInWaitingRoomDialogConsumed()
        setShowDenyParticipantDialogConsumed()
    }

    /**
     * Cancel deny entry option selected
     */
    fun cancelDenyEntryClick() {
        setShowDenyParticipantDialogConsumed()
        setShowParticipantsInWaitingRoomDialog(shouldItSound = false)
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
                setShowDenyParticipantDialogConsumed()
            }
        }
    }
}