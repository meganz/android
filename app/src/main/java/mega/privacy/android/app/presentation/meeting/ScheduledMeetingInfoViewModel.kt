package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.InviteParticipantsAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoomChanges
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.chat.ScheduledMeetingItem
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.GetVisibleContacts
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
import mega.privacy.android.domain.usecase.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.SetOpenInvite
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * StartConversationFragment view model.
 *
 * @property getVisibleContacts             [GetVisibleContacts]
 * @property getChatRoom                    [GetChatRoom]
 * @property getScheduledMeetingByChat      [GetScheduledMeetingByChat]
 * @property monitorScheduledMeetingUpdates [MonitorScheduledMeetingUpdates]
 * @property monitorConnectivity            [MonitorConnectivity]
 * @property monitorChatRoomUpdates         [MonitorChatRoomUpdates]
 * @property inviteToChat                   [InviteToChat]
 * @property leaveChat                      [LeaveChat]
 * @property setOpenInvite                  [SetOpenInvite]
 * @property getPublicChatToPrivate         [SetPublicChatToPrivate]
 * @property state                          Current view state as [ScheduledMeetingInfoState]
 */
@HiltViewModel
class ScheduledMeetingInfoViewModel @Inject constructor(
    private val getVisibleContacts: GetVisibleContacts,
    private val getChatRoom: GetChatRoom,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val monitorConnectivity: MonitorConnectivity,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val inviteToChat: InviteToChat,
    private val leaveChat: LeaveChat,
    private val setOpenInvite: SetOpenInvite,
    private val getPublicChatToPrivate: SetPublicChatToPrivate,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduledMeetingInfoState())
    val state: StateFlow<ScheduledMeetingInfoState> = _state

    private var chatId: Long = MEGACHAT_INVALID_HANDLE
    private var scheduledMeetingId: Long = MEGACHAT_INVALID_HANDLE

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Sets chat id and scheduled meeting id
     *
     * @param newChatId                 Chat id.
     * @param newScheduledMeetingId     Scheduled meeting id.
     */
    fun setChatId(newChatId: Long, newScheduledMeetingId: Long) {
        if (newChatId != MEGACHAT_INVALID_HANDLE && newChatId != chatId) {
            chatId = newChatId
            scheduledMeetingId = newScheduledMeetingId
            getChatRoom()
            getScheduledMeeting()
            getChatRoomUpdates()
            getScheduledMeetingUpdates()
        }
    }

    /**
     * Get chat room
     */
    private fun getChatRoom() {
        viewModelScope.launch {
            runCatching {
                getChatRoom(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update { it.copy(snackBar = R.string.general_text_error) }
            }.onSuccess { chat ->
                Timber.d("Chat room obtained")
                chat?.apply {
                    _state.update {
                        it.copy(
                            chatId = chatId,
                            chatTitle = title,
                            isHost = ownPrivilege == ChatRoomPermission.Moderator,
                            isOpenInvite = isOpenInvite || ownPrivilege == ChatRoomPermission.Moderator,
                            enabledAllowNonHostAddParticipantsOption = isOpenInvite,
                            isPublic = isPublic
                        )
                    }
                }
            }
        }
    }

    /**
     * Get scheduled meeting
     */
    private fun getScheduledMeeting() {
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update { it.copy(snackBar = R.string.general_text_error) }
            }.onSuccess { scheduledMeetingList ->
                Timber.d("Scheduled meeting obtained")
                scheduledMeetingList?.let { list ->
                    list.forEach { schedMeeting ->
                        if (schedMeeting.parentSchedId == MEGACHAT_INVALID_HANDLE) {
                            _state.update {
                                it.copy(scheduledMeeting = ScheduledMeetingItem(
                                    chatId = schedMeeting.chatId,
                                    scheduledMeetingId = schedMeeting.schedId,
                                    title = schedMeeting.title,
                                    description = schedMeeting.description,
                                    date = schedMeeting.getFormattedDate())
                                )
                            }
                            return@forEach
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove invite participants action
     */
    fun removeInviteParticipantsAction() {
        _state.update {
            it.copy(inviteParticipantAction = null)
        }
    }

    /**
     * Get chat room updates
     */
    private fun getChatRoomUpdates() {
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId).collectLatest { chat ->
                when (chat.changes) {
                    ChatRoomChanges.OwnPrivilege ->
                        _state.update {
                            it.copy(isHost = chat.ownPrivilege == ChatRoomPermission.Moderator)
                        }

                    ChatRoomChanges.OpenInvite ->
                        _state.update {
                            it.copy(
                                isOpenInvite = chat.isOpenInvite || chat.ownPrivilege == ChatRoomPermission.Moderator,
                                enabledAllowNonHostAddParticipantsOption = chat.isOpenInvite)
                        }

                    ChatRoomChanges.Title ->
                        _state.update {
                            it.copy(chatTitle = chat.title)
                        }

                    ChatRoomChanges.ChatMode ->
                        _state.update {
                            it.copy(isPublic = chat.isPublic)
                        }
                    else -> {}
                }
            }
        }
    }

    /**
     * Get scheduled meeting updates
     */
    private fun getScheduledMeetingUpdates() {
        viewModelScope.launch {
            monitorScheduledMeetingUpdates().collectLatest { schedMeetReceived ->
                when (schedMeetReceived.changes) {
                    ScheduledMeetingChanges.NewScheduledMeeting -> {
                        if (schedMeetReceived.parentSchedId == MEGACHAT_INVALID_HANDLE) {
                            _state.update {
                                it.copy(scheduledMeeting = ScheduledMeetingItem(
                                    schedMeetReceived.chatId,
                                    schedMeetReceived.schedId,
                                    schedMeetReceived.title,
                                    schedMeetReceived.description,
                                    schedMeetReceived.getFormattedDate())
                                )
                            }
                        }
                    }
                    ScheduledMeetingChanges.Title -> {
                        _state.value.scheduledMeeting?.let {
                            if (schedMeetReceived.schedId == it.scheduledMeetingId) {
                                _state.update { state ->
                                    state.copy(scheduledMeeting = state.scheduledMeeting?.copy(title = schedMeetReceived.title))
                                }
                            }
                        }
                    }
                    ScheduledMeetingChanges.Description -> {
                        _state.value.scheduledMeeting?.let {
                            if (schedMeetReceived.schedId == it.scheduledMeetingId) {
                                _state.update { state ->
                                    state.copy(scheduledMeeting = state.scheduledMeeting?.copy(
                                        description = schedMeetReceived.description))
                                }
                            }
                        }
                    }
                    ScheduledMeetingChanges.StartDate -> {
                        _state.value.scheduledMeeting?.let {
                            if (schedMeetReceived.schedId == it.scheduledMeetingId) {
                                _state.update { state ->
                                    state.copy(scheduledMeeting = state.scheduledMeeting?.copy(
                                        date = schedMeetReceived.getFormattedDate()
                                    ))
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Invite participants to the chat room
     *
     * @param contacts list of contacts
     */
    fun inviteToChat(contacts: ArrayList<String>) {
        Timber.d("Invite participants")
        viewModelScope.launch {
            inviteToChat(_state.value.chatId, contacts)
        }

        _state.update { it.copy(snackBar = R.string.invite_sent) }
    }

    /**
     * Edit scheduled meeting if there is internet connection, shows an error if not.
     */
    fun onEditTap() {
        if (isConnected) {
            Timber.d("Edit scheduled meeting")
        } else {
            showError()
        }
    }

    /**
     * See more participants in the list.
     */
    fun onSeeMoreTap() {
    }

    /**
     * Leave group chat button clicked
     */
    fun onLeaveGroupTap() {
        _state.update { state ->
            state.copy(leaveGroupDialog = !state.leaveGroupDialog)
        }
    }

    /**
     * Dismiss alert dialog
     */
    fun dismissDialog() {
        _state.update { state ->
            state.copy(leaveGroupDialog = false)
        }
    }

    /**
     * Finish activity
     */
    fun finishActivity() {
        _state.update { state ->
            state.copy(finish = true)
        }
    }

    /**
     * Leave chat
     */
    fun leaveChat() {
        viewModelScope.launch {
            runCatching {
                leaveChat(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                dismissDialog()
                _state.update { it.copy(snackBar = R.string.general_error) }
            }.onSuccess { result ->
                Timber.d("Chat left ")
                if (result.userHandle == MegaApiJava.INVALID_HANDLE) {
                    result.chatHandle?.let { chatHandle ->
                        MegaApplication.getChatManagement().removeLeavingChatId(chatHandle)
                    }
                }

                dismissDialog()
                finishActivity()
            }
        }
    }

    /**
     * Add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onInviteParticipantsTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
            viewModelScope.launch {
                val contactList = getVisibleContacts()
                when {
                    contactList.isEmpty() -> {
                        _state.update {
                            it.copy(inviteParticipantAction = InviteParticipantsAction.NO_CONTACTS_DIALOG)
                        }
                    }
                    ChatUtil.areAllMyContactsChatParticipants(chatId) -> {
                        _state.update {
                            it.copy(inviteParticipantAction = InviteParticipantsAction.NO_MORE_CONTACTS_DIALOG)
                        }
                    }
                    else -> {
                        _state.update {
                            it.copy(inviteParticipantAction = InviteParticipantsAction.ADD_CONTACTS)
                        }
                    }
                }
            }
        } else {
            showError()
        }
    }

    /**
     * Open bottom panel option of a participant.
     */
    fun onParticipantTap(contactItem: ContactItem) {
        Timber.d("Participant ${contactItem.handle} clicked")
    }

    /**
     * Create or removed meeting link if there is internet connection, shows an error if not.
     */
    fun onMeetingLinkTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
        } else {
            showError()
        }
    }

    /**
     * Share meeting link if there is internet connection, shows an error if not.
     */
    fun onShareMeetingLinkTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
        } else {
            showError()
        }
    }

    /**
     * Enable or disable chat notifications if there is internet connection, shows an error if not.
     */
    fun onChatNotificationsTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
        } else {
            showError()
        }
    }

    /**
     * Enable or disable the option Allow non-host add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onAllowAddParticipantsTap() {
        if (isConnected) {
            Timber.d("Update option Allow non-host add participants to the chat room")
            viewModelScope.launch {
                runCatching {
                    setOpenInvite(chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { it.copy(snackBar = R.string.general_text_error) }
                }.onSuccess { result ->
                    _state.update {
                        it.copy(
                            isOpenInvite = result || it.isHost,
                            enabledAllowNonHostAddParticipantsOption = result)
                    }
                }
            }
        } else {
            showError()
        }
    }

    /**
     * Manage chat history if there is internet connection, shows an error if not.
     */
    fun onManageChatHistoryTap() {
        if (isConnected) {
            Timber.d("Manage chat history")
        } else {
            showError()
        }
    }

    /**
     * Enable encrypted key rotation if there is internet connection, shows an error if not.
     */
    fun enableEncryptedKeyRotation() {
        if (_state.value.participantItemList.size > MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE) {
            _state.update { it.copy(snackBar = R.string.warning_make_chat_private) }
        } else {
            viewModelScope.launch {
                runCatching {
                    getPublicChatToPrivate(chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { it.copy(snackBar = R.string.general_error) }
                }.onSuccess { _ ->
                    _state.update { it.copy(isPublic = false) }
                }
            }
        }
    }

    /**
     * Add error when there is no internet connection
     */
    private fun showError() {
        _state.update { it.copy(snackBar = R.string.check_internet_connection_error) }
    }

    companion object {
        private const val MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE = 100
    }


    /**
     * Format ZonedDateTime to a readable date
     *
     * @return  String with the formatted date
     */
    private fun ChatScheduledMeeting.getFormattedDate(): String =
        DateTimeFormatter.ofPattern("d MMM yyyy '·' HH:mm").format(startDateTime) +
                " - ${DateTimeFormatter.ofPattern("HH:mm").format(endDateTime)}"

}
