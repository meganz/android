package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import nz.mega.sdk.MegaIntegerMap
import javax.inject.Inject


/**
 * Mapper to convert list of [MonthWeekDayItem] to [MegaIntegerMap]
 */
internal class MegaMonthWeekDayListMapper @Inject constructor(
    private val megaIntegerWeekDaysListMapper: MegaIntegerWeekDaysListMapper,
    private val megaWeekOfMonthMapper: MegaWeekOfMonthMapper,
) {
    operator fun invoke(monthWeekDayList: List<MonthWeekDayItem>?): MegaIntegerMap? {
        val map: MegaIntegerMap = MegaIntegerMap.createInstance()
        if (monthWeekDayList.isNullOrEmpty()) return map

        val key = megaWeekOfMonthMapper(monthWeekDayList.first().weekOfMonth)
        val list =
            megaIntegerWeekDaysListMapper(monthWeekDayList.first().weekDaysList) ?: return null
        val value = list.get(0)
        map.set(key, value)
        return map
    }
}