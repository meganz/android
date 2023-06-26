package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType

/**
 * Data class defining the custom recurrence state
 *
 * @property newRules                   [ChatScheduledRules]
 * @property dropdownOccurrenceType     [DropdownOccurrenceType]
 * @property isWeekdaysSelected         True if weekday option is selected. False, if not.
 * @property isValidRecurrence          True if the custom recurrence is valid. False, if not.
 */
data class CustomRecurrenceState constructor(
    val newRules: ChatScheduledRules = ChatScheduledRules(),
    val dropdownOccurrenceType: DropdownOccurrenceType = DropdownOccurrenceType.Day,
    val isWeekdaysSelected: Boolean = false,
    val isValidRecurrence: Boolean = true,
)