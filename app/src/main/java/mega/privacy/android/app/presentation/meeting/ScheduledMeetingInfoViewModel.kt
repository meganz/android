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
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.InviteParticipantsAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoomChanges
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetVisibleContacts
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * StartConversationFragment view model.
 *
 * @property getVisibleContacts             [GetVisibleContacts]
 * @property monitorConnectivity            [MonitorConnectivity]
 * @property monitorChatRoomUpdates         [MonitorChatRoomUpdates]
 * @property getChatRoom                    [GetChatRoom]
 * @property inviteToChat                   [InviteToChat]
 * @property getPublicChatToPrivate         [SetPublicChatToPrivate]
 * @property state                          Current view state as [ScheduledMeetingInfoState]
 */
@HiltViewModel
class ScheduledMeetingInfoViewModel @Inject constructor(
    private val getVisibleContacts: GetVisibleContacts,
    private val monitorConnectivity: MonitorConnectivity,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val getChatRoom: GetChatRoom,
    private val inviteToChat: InviteToChat,
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
        if (newChatId != MEGACHAT_INVALID_HANDLE) {
            chatId = newChatId
            scheduledMeetingId = newScheduledMeetingId
            getChatRoomAssociated()
            getChatRoomUpdates()
        }
    }

    /**
     * Get chat room
     */
    private fun getChatRoomAssociated() {
        viewModelScope.launch {
            runCatching {
                getChatRoom(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update { it.copy(result = -1L, snackBar = R.string.general_text_error) }
            }.onSuccess { chat ->
                chat?.apply {
                    _state.update {
                        it.copy(
                            chatId = chatId,
                            chatTitle = title,
                            isHost = ownPrivilege == ChatRoomPermission.Moderator,
                            isPublic = isPublic,
                            isOpenInvite = chat.isOpenInvite || ownPrivilege == ChatRoomPermission.Moderator,
                        )
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
                            it.copy(isOpenInvite = chat.isOpenInvite)
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
     * Invite participants to the chat room
     *
     * @param contacts list of contacts
     */
    fun inviteToChat(contacts: ArrayList<String>) {
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
     * Leave group chat.
     */
    fun onLeaveGroupTap() {
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
            Timber.d("Allow non host add participants to the chat room")
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
}