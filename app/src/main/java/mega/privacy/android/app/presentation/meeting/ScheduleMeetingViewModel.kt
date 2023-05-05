package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingState
import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.meeting.CreateChatroomAndSchedMeetingUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ScheduleMeetingActivity view model.
 * @property monitorConnectivityUseCase                 [MonitorConnectivityUseCase]
 * @property getVisibleContactsUseCase                  [GetVisibleContactsUseCase]
 * @property getContactFromEmailUseCase                 [GetContactFromEmailUseCase]
 * @property createChatroomAndSchedMeetingUseCase       [CreateChatroomAndSchedMeetingUseCase]
 * @property createChatLink                             [CreateChatLink]
 * @property state                                      Current view state as [ScheduleMeetingState]
 */
@HiltViewModel
class ScheduleMeetingViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val createChatroomAndSchedMeetingUseCase: CreateChatroomAndSchedMeetingUseCase,
    private val createChatLink: CreateChatLink,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleMeetingState())
    val state: StateFlow<ScheduleMeetingState> = _state

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = monitorConnectivityUseCase().value

    /**
     * Enable or disable meeting link option
     */
    fun onMeetingLinkTap() =
        _state.update { state ->
            state.copy(enabledMeetingLinkOption = !state.enabledMeetingLinkOption)
        }

    /**
     * Enable or disable send calendar invite option
     */
    fun onSendCalendarInviteTap() =
        _state.update { state ->
            state.copy(enabledSendCalendarInviteOption = !state.enabledSendCalendarInviteOption)
        }

    /**
     * Add participants to the schedule meeting.
     */
    fun onAddParticipantsTap() {
        Timber.d("Add participants to the schedule meeting")
        viewModelScope.launch {
            val contactList = getVisibleContactsUseCase()
            when {
                contactList.isEmpty() -> {
                    _state.update {
                        it.copy(addParticipantsNoContactsDialog = true, openAddContact = false)
                    }
                }
                else -> {
                    _state.update {
                        it.copy(openAddContact = true)
                    }
                }
            }
        }
    }

    /**
     * Add selected contacts as participants
     *
     * @param contacts list of contacts selected
     */
    fun addContactsSelected(contacts: ArrayList<String>) {
        _state.update {
            it.copy(
                numOfParticipants = contacts.size + 1,
                allowAddParticipants = contacts.isEmpty()
            )
        }
        viewModelScope.launch {
            val list = mutableListOf<ContactItem>()
            contacts.forEach { email ->
                runCatching {
                    getContactFromEmailUseCase(email, isOnline())
                }.onSuccess { contactItem ->
                    contactItem?.let {
                        list.add(it)
                    }
                }
            }

            _state.update {
                it.copy(
                    participantItemList = list,
                    allowAddParticipants = true,
                    snackBar = if (list.isEmpty()) null else R.string.number_of_participants
                )
            }
        }
    }

    /**
     * Set start date
     *
     * @param newDate   Start Date
     */
    fun setStartDate(newDate: Instant) {
        if (newDate.isBefore(Instant.now())) return

        _state.update { state ->
            if (state.endDate.isAfter(newDate)) {
                state.copy(
                    startDate = newDate,
                )
            } else {
                state.copy(
                    startDate = newDate,
                    endDate = newDate.plus(1, ChronoUnit.HOURS)
                )
            }
        }
    }

    /**
     * Set end date
     *
     * @param newDate   End Date
     */
    fun setEndDate(newDate: Instant) {
        if (newDate.isBefore(state.value.startDate)) return

        _state.update {
            it.copy(
                endDate = newDate
            )
        }
    }

    /**
     * Remove open add contact screen
     */
    fun removeAddContact() =
        _state.update {
            it.copy(openAddContact = null)
        }


    /**
     * Enable or disable allow non-hosts to add participants option
     */
    fun onAllowNonHostAddParticipantsTap() =
        _state.update { state ->
            state.copy(enabledAllowAddParticipantsOption = !state.enabledAllowAddParticipantsOption)
        }

    /**
     * Start adding description
     */
    fun onAddDescriptionTap() =
        _state.update { state ->
            state.copy(isEditingDescription = true)
        }

    /**
     * Description text
     *
     * @param text Meeting description
     */
    fun onDescriptionChange(text: String) =
        _state.update { state ->
            state.copy(descriptionText = text.ifEmpty { "" })
        }

    /**
     * Title meeting text
     *
     * @param text Meeting title
     */
    fun onTitleChange(text: String) {
        _state.update { state ->
            state.copy(meetingTitle = text.ifEmpty { "" })
        }

        if (text.isNotEmpty()) {
            _state.update { state ->
                state.copy(isEmptyTitleError = false)
            }
        }
    }

    /**
     * Updates state after shown snackBar.
     */
    fun snackbarShown() = _state.update { it.copy(snackBar = null) }

    /**
     * Discard meeting button clicked
     */
    fun onDiscardMeetingTap() =
        _state.update { state ->
            state.copy(discardMeetingDialog = !state.discardMeetingDialog)
        }

    /**
     * Schedule meeting option
     */
    fun onScheduleMeetingTap() {
        if (!state.value.isMeetingTitleRightSize()) {
            return
        }

        if (state.value.isMeetingDescriptionTooLong()) {
            return
        }

        _state.update { state ->
            state.copy(isEmptyTitleError = state.meetingTitle.isEmpty())
        }

        if (state.value.meetingTitle.isNotEmpty()) {
            viewModelScope.launch {
                runCatching {
                    _state.value.let {
                        val flags = ChatScheduledFlags(
                            sendEmails = it.enabledSendCalendarInviteOption,
                            isEmpty = false
                        )

                        createChatroomAndSchedMeetingUseCase(
                            peerList = it.getParticipantsIds(),
                            isMeeting = true,
                            publicChat = true,
                            title = it.meetingTitle,
                            speakRequest = false,
                            waitingRoom = false,
                            openInvite = it.enabledAllowAddParticipantsOption,
                            timezone = ZoneId.systemDefault().id,
                            startDate = it.startDate.epochSecond,
                            endDate = it.endDate.epochSecond,
                            description = it.descriptionText,
                            flags = flags,
                            rules = null,
                            attributes = null
                        )
                    }
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess {
                    it.chatHandle?.let { id ->
                        if (state.value.enabledMeetingLinkOption) {
                            createMeetingLink(id)
                        } else {
                            openInfo(id)
                        }
                    }
                }
            }
        }
    }

    /**
     * Open chat room with specific id
     *
     * @param chatId Chat Id.
     */
    fun openInfo(chatId: Long?) {
        Timber.d("Scheduled meeting created, open scheduled meeting info with chat id $chatId")
        _state.update { state ->
            state.copy(chatIdToOpenInfoScreen = chatId)
        }
    }

    /**
     * Get participants emails
     */
    fun getEmails(): ArrayList<String> = ArrayList(_state.value.getParticipantsEmails())

    /**
     * Create meeting link
     *
     * @param chatId Chat Id.
     */
    private fun createMeetingLink(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                createChatLink(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { request ->
                request.chatHandle?.let { id ->
                    openInfo(id)
                }
            }
        }

    /**
     * Dismiss alert dialogs
     */
    fun dismissDialog() =
        _state.update { state ->
            state.copy(
                discardMeetingDialog = false,
            )
        }
}