package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.meeting.MonthWeekDayItem
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import nz.mega.sdk.MegaChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledRules
import nz.mega.sdk.MegaIntegerList
import javax.inject.Inject

/**
 * Chat scheduled meeting mapper implementation
 */
internal class ChatScheduledMeetingMapperImpl @Inject constructor() : ChatScheduledMeetingMapper {

    override fun invoke(megaChatScheduledMeeting: MegaChatScheduledMeeting): ChatScheduledMeeting =
        ChatScheduledMeeting(
            megaChatScheduledMeeting.chatId(),
            megaChatScheduledMeeting.schedId(),
            megaChatScheduledMeeting.parentSchedId(),
            megaChatScheduledMeeting.organizerUserId(),
            megaChatScheduledMeeting.timezone(),
            megaChatScheduledMeeting.startDateTime(),
            megaChatScheduledMeeting.endDateTime(),
            megaChatScheduledMeeting.title(),
            megaChatScheduledMeeting.description(),
            megaChatScheduledMeeting.attributes(),
            megaChatScheduledMeeting.overrides(),
            megaChatScheduledMeeting.flags()?.mapFlags(),
            megaChatScheduledMeeting.rules()?.mapRules(),
            megaChatScheduledMeeting.mapChanges(),
            megaChatScheduledMeeting.isCanceled()
        )

    private fun MegaChatScheduledFlags.mapFlags(): ChatScheduledFlags =
        ChatScheduledFlags(emailsDisabled(), isEmpty)

    private fun MegaChatScheduledRules.mapRules(): ChatScheduledRules {
        val freq = mapToOccurrenceFreq(freq())
        var weekDayList: List<Weekday>? = null
        this.byWeekDay()?.let {
            weekDayList = it.mapToHandleWeekDaysList()
        }

        var monthDayList: List<Int>? = null
        this.byMonthDay()?.let {
            monthDayList = it.mapToHandleList()
        }

        val monthWeekDayList = mutableListOf<MonthWeekDayItem>()
        this.byMonthWeekDay()?.let { map ->
            map.keys?.let { keys ->
                for (i in 0 until keys.size()) {
                    keys.get(i).let { key ->
                        key.mapToWeekOfMonth()?.let { weekOfMonth ->
                            val values: MegaIntegerList = map.get(key)
                            monthWeekDayList.add(
                                MonthWeekDayItem(
                                    weekOfMonth = weekOfMonth,
                                    weekDaysList = values.mapToHandleWeekDaysList()
                                )
                            )
                        }
                    }
                }
            }
        }

        return ChatScheduledRules(
            freq = freq,
            interval = interval(),
            until = until(),
            weekDayList = weekDayList,
            monthDayList = monthDayList,
            monthWeekDayList = monthWeekDayList
        )
    }

    private fun MegaIntegerList.mapToHandleList(): List<Int> {
        val list = mutableListOf<Int>()
        for (i in 0 until size()) {
            val value = this@mapToHandleList.get(i).toInt()
            list.add(value)
        }

        return list
    }

    private fun Long.mapToWeekOfMonth(): WeekOfMonth? =
        when (this.toInt()) {
            1 -> WeekOfMonth.First
            2 -> WeekOfMonth.Second
            3 -> WeekOfMonth.Third
            4 -> WeekOfMonth.Fourth
            5 -> WeekOfMonth.Fifth
            else -> null
        }

    private fun MegaIntegerList.mapToHandleWeekDaysList(): List<Weekday> {
        val list = mutableListOf<Weekday>()
        for (i in 0 until size()) {
            val weekDay: Weekday? = when (get(i).toInt()) {
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

    private fun mapToOccurrenceFreq(freq: Int): OccurrenceFrequencyType =
        when (freq) {
            MegaChatScheduledRules.FREQ_DAILY -> OccurrenceFrequencyType.Daily
            MegaChatScheduledRules.FREQ_WEEKLY -> OccurrenceFrequencyType.Weekly
            MegaChatScheduledRules.FREQ_MONTHLY -> OccurrenceFrequencyType.Monthly
            else -> OccurrenceFrequencyType.Invalid
        }

    private fun MegaChatScheduledMeeting.mapChanges(): ScheduledMeetingChanges = when {
        hasChanged(MegaChatScheduledMeeting.SC_NEW_SCHED.toLong()) -> ScheduledMeetingChanges.NewScheduledMeeting
        hasChanged(MegaChatScheduledMeeting.SC_PARENT.toLong()) -> ScheduledMeetingChanges.ParentScheduledMeetingId
        hasChanged(MegaChatScheduledMeeting.SC_TZONE.toLong()) -> ScheduledMeetingChanges.TimeZone
        hasChanged(MegaChatScheduledMeeting.SC_START.toLong()) -> ScheduledMeetingChanges.StartDate
        hasChanged(MegaChatScheduledMeeting.SC_END.toLong()) -> ScheduledMeetingChanges.EndDate
        hasChanged(MegaChatScheduledMeeting.SC_TITLE.toLong()) -> ScheduledMeetingChanges.Title
        hasChanged(MegaChatScheduledMeeting.SC_DESC.toLong()) -> ScheduledMeetingChanges.Description
        hasChanged(MegaChatScheduledMeeting.SC_ATTR.toLong()) -> ScheduledMeetingChanges.Attributes
        hasChanged(MegaChatScheduledMeeting.SC_OVERR.toLong()) -> ScheduledMeetingChanges.OverrideDateTime
        hasChanged(MegaChatScheduledMeeting.SC_CANC.toLong()) -> ScheduledMeetingChanges.CancelledFlag
        hasChanged(MegaChatScheduledMeeting.SC_FLAGS.toLong()) -> ScheduledMeetingChanges.ScheduledMeetingsFlags
        hasChanged(MegaChatScheduledMeeting.SC_RULES.toLong()) -> ScheduledMeetingChanges.RepetitionRules
        else -> ScheduledMeetingChanges.ScheduledMeetingFlagsSize
    }

    private fun MegaChatScheduledMeeting.isCanceled(): Boolean =
        cancelled() != null && cancelled() > 0

}
