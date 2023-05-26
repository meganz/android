package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.Weekday
import javax.inject.Inject

/**
 * Mapper to convert Int to [Weekday]
 */
internal class WeekDayMapper @Inject constructor() {
    operator fun invoke(day: Int): Weekday = when (day) {
        1 -> Weekday.Monday
        2 -> Weekday.Tuesday
        3 -> Weekday.Wednesday
        4 -> Weekday.Thursday
        5 -> Weekday.Friday
        6 -> Weekday.Saturday
        else -> Weekday.Sunday
    }
}