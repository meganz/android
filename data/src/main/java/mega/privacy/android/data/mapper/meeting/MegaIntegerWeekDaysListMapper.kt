package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.Weekday
import nz.mega.sdk.MegaIntegerList
import javax.inject.Inject

/**
 * Mapper to convert list of [Weekday] to [MegaIntegerList]
 */
internal class MegaIntegerWeekDaysListMapper @Inject constructor() {
    operator fun invoke(weekdayList: List<Weekday>?): MegaIntegerList? {
        if (weekdayList == null) return null

        val list = MegaIntegerList.createInstance()
        weekdayList.forEach { day ->
            val value: Long = when (day) {
                Weekday.Monday -> 1
                Weekday.Tuesday -> 2
                Weekday.Wednesday -> 3
                Weekday.Thursday -> 4
                Weekday.Friday -> 5
                Weekday.Saturday -> 6
                Weekday.Sunday -> 7
            }
            list.add(value)
        }

        return list
    }
}