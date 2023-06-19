package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.CustomRecurrenceViewModel
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.DropdownOccurrenceType

/**
 * Data class defining the state of [CustomRecurrenceViewModel]
 *

 * @property rules                      [ChatScheduledRules]
 * @property dropdownOccurrenceType     [DropdownOccurrenceType]
 * @property maxOccurrenceNumber        Maximum number of occurrences
 * @property isWeekdaysSelected         True if weekday option is selected. False, if not.
 * @property isValidRecurrence          True if the custom recurrence is valid. False, if not.
 */
data class CustomRecurrenceState constructor(
    val rules: ChatScheduledRules = ChatScheduledRules(),
    val dropdownOccurrenceType: DropdownOccurrenceType? = null,
    val maxOccurrenceNumber: Int? = null,
    val isWeekdaysSelected: Boolean = false,
    val isValidRecurrence: Boolean = true
)
