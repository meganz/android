package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import javax.inject.Inject


/**
 * Week f month Mapper
 */
internal class MegaWeekOfMonthMapper @Inject constructor() {
    operator fun invoke(numberOfWeek: WeekOfMonth): Long =
        when (numberOfWeek) {
            WeekOfMonth.First -> 1
            WeekOfMonth.Second -> 2
            WeekOfMonth.Third -> 3
            WeekOfMonth.Fourth -> 4
            WeekOfMonth.Fifth -> 5
        }
}