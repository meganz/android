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
import mega.privacy.android.app.presentation.mapper.GetPluralStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.domain.entity.chat.ChatParticipant
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
    fun setChatIdCallOpened(chatId: Long) {
        if (state.value.chatIdOfCallOpened != chatId) {
            _state.update { state ->
                state.copy(
                    chatIdOfCallOpened = chatId
                )
            }
        }
    }

    /**
     * Set dialog closed
     */
    fun setDialogClosed() = _state.update { state ->
        state.copy(
            isDialogClosed = true,
        )
    }

    /**
     * Get call
     */
    private fun getCall() = viewModelScope.launch {
        runCatching {
            getChatCallInProgress()
        }.onFailure { exception ->
            Timber.e(exception)
        }.onSuccess { chatCall ->
            chatCall?.let { call ->
                call.waitingRoom?.apply {
                    _state.update { state ->
                        state.copy(
                            chatId = call.chatId,
                        )
                    }

                    if (!state.value.isDialogClosed) {
                        peers?.let { peersInWaitingRoom ->
                            val peersNonHost = peersInWaitingRoom.filterNot {
                                call.moderators?.contains(it) ?: false
                            }

                            setTemporaryUsersList(users = peersNonHost)
                            checkWaitingRoomParticipants(
                                chatId = call.chatId,
                                shouldDialogBeShown = true
                            )
                        }
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
    private fun startMonitoringChatCallUpdates() = viewModelScope.launch {
        monitorChatCallUpdates().collectLatest { call ->
            call.changes?.apply {
                if (contains(ChatCallChanges.Status)) {
                    if (call.chatId == state.value.chatId && (call.status == ChatCallStatus.UserNoPresent || call.status == ChatCallStatus.Destroyed)) {
                        setShowParticipantsInWaitingRoomDialogConsumed()
                        setShowDenyParticipantDialogConsumed()
                    }
                }

                if (contains(ChatCallChanges.WaitingRoomUsersEntered) || contains(ChatCallChanges.WaitingRoomUsersLeave)) {
                    call.waitingRoom?.apply {
                        Timber.d("Users entered or left waiting room")
                        peers?.let { peersInWaitingRoom ->
                            val peersNonHost = peersInWaitingRoom.filterNot {
                                call.moderators?.contains(it) ?: false
                            }
                            setTemporaryUsersList(users = peersNonHost)
                            if (contains(ChatCallChanges.WaitingRoomUsersEntered)) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    checkWaitingRoomParticipants(
                                        chatId = call.chatId,
                                        shouldDialogBeShown = true

                                    )
                                }, 1000)
                            }
                            if (contains(ChatCallChanges.WaitingRoomUsersLeave)) {
                                checkWaitingRoomParticipants(
                                    chatId = call.chatId,
                                    shouldDialogBeShown = state.value.showParticipantsInWaitingRoomDialog
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Get scheduled meeting updates
     */
    private fun startMonitoringScheduledMeetingUpdates() = viewModelScope.launch {
        monitorScheduledMeetingUpdates().collectLatest { scheduledMeetReceived ->
            if (scheduledMeetReceived.chatId != state.value.chatId) return@collectLatest

            scheduledMeetReceived.changes?.let { changes ->
                Timber.d("Monitor scheduled meeting updated, changes $changes")
                changes.forEach {
                    when (it) {
                        ScheduledMeetingChanges.Title -> _state.update { state ->
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
     * @param shouldDialogBeShown   True, dialog should be shown. False, otherwise.
     */
    private fun checkWaitingRoomParticipants(chatId: Long, shouldDialogBeShown: Boolean) {
        when {
            _state.value.temporaryUsersInWaitingRoomList.isEmpty() -> {
                setShowDenyParticipantDialogConsumed()
                setShowParticipantsInWaitingRoomDialogConsumed()
                _state.update { state ->
                    state.copy(
                        chatId = chatId,
                        usersInWaitingRoomIDs = emptyList(),
                        nameOfTheFirstUserInTheWaitingRoom = "",
                        nameOfTheSecondUserInTheWaitingRoom = "",
                        scheduledMeetingTitle = "",
                    )
                }
            }

            else -> {
                val users = _state.value.temporaryUsersInWaitingRoomList
                _state.update { state ->
                    state.copy(
                        usersInWaitingRoomIDs = users,
                    )
                }

                val checkUserToDeny =
                    users.find { it == state.value.participantToDenyEntry?.handle }
                if (checkUserToDeny == null) {
                    setShowDenyParticipantDialogConsumed()
                }

                when (users.size) {
                    1, 2 -> {
                        getNameOfUserInWaitingRoom(
                            users[0],
                            chatId,
                            true,
                            shouldDialogBeShown && users.size == 1
                        )
                        if (users.size == 2) {
                            getNameOfUserInWaitingRoom(
                                users[1],
                                chatId,
                                isFirstUser = false,
                                shouldShowDialog = shouldDialogBeShown
                            )
                        }
                    }

                    else -> setShowParticipantsInWaitingRoomDialog(shouldDialogBeShown)
                }
                getScheduledMeetingTitle(chatId)
            }
        }
    }

    /**
     * Get scheduled meeting
     *
     * @param chatId Chat id.
     */
    private fun getScheduledMeetingTitle(chatId: Long) = viewModelScope.launch {
        runCatching {
            getScheduledMeetingByChat(chatId)
        }.onFailure { exception ->
            Timber.e(exception)
            setShowParticipantsInWaitingRoomDialogConsumed()
        }.onSuccess { scheduledMeetingList ->
            scheduledMeetingList?.firstOrNull { !it.isCanceled && it.parentSchedId == -1L }
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
    ) = viewModelScope.launch {
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
                    setShowParticipantsInWaitingRoomDialog()
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
     */
    private fun setShowParticipantsInWaitingRoomDialog(needToUpdateDialog: Boolean = true) {
        if (needToUpdateDialog && !state.value.isWaitingRoomSectionOpened) {
            _state.update { state ->
                state.copy(
                    showParticipantsInWaitingRoomDialog = true,
                    showDenyParticipantDialog = false
                )
            }
        } else {
            setShowParticipantsInWaitingRoomDialogConsumed()
        }
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
     * Sets usersAdmitted as consumed.
     */
    fun onConsumeUsersAdmittedEvent() = _state.update { state ->
        state.copy(usersAdmitted = false)
    }

    /**
     * Sets shouldWaitingRoomBeShown as consumed.
     */
    fun onConsumeShouldWaitingRoomBeShownEvent() = _state.update { state ->
        state.copy(shouldWaitingRoomBeShown = false)
    }

    /**
     * Sets if waiting room section is opened
     */
    fun setWaitingRoomSectionOpened(isOpened: Boolean) = _state.update { state ->
        state.copy(isWaitingRoomSectionOpened = isOpened)
    }

    /**
     * Admit users to waiting room
     *
     * @param chatParticipant   [ChatParticipant]
     */
    fun admitUsersClick(chatParticipant: ChatParticipant? = null) {
        setShowParticipantsInWaitingRoomDialogConsumed()
        setShowDenyParticipantDialogConsumed()

        if (state.value.usersInWaitingRoomIDs.isEmpty()) return

        viewModelScope.launch {
            val list = if (chatParticipant == null) state.value.usersInWaitingRoomIDs else listOf(
                chatParticipant.handle
            )
            val numberOfUsers = list.size
            runCatching {
                allowUsersJoinCallUseCase(
                    state.value.chatId, list, numberOfUsers > 1
                )
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                Timber.d("Users admitted to the call chatIdOfCallOpened ${state.value.chatIdOfCallOpened}")
                if (state.value.chatIdOfCallOpened == -1L) {
                    _state.update { state ->
                        state.copy(
                            snackbarString = when {
                                numberOfUsers == 1 && state.nameOfTheFirstUserInTheWaitingRoom.isNotEmpty() -> getStringFromStringResMapper(
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
                } else {
                    _state.update { state ->
                        state.copy(usersAdmitted = true)
                    }
                }
            }
        }
    }

    /**
     * Deny users option selected
     *
     * @param chatParticipant   [ChatParticipant]
     */
    fun denyUsersClick(chatParticipant: ChatParticipant? = null) {
        _state.update { state ->
            state.copy(participantToDenyEntry = chatParticipant)
        }
        setShowParticipantsInWaitingRoomDialogConsumed()
        setShowDenyParticipantDialog()
    }

    /**
     * See waiting room option selected
     */
    fun seeWaitingRoomClick() {
        setShowParticipantsInWaitingRoomDialogConsumed()
        setShowDenyParticipantDialogConsumed()
        _state.update { state ->
            state.copy(shouldWaitingRoomBeShown = true)
        }
        setShowParticipantsInWaitingRoomDialogConsumed()
    }

    /**
     * Cancel deny entry option selected
     */
    fun cancelDenyEntryClick() {
        setShowDenyParticipantDialogConsumed()
        setShowParticipantsInWaitingRoomDialog()
    }

    /**
     * Deny user/users to waiting room
     */
    fun denyEntryClick() {
        if (state.value.usersInWaitingRoomIDs.isEmpty() && state.value.participantToDenyEntry == null) return
        var list = state.value.usersInWaitingRoomIDs
        state.value.participantToDenyEntry?.let {
            list = listOf(it.handle)
        }
        viewModelScope.launch {
            runCatching {
                kickUsersFromCallUseCase(state.value.chatId, list)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                Timber.d("Users kicked off the call")
                setShowDenyParticipantDialogConsumed()
            }
        }
    }
}