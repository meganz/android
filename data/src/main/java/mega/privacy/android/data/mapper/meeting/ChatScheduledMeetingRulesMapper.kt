package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import mega.privacy.android.domain.entity.meeting.Weekday
import nz.mega.sdk.MegaChatScheduledRules
import nz.mega.sdk.MegaIntegerList
import javax.inject.Inject

/**
 * Mapper to convert [MegaChatScheduledRules] to [ChatScheduledRules]
 */
internal class ChatScheduledMeetingRulesMapper @Inject constructor(
    private val integerListMapper: IntegerListMapper,
    private val occurrenceFrequencyTypeMapper: OccurrenceFrequencyTypeMapper,
    private val weekOfMonthMapper: WeekOfMonthMapper,
    private val weekDaysListMapper: WeekDaysListMapper,
) {
    operator fun invoke(rules: MegaChatScheduledRules?): ChatScheduledRules? {
        val megaChatScheduledRules = rules ?: return null
        val freq = occurrenceFrequencyTypeMapper(megaChatScheduledRules.freq())
        var weekDayList: List<Weekday>? = null
        megaChatScheduledRules.byWeekDay()?.let {
            weekDayList = weekDaysListMapper(it)
        }

        var monthDayList: List<Int>? = null
        megaChatScheduledRules.byMonthDay()?.let {
            monthDayList = integerListMapper(it)
        }

        val monthWeekDayList = mutableListOf<MonthWeekDayItem>()
        megaChatScheduledRules.byMonthWeekDay()?.let { map ->
            map.keys?.let { keys ->
                for (i in 0 until keys.size()) {
                    keys.get(i).let { key ->
                        weekOfMonthMapper(key.toInt())?.let { weekOfMonth ->
                            val values: MegaIntegerList = map.get(key)
                            monthWeekDayList.add(
                                MonthWeekDayItem(
                                    weekOfMonth = weekOfMonth,
                                    weekDaysList = weekDaysListMapper(values)
                                )
                            )
                        }
                    }
                }
            }
        }

        return ChatScheduledRules(
            freq = freq,
            interval = megaChatScheduledRules.interval(),
            until = megaChatScheduledRules.until(),
            weekDayList = weekDayList,
            monthDayList = monthDayList,
            monthWeekDayList = monthWeekDayList
        )
    }
}