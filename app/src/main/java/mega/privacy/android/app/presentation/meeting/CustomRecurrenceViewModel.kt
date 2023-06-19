package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.meeting.mapper.DropdownOccurrenceTypeMapper
import mega.privacy.android.app.presentation.meeting.mapper.OccurrenceFrequencyTypeMapper
import mega.privacy.android.app.presentation.meeting.mapper.WeekDayMapper
import mega.privacy.android.app.presentation.meeting.model.CustomRecurrenceState
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.Weekday
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import java.time.DayOfWeek
import javax.inject.Inject


/**
 * ScheduleMeetingActivity view model.
 * @property monitorConnectivityUseCase                 [MonitorConnectivityUseCase]
 * @property occurrenceFrequencyTypeMapper           [OccurrenceFrequencyTypeMapper]
 * @property dropdownOccurrenceTypeMapper            [DropdownOccurrenceTypeMapper]
 * @property weekDayMapper                              [WeekDayMapper]
 * @property state                                      Current view state as [CustomRecurrenceState]
 */
@HiltViewModel
class CustomRecurrenceViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val occurrenceFrequencyTypeMapper: OccurrenceFrequencyTypeMapper,
    private val dropdownOccurrenceTypeMapper: DropdownOccurrenceTypeMapper,
    private val weekDayMapper: WeekDayMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomRecurrenceState())
    val state: StateFlow<CustomRecurrenceState> = _state

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
     * Set recurrence rules
     *
     * @param rulesReceived [ChatScheduledRules]
     */
    fun setRules(rulesReceived: ChatScheduledRules?) {
        rulesReceived?.let {
            _state.update { state ->
                state.copy(
                    rules = it,
                    dropdownOccurrenceType = occurrenceFrequencyTypeMapper(it.freq),
                    isWeekdaysSelected = isWeekdays(it)
                )
            }

            setMaxOccurrenceNumber()
            checkValidCustomRecurrence()
        }
    }

    /**
     * Get recurrence rules
     *
     * @return [ChatScheduledRules]
     */
    fun getRules(): ChatScheduledRules = state.value.rules

    /**
     * Check if is week days selected
     *
     * @param rules [ChatScheduledRules]
     * @return True if weekdays are selected. False, if not.
     */
    private fun isWeekdays(rules: ChatScheduledRules): Boolean =
        rules.freq == OccurrenceFrequencyType.Daily && rules.weekDayList == getWeekdaysList()

    /**
     * List of weekdays
     *
     * @return List of [Weekday]
     */
    private fun getWeekdaysList(): List<Weekday> = listOf(
        weekDayMapper(DayOfWeek.MONDAY),
        weekDayMapper(DayOfWeek.TUESDAY),
        weekDayMapper(DayOfWeek.WEDNESDAY),
        weekDayMapper(DayOfWeek.THURSDAY),
        weekDayMapper(DayOfWeek.FRIDAY)
    )

    /**
     * Control when the user changes the occurrence type
     *
     * @param newDropdownOccurrenceType [DropdownOccurrenceType]
     */
    fun onSelectType(newDropdownOccurrenceType: DropdownOccurrenceType) {
        _state.update { state ->
            state.copy(
                dropdownOccurrenceType = newDropdownOccurrenceType,
                rules = state.rules.copy(
                    freq = dropdownOccurrenceTypeMapper(newDropdownOccurrenceType),
                ),
            )
        }

        setMaxOccurrenceNumber()
        checkValidCustomRecurrence()
    }

    /**
     * Control when the user changes the occurrence number
     *
     * @param newOccurrenceNumber
     */
    fun onSelectNumber(newOccurrenceNumber: String) {
        state.value.maxOccurrenceNumber?.let { max ->
            val newValue: Int? = if (newOccurrenceNumber.isEmpty()) {
                null
            } else if (newOccurrenceNumber.toInt() > max) {
                state.value.rules.interval
            } else {
                newOccurrenceNumber.toInt()
            }

            _state.update { state ->
                state.copy(
                    rules = state.rules.copy(
                        interval = newValue ?: -1,
                    ),

                    )
            }

            checkValidCustomRecurrence()
        }
    }

    /**
     * Weekdays option clicked
     */
    fun onWeekdaysOptionClicked() {
        val newValue = !state.value.isWeekdaysSelected
        updateWeekdays(newValue)
        checkValidCustomRecurrence()
    }

    /**
     * Set max value in number check
     */
    private fun setMaxOccurrenceNumber() {
        _state.update {
            it.copy(
                maxOccurrenceNumber = when (state.value.dropdownOccurrenceType) {
                    DropdownOccurrenceType.Day -> 99
                    DropdownOccurrenceType.Week -> 52
                    DropdownOccurrenceType.Month -> 12
                    null -> 1
                }
            )
        }
    }

    /**
     * Check if the custom occurrence is valid
     */
    private fun checkValidCustomRecurrence() {
        val isValid = state.value.isWeekdaysSelected ||
                (state.value.dropdownOccurrenceType != DropdownOccurrenceType.Day) ||
                (state.value.rules.interval != -1 && state.value.rules.interval != 0 && state.value.rules.interval != 1)

        _state.update {
            it.copy(
                isValidRecurrence = isValid
            )
        }
    }

    /**
     * Enable or disable Weekdays option
     *
     * @param enable True, if should be enabled. False, if not.
     */
    private fun updateWeekdays(enable: Boolean) {
        _state.update { it.copy(isWeekdaysSelected = enable) }
        _state.update { state ->
            state.copy(
                isWeekdaysSelected = enable,
                rules = state.rules.copy(
                    weekDayList = if (enable) getWeekdaysList() else null,
                    interval = if (enable) 1 else state.rules.interval,
                    freq = if (enable) OccurrenceFrequencyType.Daily else state.rules.freq
                )
            )
        }
    }

    /**
     * Check when focus changed and disable weekdays option
     */
    fun onFocusChanged() {
        updateWeekdays(false)
    }
}