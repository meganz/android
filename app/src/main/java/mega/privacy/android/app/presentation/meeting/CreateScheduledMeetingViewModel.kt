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
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.meeting.DropdownType
import mega.privacy.android.app.presentation.extensions.meeting.MaximumValue
import mega.privacy.android.app.presentation.extensions.meeting.OccurrenceType
import mega.privacy.android.app.presentation.meeting.mapper.RecurrenceDialogOptionMapper
import mega.privacy.android.app.presentation.meeting.mapper.WeekDayMapper
import mega.privacy.android.app.presentation.meeting.model.CreateScheduledMeetingState
import mega.privacy.android.app.presentation.meeting.model.CustomRecurrenceState
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import mega.privacy.android.domain.entity.meeting.MonthlyRecurrenceOption
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.CreateChatroomAndSchedMeetingUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * CreateScheduledMeetingActivity view model.
 * @property monitorConnectivityUseCase                 [MonitorConnectivityUseCase]
 * @property getVisibleContactsUseCase                  [GetVisibleContactsUseCase]
 * @property getContactFromEmailUseCase                 [GetContactFromEmailUseCase]
 * @property createChatroomAndSchedMeetingUseCase       [CreateChatroomAndSchedMeetingUseCase]
 * @property createChatLink                             [CreateChatLink]
 * @property recurrenceDialogOptionMapper               [RecurrenceDialogOptionMapper]
 * @property weekDayMapper                              [WeekDayMapper]
 * @property getFeatureFlagValue                        [GetFeatureFlagValueUseCase]
 * @property deviceGateway                              [DeviceGateway]
 * @property state                                      Current view state as [CreateScheduledMeetingState]
 */
@HiltViewModel
class CreateScheduledMeetingViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase,
    private val createChatroomAndSchedMeetingUseCase: CreateChatroomAndSchedMeetingUseCase,
    private val createChatLink: CreateChatLink,
    private val recurrenceDialogOptionMapper: RecurrenceDialogOptionMapper,
    private val weekDayMapper: WeekDayMapper,
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase,
    private val deviceGateway: DeviceGateway,
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
            getFeatureFlagValue(AppFeatures.ScheduleMeeting).let { flag ->
                _state.update {
                    it.copy(
                        scheduledMeetingEnabled = flag,
                        currentWeekDay = weekDayMapper(
                            Instant.now().atZone(ZoneId.systemDefault()).dayOfWeek
                        ),
                        currentDayOfMonth = Instant.now().atZone(ZoneId.systemDefault()).dayOfMonth
                    )
                }
            }
        }
    }

    /**
     * Recurring meeting button clicked
     */
    fun onRecurrenceTap() {
        _state.update { state ->
            state.copy(recurringMeetingDialog = !state.recurringMeetingDialog)
        }
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
                    snackBar = if (list.isEmpty()) null else R.string.number_of_participants
                )
            }
        }
    }

    /**
     * Set start date and time
     *
     * @param selectedStartDate     Start date and time
     * @param isDate                True, if is date. False, if not.
     */
    fun onStartDateTimeTap(selectedStartDate: ZonedDateTime, isDate: Boolean) {
        val nowZonedDateTime: ZonedDateTime = Instant.now().atZone(ZoneId.systemDefault())
        var newStartZonedDateTime =
            if (isDate) selectedStartDate.withHour(state.value.startDate.hour)
                .withMinute(state.value.startDate.minute) else selectedStartDate

        if (newStartZonedDateTime.isBefore(nowZonedDateTime)) {
            if (isDate) {
                return
            }

            newStartZonedDateTime = newStartZonedDateTime.plus(1, ChronoUnit.DAYS)
        }

        val newWeekdayList: List<Weekday>? =
            if (state.value.rulesSelected.freq == OccurrenceFrequencyType.Weekly && state.value.rulesSelected.interval == 1) listOf(
                weekDayMapper(
                    newStartZonedDateTime.dayOfWeek
                )
            ) else state.value.rulesSelected.weekDayList

        val newMonthDayList: List<Int>? =
            if (state.value.rulesSelected.freq == OccurrenceFrequencyType.Monthly && state.value.rulesSelected.interval == 1) listOf(
                newStartZonedDateTime.dayOfMonth
            ) else state.value.rulesSelected.monthDayList

        _state.update { state ->
            val newEndDate =
                if ((state.endDate.isAfter(newStartZonedDateTime))) state.endDate else newStartZonedDateTime.plus(
                    30,
                    ChronoUnit.MINUTES
                )

            state.copy(
                startDate = newStartZonedDateTime,
                endDate = newEndDate,
                rulesSelected = state.rulesSelected.copy(
                    weekDayList = newWeekdayList,
                    monthDayList = newMonthDayList,
                )
            )
        }

        checkMonthWarning()
    }

    /**
     * Set end date and time
     *
     * @param selectedEndDate   End date and time
     * @param isDate            True, if is date. False, if not.
     */
    fun onEndDateTimeTap(selectedEndDate: ZonedDateTime, isDate: Boolean) {
        val newEndZonedDateTime = if (isDate) selectedEndDate.withHour(state.value.endDate.hour)
            .withMinute(state.value.endDate.minute) else selectedEndDate

        if (newEndZonedDateTime.isBefore(state.value.startDate)) {
            return
        }

        _state.update {
            it.copy(
                endDate = newEndZonedDateTime
            )
        }
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
                        OccurrenceFrequencyType.Weekly -> listOf(
                            weekDayMapper(
                                state.startDate.dayOfWeek
                            )
                        )

                        else -> null
                    },
                    monthDayList = when (newFreq) {
                        OccurrenceFrequencyType.Monthly -> listOf(
                            state.startDate.dayOfMonth
                        )

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
    private fun checkMonthWarning() {
        val shouldShown = state.value.rulesSelected.freq == OccurrenceFrequencyType.Monthly &&
                state.value.rulesSelected.interval == 1 &&
                (state.value.startDate.dayOfMonth == MONTH_WITH_29_DAYS ||
                        state.value.startDate.dayOfMonth == MONTH_WITH_30_DAYS ||
                        state.value.startDate.dayOfMonth == MONTH_WITH_31_DAYS)
        _state.update { state -> state.copy(showMonthlyRecurrenceWarning = shouldShown) }
    }

    /**
     * Remove open add contact screen
     */
    fun removeAddContact() =
        _state.update {
            it.copy(openAddContact = null)
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
            _state.update { state ->
                state.copy(
                    isCreatingMeeting = true, rulesSelected = state.rulesSelected.copy(
                        freq = newFreq,
                    )
                )
            }
            viewModelScope.launch {
                runCatching {
                    _state.value.let { state ->
                        val flags = ChatScheduledFlags(
                            sendEmails = state.enabledSendCalendarInviteOption,
                            isEmpty = false
                        )

                        createChatroomAndSchedMeetingUseCase(
                            peerList = state.getParticipantsIds(),
                            isMeeting = true,
                            publicChat = true,
                            title = state.meetingTitle,
                            speakRequest = false,
                            waitingRoom = false,
                            openInvite = state.enabledAllowAddParticipantsOption,
                            timezone = ZoneId.systemDefault().id,
                            startDate = state.startDate.toEpochSecond(),
                            endDate = state.endDate.toEpochSecond(),
                            description = state.descriptionText,
                            flags = flags,
                            rules = state.rulesSelected,
                            attributes = null
                        )
                    }
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { state ->
                        state.copy(isCreatingMeeting = false)
                    }
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
     * Set the new initial custom rules
     */
    fun setInitialCustomRules() {

        updateCustomRules(
            newFreq = if (state.value.rulesSelected.freq == OccurrenceFrequencyType.Invalid) OccurrenceFrequencyType.Daily else state.value.rulesSelected.freq,
            newInterval = if (state.value.rulesSelected.freq == OccurrenceFrequencyType.Invalid) 1 else state.value.rulesSelected.interval,
            newUntil = state.value.rulesSelected.until,
            newWeekDayList = state.value.rulesSelected.weekDayList,
            newMonthDayList = state.value.rulesSelected.monthDayList,
            newMonthWeekDayList = state.value.rulesSelected.monthWeekDayList,
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
                add(state.value.currentWeekDay)
            } else null,
            newMonthDayList = if (newType == OccurrenceFrequencyType.Monthly) mutableListOf<Int>().apply {
                add(state.value.currentDayOfMonth)
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
        val newInterval = when {
            newValue.isEmpty() -> -1
            newValue.toInt() > state.value.customRecurrenceState.newRules.freq.DropdownType.MaximumValue -> state.value.customRecurrenceState.newRules.interval
            else -> newValue.toInt()
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
        val currentList = state.value.customRecurrenceState.newRules.monthDayList
        val newMonthDay = when {
            newValue.isEmpty() -> -1
            newValue.toInt() > MAXIMUM_DAYS_IN_MONTH -> if (currentList.isNullOrEmpty()) 1 else currentList.first()
            else -> newValue.toInt()
        }

        updateCustomRules(
            newMonthDayList = mutableListOf<Int>().apply {
                add(newMonthDay)
            },
        )
    }

    /**
     * Check when user change weekday of monthly occurrence
     *
     * @param newValue  [Weekday]
     */
    fun onMonthWeekDayChanged(newValue: Weekday) {
        val monthWeekdayList: List<MonthWeekDayItem> =
            state.value.customRecurrenceState.newRules.monthWeekDayList

        val currentWeekOfMonth: WeekOfMonth =
            if (monthWeekdayList.isEmpty()) WeekOfMonth.First else monthWeekdayList.first().weekOfMonth
        val newWeekdaysList = mutableListOf<Weekday>().apply {
            add(newValue)
        }

        updateCustomRules(
            newMonthWeekDayList = mutableListOf<MonthWeekDayItem>().apply {
                add(MonthWeekDayItem(currentWeekOfMonth, newWeekdaysList))
            },
        )
    }

    /**
     * Check when user change week of month
     *
     * @param newValue  [WeekOfMonth]
     */
    fun onWeekOfMonthChanged(newValue: WeekOfMonth) {
        val monthWeekdayList: List<MonthWeekDayItem> =
            state.value.customRecurrenceState.newRules.monthWeekDayList

        val currentWeekdaysList: List<Weekday> =
            if (monthWeekdayList.isEmpty()) mutableListOf<Weekday>().apply {
                add(Weekday.Monday)
            } else monthWeekdayList.first().weekDaysList


        updateCustomRules(
            newMonthWeekDayList = mutableListOf<MonthWeekDayItem>().apply {
                add(MonthWeekDayItem(newValue, currentWeekdaysList))
            },
        )
    }

    /**
     * Check when focus changed and disable weekdays option
     */
    fun onFocusChanged() {
        updateCustomRules(
            newWeekDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Weekly) state.value.customRecurrenceState.newRules.weekDayList else null,
            newMonthDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Monthly) state.value.customRecurrenceState.newRules.monthDayList else null,
            newMonthWeekDayList = if (state.value.customRecurrenceState.newRules.freq == OccurrenceFrequencyType.Monthly) state.value.customRecurrenceState.newRules.monthWeekDayList else emptyList(),
        )
    }

    /**
     * Weekdays option clicked
     */
    fun onWeekdaysOptionTap() {
        val enabled = !state.value.customRecurrenceState.isWeekdaysSelected

        val newFreq =
            if (enabled) OccurrenceFrequencyType.Daily else state.value.customRecurrenceState.newRules.freq
        val newWeekdayList = if (enabled) state.value.getWeekdaysList() else null
        val newInterval = if (enabled) 1 else state.value.customRecurrenceState.newRules.interval

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
    fun onDayClicked(day: Weekday) {
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
    }

    /**
     * Update custom rules
     *
     * @param newFreq                   new frequency
     * @param newInterval               new interval
     * @param newUntil                  new value until
     * @param newWeekDayList            new [Weekday] list
     * @param newMonthDayList           new month day list
     * @param newMonthWeekDayList       new month weekday list
     */
    private fun updateCustomRules(
        newFreq: OccurrenceFrequencyType = state.value.customRecurrenceState.newRules.freq,
        newInterval: Int = state.value.customRecurrenceState.newRules.interval,
        newUntil: Long = state.value.customRecurrenceState.newRules.until,
        newWeekDayList: List<Weekday>? = state.value.customRecurrenceState.newRules.weekDayList,
        newMonthDayList: List<Int>? = state.value.customRecurrenceState.newRules.monthDayList,
        newMonthWeekDayList: List<MonthWeekDayItem> = state.value.customRecurrenceState.newRules.monthWeekDayList,
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

            state.copy(
                customRecurrenceState = state.customRecurrenceState.copy(
                    newRules = newRules,
                    isWeekdaysSelected = isWeekdaysSelected,
                    isValidRecurrence = newRules != state.rulesSelected && (!isWeekdaysSelected || newRules.interval == 1),
                )
            )
        }

        Timber.d("New rules selected ${state.value.customRecurrenceState.newRules}")
    }

    /**
     * On accept custom rules
     */
    fun onAcceptClicked() {
        when (state.value.customRecurrenceState.newRules.freq) {
            OccurrenceFrequencyType.Invalid -> {}
            OccurrenceFrequencyType.Daily ->
                _state.update { state ->
                    state.copy(
                        customRecurrenceState = state.customRecurrenceState.copy(
                            newRules = state.customRecurrenceState.newRules.copy(
                                weekDayList = null,
                                monthDayList = null,
                                monthWeekDayList = emptyList()
                            )
                        ),
                    )
                }

            OccurrenceFrequencyType.Weekly ->
                _state.update { state ->
                    state.copy(
                        customRecurrenceState = state.customRecurrenceState.copy(
                            newRules = state.customRecurrenceState.newRules.copy(
                                monthDayList = null,
                                monthWeekDayList = emptyList()
                            )
                        ),
                    )
                }

            OccurrenceFrequencyType.Monthly ->
                _state.update { state ->
                    state.copy(
                        customRecurrenceState = state.customRecurrenceState.copy(
                            newRules = state.customRecurrenceState.newRules.copy(
                                weekDayList = null,
                                monthDayList = if (state.customRecurrenceState.monthlyRadioButtonOptionSelected == MonthlyRecurrenceOption.MonthWeekday ||
                                    state.customRecurrenceState.newRules.monthDayList?.get(0) == -1
                                )
                                    null
                                else
                                    state.customRecurrenceState.newRules.monthDayList,
                                monthWeekDayList = if (state.customRecurrenceState.monthlyRadioButtonOptionSelected == MonthlyRecurrenceOption.MonthDay)
                                    emptyList()
                                else
                                    state.customRecurrenceState.newRules.monthWeekDayList
                            )
                        ),
                    )
                }
        }

        val newRulesSelected = state.value.customRecurrenceState.newRules
        val monthlyRadioButtonOptionSelected =
            state.value.customRecurrenceState.monthlyRadioButtonOptionSelected
        _state.update { state ->
            state.copy(
                rulesSelected = newRulesSelected,
                customRecurrenceState = CustomRecurrenceState(monthlyRadioButtonOptionSelected = monthlyRadioButtonOptionSelected),
            )
        }

    }

    /**
     * On reject custom rules
     */
    fun onRejectClicked() {
        _state.update { state ->
            state.copy(customRecurrenceState = CustomRecurrenceState())
        }
    }

    /**
     * Updates state after shown snackBar.
     */
    fun snackbarShown() = _state.update { it.copy(snackBar = null) }

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