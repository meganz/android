package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getZoneEndTime
import mega.privacy.android.app.presentation.extensions.getZoneStartTime
import mega.privacy.android.app.presentation.extensions.meeting.DropdownType
import mega.privacy.android.app.presentation.extensions.meeting.MaximumValue
import mega.privacy.android.app.presentation.extensions.meeting.OccurrenceType
import mega.privacy.android.app.presentation.extensions.meeting.getUntilZonedDateTime
import mega.privacy.android.app.presentation.extensions.meeting.isForever
import mega.privacy.android.app.presentation.mapper.GetPluralStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.mapper.RecurrenceDialogOptionMapper
import mega.privacy.android.app.presentation.meeting.mapper.WeekDayMapper
import mega.privacy.android.app.presentation.meeting.model.CreateScheduledMeetingState
import mega.privacy.android.app.presentation.meeting.model.CustomRecurrenceState
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.EndsRecurrenceOption
import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import mega.privacy.android.domain.entity.meeting.MonthlyRecurrenceOption
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingType
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveChatLink
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.chat.InviteParticipantToChatUseCase
import mega.privacy.android.domain.usecase.chat.RemoveParticipantFromChatUseCase
import mega.privacy.android.domain.usecase.chat.SetOpenInviteUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetContactItem
import mega.privacy.android.domain.usecase.meeting.CreateChatroomAndSchedMeetingUseCase
import mega.privacy.android.domain.usecase.meeting.SetWaitingRoomRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.SetWaitingRoomUseCase
import mega.privacy.android.domain.usecase.meeting.UpdateScheduledMeetingUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * CreateScheduledMeetingActivity view model.
 * @property monitorConnectivityUseCase                 [MonitorConnectivityUseCase]
 * @property getVisibleContactsUseCase                  [GetVisibleContactsUseCase]
 * @property getScheduledMeetingByChat                  [GetScheduledMeetingByChat]
 * @property getContactFromEmailUseCase                 [GetContactFromEmailUseCase]
 * @property getContactItem                             [GetContactItem]
 * @property getChatRoomUseCase                         [GetChatRoom]
 * @property createChatroomAndSchedMeetingUseCase       [CreateChatroomAndSchedMeetingUseCase]
 * @property updateScheduledMeetingUseCase              [UpdateScheduledMeetingUseCase]
 * @property createChatLink                             [CreateChatLink]
 * @property removeChatLink                             [RemoveChatLink]
 * @property recurrenceDialogOptionMapper               [RecurrenceDialogOptionMapper]
 * @property weekDayMapper                              [WeekDayMapper]
 * @property deviceGateway                              [DeviceGateway]
 * @property getStringFromStringResMapper               [GetStringFromStringResMapper]
 * @property getPluralStringFromStringResMapper         [GetPluralStringFromStringResMapper]
 * @property setOpenInvite                              [SetOpenInvite]
 * @property queryChatLink                              [QueryChatLink]
 * @property removeParticipantFromChat                  [RemoveParticipantFromChatUseCase]
 * @property inviteParticipantToChat                    [InviteParticipantToChatUseCase]
 * @property monitorChatRoomUpdates                     [MonitorChatRoomUpdates]
 * @property setWaitingRoomUseCase                      [SetWaitingRoomUseCase]
 * @property setWaitingRoomRemindersUseCase             [SetWaitingRoomRemindersUseCase]
 * @property state                                      Current view state as [CreateScheduledMeetingState]
 */
@HiltViewModel
class CreateScheduledMeetingViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val getContactItem: GetContactItem,
    private val getChatRoomUseCase: GetChatRoom,
    private val createChatroomAndSchedMeetingUseCase: CreateChatroomAndSchedMeetingUseCase,
    private val updateScheduledMeetingUseCase: UpdateScheduledMeetingUseCase,
    private val createChatLink: CreateChatLink,
    private val removeChatLink: RemoveChatLink,
    private val queryChatLink: QueryChatLink,
    private val recurrenceDialogOptionMapper: RecurrenceDialogOptionMapper,
    private val weekDayMapper: WeekDayMapper,
    private val deviceGateway: DeviceGateway,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val getPluralStringFromStringResMapper: GetPluralStringFromStringResMapper,
    private val setOpenInviteUseCase: SetOpenInviteUseCase,
    private val removeParticipantFromChat: RemoveParticipantFromChatUseCase,
    private val inviteParticipantToChat: InviteParticipantToChatUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val setWaitingRoomUseCase: SetWaitingRoomUseCase,
    private val setWaitingRoomRemindersUseCase: SetWaitingRoomRemindersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateScheduledMeetingState())
    val state: StateFlow<CreateScheduledMeetingState> = _state

    /**
     * Check if it's 24 hour format
     */
    val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

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

    init {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    startDate = state.getInitialStartDate(),
                    endDate = state.getInitialEndDate()
                )
            }
        }
    }

    /**
     * Get chat room of the scheduled meeting
     */
    fun getChatRoom(chatId: Long) {
        if (state.value.scheduledMeeting?.chatId == chatId) {
            return
        }

        val isEdition = chatId != -1L
        _state.update { state ->
            state.copy(
                type = if (isEdition) ScheduledMeetingType.Edition else ScheduledMeetingType.Creation,
            )
        }

        if (isEdition) {
            getScheduledMeeting(chatId)
            viewModelScope.launch {
                runCatching {
                    getChatRoomUseCase(chatId)
                }.onSuccess { chatRoom ->
                    chatRoom?.let { chat ->
                        _state.update {
                            it.copy(
                                enabledAllowAddParticipantsOption = chat.isOpenInvite,
                                initialAllowAddParticipantsOption = chat.isOpenInvite,
                                enabledWaitingRoomOption = chat.isWaitingRoom,
                                initialWaitingRoomOption = chat.isWaitingRoom,
                                numOfParticipants = chat.peerHandlesList.size + 1,
                            )
                        }

                        val list = mutableListOf<ContactItem>()
                        chat.peerHandlesList.forEach { id ->
                            runCatching {
                                getContactItem(UserId(id), isOnline())
                            }.onSuccess { contactItem ->
                                contactItem?.let {
                                    list.add(it)
                                }
                            }.onFailure { exception ->
                                Timber.e(exception)
                            }
                        }

                        _state.update {
                            it.copy(
                                initialParticipantsList = list,
                                participantItemList = list
                            )
                        }

                        checkMeetingLink(chat.chatId)
                        getChatRoomUpdates(chat.chatId)
                    }

                }.onFailure { exception ->
                    Timber.e(exception)
                }
            }
        }
    }

    /**
     * Get scheduled meeting
     *
     * @param chatId Chat id.
     */
    private fun getScheduledMeeting(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(chatId)
            }.onFailure { exception ->
                Timber.e("Scheduled meeting does not exist $exception")
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList
                    ?.firstOrNull { !it.isCanceled && it.parentSchedId == -1L }
                    ?.let { schedMeet ->
                        Timber.d("Scheduled meeting recovered")
                        val initialStartDate = schedMeet.getZoneStartTime()
                        val initialEndDate = schedMeet.getZoneEndTime()
                        val sendCalendarInvite = schedMeet.flags?.sendEmails ?: false
                        val title = schedMeet.title ?: ""
                        val description = schedMeet.description ?: ""
                        _state.update { state ->
                            state.copy(
                                scheduledMeeting = schedMeet,
                                meetingTitle = title,
                                descriptionText = description,
                                isEmptyTitleError = if (title.isNotEmpty()) false else state.isEmptyTitleError,
                                enabledSendCalendarInviteOption = sendCalendarInvite,
                                initialSendCalendarInviteOption = sendCalendarInvite,
                                rulesSelected = schedMeet.rules ?: ChatScheduledRules(),
                                startDate = initialStartDate ?: state.getInitialStartDate(),
                                endDate = initialEndDate ?: state.getInitialEndDate(),
                            )
                        }
                    }
            }
        }

    /**
     * Recurring meeting button clicked
     */
    fun onRecurrenceTap() =
        _state.update { state ->
            state.copy(recurringMeetingDialog = !state.recurringMeetingDialog)
        }

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
        _state.update {
            it.copy(allowAddParticipants = false)
        }

        Timber.d("Add participants to the schedule meeting")
        viewModelScope.launch {
            val contactList = getVisibleContactsUseCase()
            _state.update {
                it.copy(
                    addParticipantsNoContactsDialog = contactList.isEmpty(),
                    openAddContact = contactList.isNotEmpty()
                )
            }
        }
    }

    /**
     * Add selected contacts as participants
     *
     * @param contacts list of contacts selected
     */
    fun addContactsSelected(contacts: ArrayList<String>) {
        val currentList = state.value.participantItemList
        _state.update {
            it.copy(
                numOfParticipants = contacts.size + 1,
            )
        }

        viewModelScope.launch {
            val newList = mutableListOf<ContactItem>()
            contacts.forEach { email ->
                runCatching {
                    getContactFromEmailUseCase(email, isOnline())
                }.onSuccess { contactItem ->
                    contactItem?.let {
                        newList.add(it)
                    }
                }
            }

            val participantsRemoved = currentList.minus(newList.toSet())
            val participantsAdded = newList.minus(currentList.toSet())

            _state.update {
                it.copy(
                    participantItemList = newList,
                )
            }

            if (state.value.participantItemList.isNotEmpty()) {
                if (participantsAdded.isNotEmpty()) {
                    updateParticipantsSnackbarMessage(isAdding = true, list = participantsAdded)
                    _state.update {
                        it.copy(
                            participantsRemoved = participantsRemoved,
                        )
                    }

                    return@launch
                }

                if (participantsRemoved.isNotEmpty()) {
                    updateParticipantsSnackbarMessage(isAdding = false, list = participantsRemoved)
                }
            }
        }
    }

    /**
     * Set until date
     *
     * @param selectedUntilDate     Until date
     */
    fun onUntilDateTap(selectedUntilDate: ZonedDateTime) {
        if (selectedUntilDate.isBefore(ZonedDateTime.now()) || selectedUntilDate.isBefore(state.value.startDate)) {
            return
        }

        updateCustomRules(
            newEndDateOccurrenceOption = selectedUntilDate,
            newUntil = selectedUntilDate.toEpochSecond()
        )
    }

    /**
     * Set start date and time
     *
     * @param selectedStartDate     Start date and time
     */
    fun onStartDateTimeTap(selectedStartDate: ZonedDateTime) {
        if (selectedStartDate.isBefore(ZonedDateTime.now())) {
            return
        }

        val newWeekdayList = newWeekdayList(selectedStartDate)
        val newMonthDayList = newMonthDayList(selectedStartDate)

        _state.update { state ->
            var newUntil = state.rulesSelected.until
            state.rulesSelected.getUntilZonedDateTime()?.let {
                if (it.isBefore(selectedStartDate)) {
                    newUntil = selectedStartDate.plusMonths(
                        6
                    ).toEpochSecond()
                }
            }

            val rightEndDate = getRightEndDate(selectedStartDate)

            Timber.d("Set start date $selectedStartDate and update end date $rightEndDate")

            state.copy(
                startDate = selectedStartDate,
                endDate = rightEndDate,
                rulesSelected = state.rulesSelected.copy(
                    weekDayList = newWeekdayList,
                    monthDayList = newMonthDayList,
                    until = newUntil
                )
            )
        }

        checkMonthWarning()
    }

    /**
     * Get month day list with a new start date
     *
     * @param newStartDate  [ZonedDateTime]
     * @return Month day List
     */
    private fun newMonthDayList(newStartDate: ZonedDateTime) =
        if (state.value.rulesSelected.freq == OccurrenceFrequencyType.Monthly && state.value.rulesSelected.interval == 1) listOf(
            newStartDate.dayOfMonth
        ) else state.value.rulesSelected.monthDayList

    /**
     * Get weekday list with a new start date
     *
     * @param newStartDate  [ZonedDateTime]
     * @return Weekday List
     */
    private fun newWeekdayList(newStartDate: ZonedDateTime) =
        if (state.value.rulesSelected.freq == OccurrenceFrequencyType.Weekly && state.value.rulesSelected.interval == 1) listOf(
            weekDayMapper(
                newStartDate.dayOfWeek
            )
        ) else state.value.rulesSelected.weekDayList

    /**
     * Get the right end date when I change the start date
     *
     * @param newStartDate  [ZonedDateTime]
     * @return newEndDate   [ZonedDateTime]
     */
    private fun getRightEndDate(newStartDate: ZonedDateTime): ZonedDateTime {
        val startDate = getOnlyDate(newStartDate)
        val endDate = getOnlyDate(state.value.endDate)

        val startDateWithEndTime =
            newStartDate
                .withHour(state.value.endDate.hour)
                .withMinute(state.value.endDate.minute)
                .withSecond(state.value.endDate.second)

        return when {
            state.value.endDate.isAfter(newStartDate) -> state.value.endDate
            state.value.endDate.isEqual(newStartDate) ||
                    endDate.isEqual(startDate) -> newStartDate.plus(30, ChronoUnit.MINUTES)

            startDateWithEndTime.isAfter(newStartDate) -> startDateWithEndTime
            else -> newStartDate.plus(30, ChronoUnit.MINUTES)

        }
    }

    /**
     * Get only date of ZonedDateTime
     *
     * @return [ZonedDateTime]
     */
    private fun getOnlyDate(zonedDateTime: ZonedDateTime) = zonedDateTime
        .withHour(0)
        .withMinute(0)
        .withSecond(0)

    /**
     * Get the right start date when I change the end date
     *
     * @param newEndDate  [ZonedDateTime]
     * @return newStartDate   [ZonedDateTime]
     */
    private fun getRightStartDate(newEndDate: ZonedDateTime): ZonedDateTime {
        val startDate = getOnlyDate(state.value.startDate)
        val endDate = getOnlyDate(newEndDate)

        val endDateWithStartTime = newEndDate
            .withHour(state.value.startDate.hour)
            .withMinute(state.value.startDate.minute)
            .withSecond(state.value.startDate.second)

        return when {
            state.value.startDate.isBefore(newEndDate) -> state.value.startDate
            state.value.startDate.isEqual(newEndDate) ||
                    endDate.isEqual(startDate) -> newEndDate.minus(30, ChronoUnit.MINUTES)

            endDateWithStartTime.isBefore(newEndDate) -> endDateWithStartTime
            else -> newEndDate.minus(30, ChronoUnit.MINUTES)
        }
    }

    /**
     * Set end date and time
     *
     * @param selectedEndDate   End date and time
     */
    fun onEndDateTimeTap(selectedEndDate: ZonedDateTime) {
        if (selectedEndDate.isBefore(ZonedDateTime.now())) {
            return
        }

        val rightStartDate = getRightStartDate(selectedEndDate)

        val newWeekdayList = newWeekdayList(rightStartDate)
        val newMonthDayList = newMonthDayList(rightStartDate)

        _state.update { state ->
            var newUntil = state.rulesSelected.until
            state.rulesSelected.getUntilZonedDateTime()?.let {
                if (it.isBefore(rightStartDate)) {
                    newUntil = rightStartDate.plusMonths(
                        6
                    ).toEpochSecond()
                }
            }

            Timber.d("Set end date $selectedEndDate and update start date $rightStartDate")

            state.copy(
                startDate = rightStartDate,
                endDate = selectedEndDate,
                rulesSelected = state.rulesSelected.copy(
                    weekDayList = newWeekdayList,
                    monthDayList = newMonthDayList,
                    until = newUntil
                )
            )
        }

        checkMonthWarning()
    }

    /**
     * Recurrence meeting changed
     *
     * @param optionSelected Recurrence option selected
     */
    fun onDefaultRecurrenceOptionTap(optionSelected: RecurrenceDialogOption) {
        val newFreq = recurrenceDialogOptionMapper(
            optionSelected
        )

        _state.update { state ->
            state.copy(
                rulesSelected = state.rulesSelected.copy(
                    freq = newFreq,
                    interval = if (newFreq == OccurrenceFrequencyType.Invalid) 0 else 1,
                    until = state.rulesSelected.until,
                    weekDayList = when (newFreq) {
                        OccurrenceFrequencyType.Weekly -> listOf(state.getStartWeekDay())
                        else -> null
                    },
                    monthDayList = when (newFreq) {
                        OccurrenceFrequencyType.Monthly -> state.getStartMonthDayList()
                        else -> null
                    },
                    monthWeekDayList = emptyList()
                )
            )
        }

        checkMonthWarning()
    }

    /**
     * Check if month warning should be shown
     */
    private fun shouldShownMonthWarning(
        freq: OccurrenceFrequencyType,
        dayOfMonth: Int?,
    ): Boolean =
        dayOfMonth != null && freq == OccurrenceFrequencyType.Monthly && (dayOfMonth == MONTH_WITH_29_DAYS || dayOfMonth == MONTH_WITH_30_DAYS || dayOfMonth == MONTH_WITH_31_DAYS)

    /**
     * Check if month warning should be shown
     */
    private fun checkMonthWarning() =
        _state.update { state ->
            state.copy(
                showMonthlyRecurrenceWarning = shouldShownMonthWarning(
                    state.rulesSelected.freq,
                    state.rulesSelected.monthDayList.takeIf { !it.isNullOrEmpty() }?.first()
                )
            )
        }

    /**
     * Sets openAddContact as consumed.
     */
    fun setOnOpenAddContactConsumed() = _state.update { state ->
        state.copy(openAddContact = null)
    }

    /**
     * Allow add participant option
     */
    fun allowAddParticipantsOption() =
        _state.update {
            it.copy(allowAddParticipants = true)
        }

    /**
     * Enable or disable allow non-hosts to add participants option
     */
    fun onAllowNonHostAddParticipantsTap() =
        _state.update { state ->
            val newValueForAllowAddParticipantsOption = !state.enabledAllowAddParticipantsOption
            if (state.enabledWaitingRoomOption && newValueForAllowAddParticipantsOption) {
                setWaitingRoomReminderEnabled()
            }
            state.copy(
                enabledAllowAddParticipantsOption = newValueForAllowAddParticipantsOption,
            )
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
    fun onDescriptionChange(text: String) {
        _state.update { state ->
            state.copy(descriptionText = text.ifEmpty { "" })
        }
    }

    /**
     * Title meeting text
     *
     * @param text Meeting title
     */
    fun onTitleChange(text: String) =
        _state.update { state ->
            state.copy(
                meetingTitle = text.ifEmpty { "" },
                isEmptyTitleError = if (text.isNotEmpty()) false else state.isEmptyTitleError
            )
        }

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
            val newFreq =
                if (state.value.isWeekdays()) OccurrenceFrequencyType.Weekly else state.value.rulesSelected.freq

            val newUntil =
                if (state.value.rulesSelected.until > 0 && state.value.rulesSelected.until < state.value.startDate.toEpochSecond()) state.value.startDate.toEpochSecond() else state.value.rulesSelected.until

            _state.update { state ->
                state.copy(
                    isCreatingMeeting = true, rulesSelected = state.rulesSelected.copy(
                        freq = newFreq,
                        until = newUntil
                    )
                )
            }
            val flags = ChatScheduledFlags(
                sendEmails = state.value.enabledSendCalendarInviteOption,
                isEmpty = false
            )
            val peerList = state.value.getParticipantsIds()
            val chatId = state.value.scheduledMeeting?.chatId ?: -1L
            viewModelScope.launch {
                runCatching {
                    _state.value.let { state ->
                        Timber.d("Create or edit scheduled meeting")
                        when (state.type) {
                            ScheduledMeetingType.Creation ->
                                createChatroomAndSchedMeetingUseCase(
                                    peerList = peerList,
                                    isMeeting = true,
                                    publicChat = true,
                                    title = state.meetingTitle,
                                    speakRequest = false,
                                    waitingRoom = state.enabledWaitingRoomOption,
                                    openInvite = state.enabledAllowAddParticipantsOption,
                                    timezone = ZoneId.systemDefault().id,
                                    startDate = state.startDate.toEpochSecond(),
                                    endDate = state.endDate.toEpochSecond(),
                                    description = state.descriptionText,
                                    flags = flags,
                                    rules = state.rulesSelected,
                                    attributes = null
                                )

                            ScheduledMeetingType.Edition ->
                                updateScheduledMeetingUseCase(
                                    chatId = chatId,
                                    schedId = state.scheduledMeeting?.schedId ?: -1L,
                                    timezone = state.scheduledMeeting?.timezone
                                        ?: ZoneId.systemDefault().id,
                                    startDate = state.startDate.toEpochSecond(),
                                    endDate = state.endDate.toEpochSecond(),
                                    title = state.meetingTitle,
                                    description = state.descriptionText,
                                    cancelled = false,
                                    flags = flags,
                                    rules = state.rulesSelected
                                )
                        }
                    }
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { state ->
                        state.copy(isCreatingMeeting = false)
                    }
                }.onSuccess { request ->
                    _state.update { state ->
                        state.copy(isCreatingMeeting = false)
                    }

                    (when (state.value.type) {
                        ScheduledMeetingType.Creation -> request.chatHandle
                        ScheduledMeetingType.Edition -> chatId
                    }).takeIf { it != -1L }?.let { id ->
                        when (state.value.type) {
                            ScheduledMeetingType.Creation -> {
                                if (state.value.enabledMeetingLinkOption) {
                                    createMeetingLink(id)
                                }

                                Timber.d("Scheduled meeting created, open scheduled meeting info with chat id $id")
                                _state.update { state ->
                                    state.copy(chatIdToOpenInfoScreen = id)
                                }
                            }

                            ScheduledMeetingType.Edition -> {
                                setOpenInvite(id, state.value.enabledAllowAddParticipantsOption)
                                setParticipants(id)
                                if (state.value.enabledMeetingLinkOption) {
                                    createMeetingLink(id)
                                } else {
                                    removeMeetingLink(id)
                                }

                                if (state.value.initialWaitingRoomOption != state.value.enabledWaitingRoomOption) {
                                    setWaitingRoom(id, state.value.enabledWaitingRoomOption)
                                } else {
                                    _state.update { it.copy(finish = true) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Enable or disable waiting room option
     */
    fun onWaitingRoomTap() =
        _state.update { state ->
            val newValueForWaitingRoomOption = !state.enabledWaitingRoomOption
            if (newValueForWaitingRoomOption && state.enabledAllowAddParticipantsOption) {
                setWaitingRoomReminderEnabled()
            }
            state.copy(
                enabledWaitingRoomOption = newValueForWaitingRoomOption,
            )
        }

    /**
     * Sets chatIdToOpenInfoScreen as consumed.
     */
    fun setOnOpenInfoConsumed() = _state.update { state ->
        state.copy(chatIdToOpenInfoScreen = null)
    }

    /**
     * Get chat room updates
     *
     * @param chatId Chat id.
     */
    private fun getChatRoomUpdates(chatId: Long) =
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId).collectLatest { chat ->
                _state.update { state ->
                    with(state) {
                        val waitingRoomValue = if (chat.hasChanged(ChatRoomChange.WaitingRoom)) {
                            Timber.d("Changes in waiting room")
                            if (chat.isWaitingRoom && enabledAllowAddParticipantsOption) {
                                setWaitingRoomReminderEnabled()
                            }

                            chat.isWaitingRoom
                        } else {
                            enabledWaitingRoomOption
                        }

                        val openInviteValue = if (chat.hasChanged(ChatRoomChange.OpenInvite)) {
                            Timber.d("Changes in OpenInvite")
                            if (enabledWaitingRoomOption && chat.isOpenInvite) {
                                setWaitingRoomReminderEnabled()
                            }
                            chat.isOpenInvite
                        } else {
                            enabledAllowAddParticipantsOption
                        }

                        copy(
                            enabledWaitingRoomOption = waitingRoomValue,
                            enabledAllowAddParticipantsOption = openInviteValue
                        )
                    }
                }
            }
        }

    /**
     * Enable waiting room reminder
     */
    private fun setWaitingRoomReminderEnabled() = viewModelScope.launch {
        runCatching {
            setWaitingRoomRemindersUseCase(WaitingRoomReminders.Enabled)
        }
    }

    /**
     * Get participants emails
     */
    fun getEmails(): ArrayList<String> = ArrayList(_state.value.getParticipantsEmails())

    /**
     * Set open invite
     *
     * @param chatId        Chat Id.
     * @param isEnabled     True, enabled allow non-host add participants. False, otherwise.
     */
    private fun setOpenInvite(chatId: Long, isEnabled: Boolean) =
        viewModelScope.launch {
            runCatching {
                setOpenInviteUseCase(chatId, isEnabled)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Enable or disable waiting room option
     *
     * @param chatId        Chat Id.
     * @param isEnabled     True, enabled allow non-host add participants. False, otherwise.
     */
    private fun setWaitingRoom(chatId: Long, isEnabled: Boolean) =
        viewModelScope.launch {
            runCatching {
                setWaitingRoomUseCase(chatId, isEnabled)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                _state.update { state ->
                    state.copy(
                        finish = true
                    )
                }
            }
        }

    /**
     * Set participants
     *
     * @param chatId    Chat id
     */
    private fun setParticipants(chatId: Long) {
        val participantsToAdd = state.value.participantItemList.filterNot {
            state.value.initialParticipantsList.contains(it)
        }
        val participantsToRemove = state.value.initialParticipantsList.filterNot {
            state.value.participantItemList.contains(it)
        }

        participantsToAdd.takeIf { it.isNotEmpty() }?.forEach {
            inviteParticipant(chatId, it.handle)
        }

        participantsToRemove.takeIf { it.isNotEmpty() }?.forEach {
            removeParticipant(chatId, it.handle)
        }
    }

    /**
     * Invite participant to chat
     *
     * @param chatId    Chat id
     * @param handle    User handle
     */
    private fun inviteParticipant(chatId: Long, handle: Long) =
        viewModelScope.launch {
            runCatching {
                inviteParticipantToChat(chatId, handle)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Remove participant from chat
     *
     * @param chatId    Chat id
     * @param handle    User handle
     */
    private fun removeParticipant(chatId: Long, handle: Long) =
        viewModelScope.launch {
            runCatching {
                removeParticipantFromChat(chatId, handle)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Create meeting link
     *
     * @param chatId    Chat Id.
     */
    private fun createMeetingLink(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                createChatLink(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Remove chat link
     *
     * @param chatId    Chat Id.
     */
    private fun removeMeetingLink(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                removeChatLink(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    /**
     * Check if there is an existing chat-link for an public chat
     */
    private fun checkMeetingLink(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                queryChatLink(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { request ->
                Timber.d("Query chat link successfully")
                val hastMeetingLink = request.text != null
                _state.update {
                    it.copy(
                        enabledMeetingLinkOption = hastMeetingLink,
                        initialMeetingLinkOption = hastMeetingLink
                    )
                }
            }
        }

    /**
     * Set the new initial custom rules
     */
    fun setInitialCustomRules() {
        val monthDayList = state.value.rulesSelected.monthDayList
        updateCustomRules(
            newFreq = if (state.value.rulesSelected.freq == OccurrenceFrequencyType.Invalid) OccurrenceFrequencyType.Daily else state.value.rulesSelected.freq,
            newInterval = if (state.value.rulesSelected.freq == OccurrenceFrequencyType.Invalid) 1 else state.value.rulesSelected.interval,
            newUntil = state.value.rulesSelected.until,
            newWeekDayList = state.value.rulesSelected.weekDayList,
            newMonthDayList = state.value.rulesSelected.monthDayList,
            newMonthWeekDayList = state.value.rulesSelected.monthWeekDayList,
            newMonthDayOption = if (monthDayList.isNullOrEmpty()) state.value.getStartMonthDay() else monthDayList.first(),
            newMonthWeekDayListOption = state.value.rulesSelected.monthWeekDayList.ifEmpty { state.value.getDefaultMonthWeekDayList() },
            newEndDateOccurrenceOption = state.value.rulesSelected.getUntilZonedDateTime()
                ?: state.value.getStartDateTimePlus6Months()
        )
    }

    /**
     * Check when user change dropdown option
     *
     * @param dropdownOccurrenceType    [DropdownOccurrenceType]
     */
    fun onFrequencyTypeChanged(dropdownOccurrenceType: DropdownOccurrenceType) {
        val newType = dropdownOccurrenceType.OccurrenceType
        updateCustomRules(
            newFreq = newType,
            newInterval = 1,
            newWeekDayList = if (newType == OccurrenceFrequencyType.Weekly) mutableListOf<Weekday>().apply {
                add(state.value.getStartWeekDay())
            } else null,
            newMonthDayList = if (newType == OccurrenceFrequencyType.Monthly) mutableListOf<Int>().apply {
                add(state.value.getStartMonthDay())
            } else null,
            newMonthWeekDayList = emptyList()
        )
    }

    /**
     * Check when user change number of occurrences
     *
     * @param newValue
     */
    fun onIntervalChanged(newValue: String) {
        var newInterval: Int? = when {
            newValue.isEmpty() -> -1
            else -> newValue.toIntOrNull()
        }

        newInterval = when {
            newInterval == null || newInterval == 0 || newInterval > state.value.customRecurrenceState.newRules.freq.DropdownType.MaximumValue -> state.value.customRecurrenceState.newRules.interval
            else -> newInterval
        }

        updateCustomRules(
            newInterval = newInterval,
            newWeekDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Weekly) state.value.customRecurrenceState.newRules.weekDayList else null,
            newMonthDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Monthly) state.value.customRecurrenceState.newRules.monthDayList else null,
            newMonthWeekDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Monthly) state.value.customRecurrenceState.newRules.monthWeekDayList else emptyList(),
        )
    }

    /**
     * Check when user change month day of monthly occurrence
     *
     * @param newValue
     */
    fun onMonthDayChanged(newValue: String) {
        var newMonthDay: Int? = when {
            newValue.isEmpty() -> -1
            else -> newValue.toIntOrNull()
        }

        newMonthDay = when {
            newMonthDay == null || newMonthDay == 0 || newMonthDay > MAXIMUM_DAYS_IN_MONTH -> state.value.customRecurrenceState.monthDayOption
            else -> newMonthDay
        }

        updateCustomRules(
            newMonthDayList = listOf(newMonthDay),
            newMonthDayOption = newMonthDay,
        )
    }

    /**
     * Check when user change weekday of monthly occurrence
     *
     * @param newValue  [Weekday]
     */
    fun onMonthWeekDayChanged(newValue: Weekday) {
        val list = listOf(
            MonthWeekDayItem(
                state.value.customRecurrenceState.monthWeekDayListOption.first().weekOfMonth,
                listOf(newValue)
            )
        )

        updateCustomRules(
            newMonthWeekDayList = list,
            newMonthWeekDayListOption = list
        )
    }

    /**
     * Check when user change week of month
     *
     * @param newValue  [WeekOfMonth]
     */
    fun onWeekOfMonthChanged(newValue: WeekOfMonth) {
        val list = listOf(
            MonthWeekDayItem(
                newValue,
                state.value.customRecurrenceState.monthWeekDayListOption.first().weekDaysList
            )
        )

        updateCustomRules(
            newMonthWeekDayList = list,
            newMonthWeekDayListOption = list
        )
    }

    /**
     * Check when focus changed and disable weekdays option
     */
    fun onFocusChanged() =
        updateCustomRules(
            newWeekDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Weekly) state.value.customRecurrenceState.newRules.weekDayList else null,
            newMonthDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Monthly) state.value.customRecurrenceState.newRules.monthDayList else null,
            newMonthWeekDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Monthly) state.value.customRecurrenceState.newRules.monthWeekDayList else emptyList(),
        )

    /**
     * Weekdays option clicked
     */
    fun onWeekdaysOptionTap() {
        val enabled = !state.value.customRecurrenceState.isWeekdaysSelected
        val newFreq =
            if (enabled) OccurrenceFrequencyType.Daily else state.value.customRecurrenceState.newRules.freq
        val newWeekdayList = if (enabled) state.value.getWeekdaysList() else null
        val newInterval =
            if (enabled) 1 else state.value.customRecurrenceState.newRules.interval
        updateCustomRules(
            newFreq = newFreq,
            newInterval = newInterval,
            newWeekDayList = newWeekdayList,
        )
    }

    /**
     * Day clicked
     *
     * @param day  [Weekday]
     */
    fun onDayClicked(day: Weekday) =
        mutableListOf<Weekday>().apply {
            state.value.customRecurrenceState.newRules.weekDayList?.let(::addAll)
        }.let { list ->
            if (state.value.customRecurrenceState.newRules.weekDayList?.contains(day) == true) {
                list.remove(day)
            } else {
                list.add(day)
            }
            updateCustomRules(
                newWeekDayList = list.sortedBy { it.ordinal }
            )
        }

    /**
     * Monthly radio button clicked
     *
     * @param newOptionClicked  [MonthlyRecurrenceOption]
     */
    fun onMonthlyRadioButtonClicked(newOptionClicked: MonthlyRecurrenceOption) {
        _state.update { state ->
            state.copy(
                customRecurrenceState = state.customRecurrenceState.copy(
                    monthlyRadioButtonOptionSelected = newOptionClicked,
                )
            )
        }

        updateCustomRules(
            newMonthDayList = when (newOptionClicked) {
                MonthlyRecurrenceOption.MonthDay -> listOf(state.value.customRecurrenceState.monthDayOption)
                MonthlyRecurrenceOption.MonthWeekday -> null
            },
            newMonthWeekDayList = when (newOptionClicked) {
                MonthlyRecurrenceOption.MonthDay -> emptyList()
                MonthlyRecurrenceOption.MonthWeekday -> state.value.customRecurrenceState.monthWeekDayListOption
            }
        )
    }


    /**
     * Ends radio button clicked
     *
     * @param newOptionClicked  [EndsRecurrenceOption]
     */
    fun onEndsRadioButtonClicked(newOptionClicked: EndsRecurrenceOption) {
        updateCustomRules(
            newUntil = when (newOptionClicked) {
                EndsRecurrenceOption.Never -> 0L
                EndsRecurrenceOption.CustomDate -> state.value.customRecurrenceState.endDateOccurrenceOption.toEpochSecond()
            }
        )
    }


    /**
     * Update custom rules
     *
     * @param newFreq                       new frequency
     * @param newInterval                   new interval
     * @param newUntil                      new value until
     * @param newWeekDayList                new [Weekday] list
     * @param newMonthDayList               new [MonthWeekDayItem] list
     * @param newMonthWeekDayList           new month weekday list
     * @param newMonthDayOption             new month day option
     * @param newMonthDayOption             new [MonthWeekDayItem] list option
     * @param newEndDateOccurrenceOption    new [ZonedDateTime]
     */
    private fun updateCustomRules(
        newFreq: OccurrenceFrequencyType = state.value.customRecurrenceState.newRules.freq,
        newInterval: Int = state.value.customRecurrenceState.newRules.interval,
        newUntil: Long = state.value.customRecurrenceState.newRules.until,
        newWeekDayList: List<Weekday>? = state.value.customRecurrenceState.newRules.weekDayList,
        newMonthDayList: List<Int>? = state.value.customRecurrenceState.newRules.monthDayList,
        newMonthWeekDayList: List<MonthWeekDayItem> = state.value.customRecurrenceState.newRules.monthWeekDayList,
        newMonthDayOption: Int = state.value.customRecurrenceState.monthDayOption,
        newMonthWeekDayListOption: List<MonthWeekDayItem> = state.value.customRecurrenceState.monthWeekDayListOption,
        newEndDateOccurrenceOption: ZonedDateTime = state.value.customRecurrenceState.endDateOccurrenceOption,
    ) {
        val newRules = ChatScheduledRules(
            freq = newFreq,
            interval = newInterval,
            until = newUntil,
            weekDayList = if (newWeekDayList.isNullOrEmpty()) null else newWeekDayList,
            monthDayList = if (newMonthDayList.isNullOrEmpty()) null else newMonthDayList,
            monthWeekDayList = newMonthWeekDayList
        )

        _state.update { state ->
            val isWeekdaysSelected =
                newRules.freq == OccurrenceFrequencyType.Daily && newRules.weekDayList == state.getWeekdaysList()

            val isValidRecurrence =
                newRules != state.rulesSelected && newRules.interval != -1 && (
                        (newRules.freq == OccurrenceFrequencyType.Daily) ||
                                (newRules.freq == OccurrenceFrequencyType.Weekly && !newRules.weekDayList.isNullOrEmpty()) ||
                                (newRules.freq == OccurrenceFrequencyType.Monthly && newMonthDayOption != -1)
                        )

            Timber.d("Check valid recurrence: $isValidRecurrence. Current rules ${state.rulesSelected}, new rules $newRules")

            state.copy(
                customRecurrenceState = state.customRecurrenceState.copy(
                    newRules = newRules,
                    isWeekdaysSelected = isWeekdaysSelected,
                    isValidRecurrence = isValidRecurrence,
                    monthDayOption = newMonthDayOption,
                    monthWeekDayListOption = newMonthWeekDayListOption,
                    showMonthlyRecurrenceWarning = shouldShownMonthWarning(
                        newRules.freq,
                        newRules.monthDayList?.first()
                    ),
                    endDateOccurrenceOption = newEndDateOccurrenceOption,
                    monthlyRadioButtonOptionSelected = when {
                        newRules.monthWeekDayList.isEmpty() -> MonthlyRecurrenceOption.MonthDay
                        else -> MonthlyRecurrenceOption.MonthWeekday
                    },
                    endsRadioButtonOptionSelected = when {
                        newRules.isForever() -> EndsRecurrenceOption.Never
                        else -> EndsRecurrenceOption.CustomDate
                    }
                )
            )
        }

        Timber.d("Custom rules updated ${state.value.customRecurrenceState.newRules}")
    }

    /**
     * On accept custom rules
     */
    fun onAcceptClicked() {
        _state.update { state ->
            state.copy(
                rulesSelected = state.customRecurrenceState.newRules,
                customRecurrenceState = state.customRecurrenceState.copy(newRules = ChatScheduledRules())
            )
        }

        checkMonthWarning()
        Timber.d("Accepted rules ${state.value.customRecurrenceState.newRules}")
    }

    /**
     * On reject custom rules
     */
    fun onRejectClicked() =
        _state.update { state ->
            state.copy(customRecurrenceState = CustomRecurrenceState())
        }

    /**
     * Trigger event to show update participant Snackbar message
     *
     * @param isAdding     True, if is adding participants. False, if is removing participants.
     * @param list         List of participants.
     */
    private fun updateParticipantsSnackbarMessage(isAdding: Boolean, list: List<ContactItem>) =
        triggerSnackbarMessage(
            getPluralStringFromStringResMapper(
                if (isAdding)
                    R.plurals.meetings_schedule_meeting_snackbar_adding_participants_success
                else
                    R.plurals.meetings_schedule_meeting_snackbar_removing_participants_success,
                list.size,
                list.size,
            )
        )

    /**
     * Trigger event to show Snackbar message
     *
     * @param message     Content for snack bar
     */
    private fun triggerSnackbarMessage(message: String) =
        _state.update { it.copy(snackbarMessageContent = triggered(message)) }

    /**
     * Reset and notify that snackbarMessage is consumed
     */
    fun onSnackbarMessageConsumed() {
        val participantsRemoved = state.value.participantsRemoved

        _state.update {
            it.copy(snackbarMessageContent = consumed(), participantsRemoved = emptyList())
        }

        if (participantsRemoved.isNotEmpty()) {
            updateParticipantsSnackbarMessage(isAdding = false, list = participantsRemoved)
        }
    }


    /**
     * Dismiss alert dialogs
     */
    fun dismissDialog() =
        _state.update { state ->
            state.copy(
                discardMeetingDialog = false,
                recurringMeetingDialog = false
            )
        }

    companion object {
        private const val MONTH_WITH_31_DAYS = 31
        private const val MONTH_WITH_30_DAYS = 30
        private const val MONTH_WITH_29_DAYS = 29
        private const val MAXIMUM_DAYS_IN_MONTH = 31
    }
}