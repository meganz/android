package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import javax.inject.Inject

/**
 * Week f month Mapper
 */
internal class WeekOfMonthMapper @Inject constructor() {
    operator fun invoke(numberOfWeek: Int): WeekOfMonth? =
        when (numberOfWeek) {
            1 -> WeekOfMonth.First
            2 -> WeekOfMonth.Second
            3 -> WeekOfMonth.Third
            4 -> WeekOfMonth.Fourth
            5 -> WeekOfMonth.Fifth
            else -> null
        }
}