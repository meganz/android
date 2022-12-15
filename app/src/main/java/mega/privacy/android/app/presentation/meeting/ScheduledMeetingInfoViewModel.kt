package mega.privacy.android.app.presentation.meeting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_PUSH_NOTIFICATION_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME
import mega.privacy.android.app.constants.BroadcastConstants.RETENTION_TIME
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatRoomChanges
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.chat.ScheduledMeetingItem
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.GetVisibleContacts
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.MonitorChatListItemUpdates
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveChatLink
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
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
 * @property monitorChatListItemUpdates     [MonitorChatListItemUpdates]
 * @property monitorScheduledMeetingUpdates [MonitorScheduledMeetingUpdates]
 * @property monitorConnectivity            [MonitorConnectivity]
 * @property monitorChatRoomUpdates         [MonitorChatRoomUpdates]
 * @property queryChatLink                  [QueryChatLink]
 * @property removeChatLink                 [RemoveChatLink]
 * @property createChatLink                 [CreateChatLink]
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
    private val getChatParticipants: GetChatParticipants,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val monitorConnectivity: MonitorConnectivity,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val monitorChatListItemUpdates: MonitorChatListItemUpdates,
    private val queryChatLink: QueryChatLink,
    private val removeChatLink: RemoveChatLink,
    private val createChatLink: CreateChatLink,
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
     * Observe changes in Chat notifications
     */
    val chatNotificationsObserver = Observer<Any> {
        updateDndSeconds(chatId)
    }

    init {
        LiveEventBus.get(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)
            .observeForever(chatNotificationsObserver)
    }

    /**
     * onCleared()
     */
    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)
            .removeObserver(chatNotificationsObserver)
    }

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
                Timber.e("Chat room does not exist, finish $exception")
                finishActivity()
            }.onSuccess { chat ->
                Timber.d("Chat room exists")
                chat?.apply {
                    if (isActive) {
                        Timber.d("Chat room is active")
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

                        loadAllChatParticipants()

                        getScheduledMeeting()
                        updateDndSeconds(chatId)
                        updateRetentionTimeSeconds(retentionTime)
                        queryChatLink()

                        getChatRoomUpdates()
                        getScheduledMeetingUpdates()
                        getChatListItemUpdates()
                    } else {
                        Timber.d("Chat room is not active, finish")
                        finishActivity()
                    }
                }
            }
        }
    }

    /**
     * Load all chat participants
     */
    private fun loadAllChatParticipants() = viewModelScope.launch {
        runCatching {
            getChatParticipants(state.value.chatId)
                .catch { exception ->
                    Timber.e(exception)
                }
                .collectLatest { list ->
                    _state.update {
                        it.copy(participantItemList = list)
                    }
                    updateFirstAndLastParticipants()
                }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Update first and last participants
     */
    private fun updateFirstAndLastParticipants() {
        _state.value.participantItemList.let { list ->
            if (list.isEmpty()) {
                _state.update {
                    it.copy(firstParticipant = null,
                        lastParticipant = null)
                }
            } else if (list.size == 1) {
                _state.update {
                    it.copy(firstParticipant = list.first(),
                        lastParticipant = null)
                }
            } else {

                _state.update {
                    it.copy(firstParticipant = list.first(),
                        lastParticipant = list.last())
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
                Timber.e("Scheduled meeting does not exist, finish $exception")
                finishActivity()
            }.onSuccess { scheduledMeetingList ->
                Timber.d("Scheduled meeting exists")
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (scheduledMeetReceived.parentSchedId == MEGACHAT_INVALID_HANDLE) {
                            _state.update {
                                it.copy(scheduledMeeting = ScheduledMeetingItem(
                                    chatId = scheduledMeetReceived.chatId,
                                    scheduledMeetingId = scheduledMeetReceived.schedId,
                                    title = scheduledMeetReceived.title,
                                    description = scheduledMeetReceived.description,
                                    date = scheduledMeetReceived.getFormattedDate())
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
     * Remove open add contact screen
     */
    fun removeAddContact() {
        _state.update {
            it.copy(openAddContact = null)
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
                    ChatRoomChanges.RetentionTime -> {
                        updateRetentionTimeSeconds(chat.retentionTime)

                        val intentRetentionTime =
                            Intent(ACTION_UPDATE_RETENTION_TIME)
                        intentRetentionTime.putExtra(RETENTION_TIME,
                            chat.retentionTime)
                        getInstance().sendBroadcast(intentRetentionTime)
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
            monitorScheduledMeetingUpdates().collectLatest { scheduledMeetReceived ->
                when (scheduledMeetReceived.changes) {
                    ScheduledMeetingChanges.NewScheduledMeeting -> {
                        if (scheduledMeetReceived.parentSchedId == MEGACHAT_INVALID_HANDLE) {
                            _state.update {
                                it.copy(scheduledMeeting = ScheduledMeetingItem(
                                    scheduledMeetReceived.chatId,
                                    scheduledMeetReceived.schedId,
                                    scheduledMeetReceived.title,
                                    scheduledMeetReceived.description,
                                    scheduledMeetReceived.getFormattedDate())
                                )
                            }
                        }
                    }
                    ScheduledMeetingChanges.Title -> {
                        _state.value.scheduledMeeting?.let {
                            if (scheduledMeetReceived.schedId == it.scheduledMeetingId) {
                                _state.update { state ->
                                    state.copy(scheduledMeeting = state.scheduledMeeting?.copy(title = scheduledMeetReceived.title))
                                }
                            }
                        }
                    }
                    ScheduledMeetingChanges.Description -> {
                        _state.value.scheduledMeeting?.let {
                            if (scheduledMeetReceived.schedId == it.scheduledMeetingId) {
                                _state.update { state ->
                                    state.copy(scheduledMeeting = state.scheduledMeeting?.copy(
                                        description = scheduledMeetReceived.description))
                                }
                            }
                        }
                    }
                    ScheduledMeetingChanges.StartDate -> {
                        _state.value.scheduledMeeting?.let {
                            if (scheduledMeetReceived.schedId == it.scheduledMeetingId) {
                                _state.update { state ->
                                    state.copy(scheduledMeeting = state.scheduledMeeting?.copy(
                                        date = scheduledMeetReceived.getFormattedDate()
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
     * Get chat list item updates
     */
    private fun getChatListItemUpdates() {
        viewModelScope.launch {
            monitorChatListItemUpdates().collectLatest { item ->
                when (item.changes) {
                    ChatListItemChanges.LastMessage -> {
                        if (item.lastMessageType == ChatRoomLastMessage.PublicHandleCreate ||
                            item.lastMessageType == ChatRoomLastMessage.PublicHandleDelete
                        ) {
                            queryChatLink()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Update seconds of Do not disturb mode
     *
     * @param id    Chat id.
     */
    private fun updateDndSeconds(id: Long) {
        getPushNotificationSettingManagement().pushNotificationSetting?.let { push ->
            if (push.isChatDndEnabled(id)) {
                _state.update {
                    it.copy(dndSeconds = push.getChatDnd(id))
                }

                return
            }
        }

        _state.update {
            it.copy(dndSeconds = null)
        }
    }

    /**
     * Update retention time seconds
     *
     * @param retentionTime    Retention time seconds
     */
    private fun updateRetentionTimeSeconds(retentionTime: Long) {
        if (retentionTime == Constants.DISABLED_RETENTION_TIME) {
            _state.update {
                it.copy(retentionTimeSeconds = null)
            }
        } else {
            _state.update {
                it.copy(retentionTimeSeconds = retentionTime)
            }
        }
    }

    /**
     * Check if there is an existing chat-link for an public chat
     */
    private fun queryChatLink() {
        viewModelScope.launch {
            runCatching {
                queryChatLink(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { request ->
                _state.update {
                    it.copy(enabledMeetingLinkOption = request.text != null,
                        meetingLink = request.text)
                }
            }
        }
    }

    /**
     * Remove chat link
     */
    private fun removeChatLink() {
        viewModelScope.launch {
            runCatching {
                removeChatLink(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                showSnackBar(R.string.general_text_error)
            }.onSuccess { _ ->
                _state.update { it.copy(enabledMeetingLinkOption = false, meetingLink = null) }
            }
        }
    }

    /**
     * Create chat link
     */
    private fun createChatLink() {
        viewModelScope.launch {
            runCatching {
                createChatLink(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                showSnackBar(R.string.general_text_error)
            }.onSuccess { request ->
                _state.update {
                    it.copy(enabledMeetingLinkOption = true,
                        meetingLink = request.text)
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
        showSnackBar(R.string.invite_sent)
    }

    /**
     * Edit scheduled meeting if there is internet connection, shows an error if not.
     */
    fun onEditTap() {
        Timber.d("Edit scheduled meeting")
    }

    /**
     * See more or less participants in the list.
     */
    fun onSeeMoreOrLessTap() {
        _state.update { state ->
            state.copy(seeMoreVisible = !state.seeMoreVisible)
        }

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
     * Dismiss alert dialogs
     */
    fun dismissDialog() {
        _state.update { state ->
            state.copy(leaveGroupDialog = false,
                addParticipantsNoContactsDialog = false,
                addParticipantsNoContactsLeftToAddDialog = false)
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
                showSnackBar(R.string.general_error)
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
                            it.copy(addParticipantsNoContactsDialog = true, openAddContact = false)
                        }
                    }
                    ChatUtil.areAllMyContactsChatParticipants(chatId) -> {
                        _state.update {
                            it.copy(addParticipantsNoContactsLeftToAddDialog = true,
                                openAddContact = false)
                        }
                    }
                    else -> {
                        _state.update {
                            it.copy(openAddContact = true)
                        }
                    }
                }
            }
        } else {
            showSnackBar(R.string.check_internet_connection_error)
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
            Timber.d("Meeting link option")
            if (_state.value.enabledMeetingLinkOption) {
                removeChatLink()
            } else {
                createChatLink()
            }
        } else {
            showSnackBar(R.string.check_internet_connection_error)
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
                    showSnackBar(R.string.general_text_error)
                }.onSuccess { result ->
                    _state.update {
                        it.copy(
                            isOpenInvite = result || it.isHost,
                            enabledAllowNonHostAddParticipantsOption = result)
                    }
                }
            }
        } else {
            showSnackBar(R.string.check_internet_connection_error)
        }
    }

    /**
     * Enable encrypted key rotation if there is internet connection, shows an error if not.
     */
    fun enableEncryptedKeyRotation() {
        if (_state.value.participantItemList.size > MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE) {
            showSnackBar(R.string.warning_make_chat_private)
        } else {
            viewModelScope.launch {
                runCatching {
                    getPublicChatToPrivate(chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    showSnackBar(R.string.general_error)
                }.onSuccess { _ ->
                    _state.update { it.copy(isPublic = false) }
                }
            }
        }
    }

    /**
     * Shares the link to chat
     *
     * @param data       Intent containing the info to share the content to chats.
     * @param action     Action to perform.
     */
    fun sendToChat(
        data: Intent?,
        action: (Intent?) -> Unit,
    ) {
        data?.putExtra(Constants.EXTRA_LINK, _state.value.meetingLink)
        action.invoke(data)
    }

    /**
     * Copy meeting link to clipboard
     *
     * @param clipboard [ClipboardManager]
     */
    fun copyMeetingLink(clipboard: ClipboardManager) {
        _state.value.meetingLink?.let { meetingLink ->
            val clip = ClipData.newPlainText(Constants.COPIED_TEXT_LABEL, meetingLink)
            clipboard.setPrimaryClip(clip)
            showSnackBar(R.string.scheduled_meetings_meeting_link_copied)
        }
    }

    /**
     * Show snackBar with a text
     *
     * @param stringId String id.
     */
    private fun showSnackBar(stringId: Int) {
        _state.update { it.copy(snackBar = stringId) }
    }

    /**
     * Format ZonedDateTime to a readable date
     *
     * @return  String with the formatted date
     */
    private fun ChatScheduledMeeting.getFormattedDate(): String =
        DateTimeFormatter.ofPattern("d MMM yyyy '·' HH:mm").format(startDateTime) +
                " - ${DateTimeFormatter.ofPattern("HH:mm").format(endDateTime)}"


    /**
     * Updates state after shown snackBar.
     */
    fun snackbarShown() = _state.update { it.copy(snackBar = null) }

    /**
     * Open send to screen
     */
    fun openSendToChat(shouldOpen: Boolean) {
        _state.update { it.copy(openSendToChat = shouldOpen) }
    }

    companion object {
        private const val MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE = 100
    }
}
