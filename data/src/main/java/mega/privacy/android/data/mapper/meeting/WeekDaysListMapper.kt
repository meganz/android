package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.Weekday
import nz.mega.sdk.MegaIntegerList
import javax.inject.Inject

/**
 * Mapper to convert [MegaIntegerList] to list of weekdays
 *
 * @property weekDayMapper  [WeekDayMapper]
 */
internal class WeekDaysListMapper @Inject constructor(private val weekDayMapper: WeekDayMapper) {
    operator fun invoke(integerList: MegaIntegerList): List<Weekday> {
        val list = mutableListOf<Weekday>()
        for (i in 0 until integerList.size()) {
            list.add(weekDayMapper(integerList.get(i).toInt()))
        }

        return list
    }
}