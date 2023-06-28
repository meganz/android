package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.MonthlyRecurrenceOption

/**
 * Data class defining the custom recurrence state
 *
 * @property newRules                           [ChatScheduledRules]
 * @property isWeekdaysSelected                 True if weekday option is selected. False, if not.
 * @property monthlyRadioButtonOptionSelected   [MonthlyRecurrenceOption]
 * @property isValidRecurrence                  True if the custom recurrence is valid. False, if not.
 */
data class CustomRecurrenceState constructor(
    val newRules: ChatScheduledRules = ChatScheduledRules(),
    val isWeekdaysSelected: Boolean = false,
    val monthlyRadioButtonOptionSelected: MonthlyRecurrenceOption = MonthlyRecurrenceOption.MonthDay,
    val isValidRecurrence: Boolean = true,
)
