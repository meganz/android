package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.domain.entity.meeting.Weekday
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Mapper to convert DayOfWeek to [Weekday]
 */
class WeekDayMapper @Inject constructor() {
    internal operator fun invoke(day: DayOfWeek): Weekday = when (day) {
        DayOfWeek.MONDAY -> Weekday.Monday
        DayOfWeek.TUESDAY -> Weekday.Tuesday
        DayOfWeek.WEDNESDAY -> Weekday.Wednesday
        DayOfWeek.THURSDAY -> Weekday.Thursday
        DayOfWeek.FRIDAY -> Weekday.Friday
        DayOfWeek.SATURDAY -> Weekday.Saturday
        else -> Weekday.Sunday
    }
}