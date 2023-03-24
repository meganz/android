package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.Weekday
import nz.mega.sdk.MegaIntegerList
import javax.inject.Inject

/**
 * Mapper to convert [MegaIntegerList] to list of weekdays
 */
internal class WeekDaysListMapper @Inject constructor() {
    operator fun invoke(integerList: MegaIntegerList): List<Weekday> {
        val list = mutableListOf<Weekday>()
        for (i in 0 until integerList.size()) {
            val weekDay: Weekday? = when (integerList.get(i).toInt()) {
                1 -> Weekday.Monday
                2 -> Weekday.Tuesday
                3 -> Weekday.Wednesday
                4 -> Weekday.Thursday
                5 -> Weekday.Friday
                6 -> Weekday.Saturday
                7 -> Weekday.Sunday
                else -> null
            }

            weekDay?.let {
                list.add(it)
            }
        }

        return list
    }
}