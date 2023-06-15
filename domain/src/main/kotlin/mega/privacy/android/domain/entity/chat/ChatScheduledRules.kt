package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.Weekday
import java.io.Serializable

/**
 * Chat scheduled rules
 *
 * @property freq               The frequency of the scheduled meeting [OccurrenceFrequencyType].
 * @property interval           The repetition interval in relation to the frequency.
 * @property until              When the repetitions should end.
 * @property weekDayList        [Weekday] list with the week days when the event will occur.
 * @property monthDayList       Integer list with the days of the month when the event will occur.
 * @property monthWeekDayList   [MonthWeekDayItem] list that allows to specify one or multiple weekday offset.
 */
data class ChatScheduledRules(
    val freq: OccurrenceFrequencyType = OccurrenceFrequencyType.Invalid,
    val interval: Int = 0,
    val until: Long = 0L,
    val weekDayList: List<Weekday>? = null,
    val monthDayList: List<Int>? = null,
    val monthWeekDayList: List<MonthWeekDayItem>? = emptyList(),
) : Serializable
