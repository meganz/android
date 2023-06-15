package mega.privacy.android.app.utils

import android.content.Context
import androidx.compose.ui.text.buildAnnotatedString
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getCompleteStartDate
import mega.privacy.android.app.presentation.extensions.getEndDate
import mega.privacy.android.app.presentation.extensions.getEndTime
import mega.privacy.android.app.presentation.extensions.getIntervalValue
import mega.privacy.android.app.presentation.extensions.getStartDate
import mega.privacy.android.app.presentation.extensions.getStartTime
import mega.privacy.android.app.presentation.extensions.isForever
import mega.privacy.android.app.presentation.extensions.isToday
import mega.privacy.android.app.presentation.extensions.isTomorrow
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.WeekOfMonth
import mega.privacy.android.domain.entity.meeting.Weekday
import mega.privacy.android.app.presentation.meeting.view.getRecurringMeetingDateTime

/**
 * Temporary object that gets the appropriate string for the date of a scheduled meeting.
 * Should be removed once [ChatActivity] migrates to kotlin. The compose fun [getRecurringMeetingDateTime] must be used.
 */
object ScheduledMeetingDateUtil {

    private fun getWeekDay(context: Context, day: Weekday, isForSentenceStart: Boolean): String =
        when (day) {
            Weekday.Monday -> context.getString(if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_mon else R.string.notification_scheduled_meeting_week_day_sentence_middle_mon)
            Weekday.Tuesday -> context.getString(if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_tue else R.string.notification_scheduled_meeting_week_day_sentence_middle_tue)
            Weekday.Wednesday -> context.getString(if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_wed else R.string.notification_scheduled_meeting_week_day_sentence_middle_wed)
            Weekday.Thursday -> context.getString(if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_thu else R.string.notification_scheduled_meeting_week_day_sentence_middle_thu)
            Weekday.Friday -> context.getString(if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_fri else R.string.notification_scheduled_meeting_week_day_sentence_middle_fri)
            Weekday.Saturday -> context.getString(if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_sat else R.string.notification_scheduled_meeting_week_day_sentence_middle_sat)
            Weekday.Sunday -> context.getString(if (isForSentenceStart) R.string.notification_scheduled_meeting_week_day_sentence_start_sun else R.string.notification_scheduled_meeting_week_day_sentence_middle_sun)
        }

    @JvmStatic
    fun getAppropriateStringForScheduledMeetingDate(
        context: Context,
        is24HourFormat: Boolean,
        scheduledMeeting: ChatScheduledMeeting,
    ): String {
        var result = ""
        val rules = scheduledMeeting.rules
        val startTime = scheduledMeeting.getStartTime(is24HourFormat)
        val endTime = scheduledMeeting.getEndTime(is24HourFormat)
        val startDate = scheduledMeeting.getStartDate()
        val endDate = scheduledMeeting.getEndDate()

        when (rules?.freq) {
            null, OccurrenceFrequencyType.Invalid -> {
                result = context.getString(
                    when {
                        scheduledMeeting.isToday() -> R.string.meetings_one_off_occurrence_info_today
                        scheduledMeeting.isTomorrow() -> R.string.meetings_one_off_occurrence_info_tomorrow
                        else -> R.string.notification_subtitle_scheduled_meeting_one_off
                    },
                    scheduledMeeting.getCompleteStartDate(),
                    startTime,
                    endTime
                )
            }

            OccurrenceFrequencyType.Daily -> {
                val interval = scheduledMeeting.getIntervalValue()
                result = when {
                    interval > 1 ->
                        context.getString(
                            when {
                                scheduledMeeting.isToday() -> R.string.meetings_one_off_occurrence_info_today
                                scheduledMeeting.isTomorrow() -> R.string.meetings_one_off_occurrence_info_tomorrow
                                else -> R.string.notification_subtitle_scheduled_meeting_one_off
                            },
                            scheduledMeeting.getCompleteStartDate(),
                            startTime,
                            endTime
                        )

                    scheduledMeeting.isForever() -> context.getString(
                        R.string.notification_subtitle_scheduled_meeting_recurring_daily_forever,
                        startDate,
                        startTime,
                        endTime
                    )

                    else -> context.getString(
                        R.string.notification_subtitle_scheduled_meeting_recurring_daily_until,
                        startDate,
                        endDate,
                        startTime,
                        endTime
                    )
                }
            }

            OccurrenceFrequencyType.Weekly -> {
                rules.weekDayList?.takeIf { it.isNotEmpty() }?.sortedBy { it.ordinal }
                    ?.let { weekDaysList ->
                        val interval = scheduledMeeting.getIntervalValue()
                        when (weekDaysList.size) {
                            1 -> {
                                val weekDay =
                                    getWeekDay(context, weekDaysList.first(), true)
                                result = when {
                                    scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_one_day_forever,
                                        interval,
                                        weekDay,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> context.resources.getQuantityString(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_one_day_until,
                                        interval,
                                        weekDay,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                            }
                            else -> {
                                val lastWeekDay =
                                    getWeekDay(context, weekDaysList.last(), false)
                                val weekDaysListString = StringBuilder().apply {
                                    weekDaysList.forEachIndexed { index, weekday ->
                                        if (index != weekDaysList.size - 1) {
                                            append(
                                                getWeekDay(
                                                    context,
                                                    weekday,
                                                    index == 0
                                                )
                                            )
                                            if (index != weekDaysList.size - 2) append(", ")
                                        }
                                    }
                                }.toString()
                                result = when {
                                    scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_several_days_forever,
                                        interval,
                                        weekDaysListString,
                                        lastWeekDay,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> context.resources.getQuantityString(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_several_days_until,
                                        interval,
                                        weekDaysListString,
                                        lastWeekDay,
                                        interval,
                                        startDate,
                                        endDate,
                                        startTime,
                                        endTime
                                    )
                                }
                            }
                        }
                    }
            }
            OccurrenceFrequencyType.Monthly -> {
                val interval = scheduledMeeting.getIntervalValue()
                rules.monthDayList?.takeIf { it.isNotEmpty() }?.let { monthDayList ->
                    val dayOfTheMonth = monthDayList.first()
                    result = when {
                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_single_day_forever,
                            interval,
                            dayOfTheMonth,
                            interval,
                            startDate,
                            startTime,
                            endTime
                        )
                        else -> context.resources.getQuantityString(
                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_single_day_until,
                            interval,
                            dayOfTheMonth,
                            interval,
                            startDate,
                            endDate,
                            startTime,
                            endTime
                        )
                    }
                }

                rules.monthWeekDayList?.takeIf { it.isNotEmpty() }?.let { monthWeekDayList ->
                    val monthWeekDayItem = monthWeekDayList.first()
                    val weekOfMonth = monthWeekDayItem.weekOfMonth

                    when (monthWeekDayItem.weekDaysList.first()) {
                        Weekday.Monday -> {
                            when (weekOfMonth) {
                                WeekOfMonth.First ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Second ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Third ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fourth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fifth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_monday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                            }
                        }
                        Weekday.Tuesday -> {
                            when (weekOfMonth) {
                                WeekOfMonth.First ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Second ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Third ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fourth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fifth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_tuesday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                            }
                        }
                        Weekday.Wednesday -> {
                            when (weekOfMonth) {
                                WeekOfMonth.First ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Second ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Third ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fourth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fifth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_wednesday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                            }
                        }
                        Weekday.Thursday -> {
                            when (weekOfMonth) {
                                WeekOfMonth.First ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Second ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Third ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fourth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fifth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_thursday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                            }
                        }
                        Weekday.Friday -> {
                            when (weekOfMonth) {
                                WeekOfMonth.First -> {
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                }
                                WeekOfMonth.Second -> {
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                }
                                WeekOfMonth.Third -> {
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                }
                                WeekOfMonth.Fourth -> {
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                }
                                WeekOfMonth.Fifth -> {
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_friday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                }
                            }
                        }
                        Weekday.Saturday -> {
                            when (weekOfMonth) {
                                WeekOfMonth.First ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Second ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Third ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fourth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fifth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_saturday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                            }
                        }
                        Weekday.Sunday -> {
                            when (weekOfMonth) {
                                WeekOfMonth.First ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_first,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Second ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_second,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Third ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_third,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fourth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_fourth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                                WeekOfMonth.Fifth ->
                                    result = when {
                                        scheduledMeeting.isForever() -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            startTime,
                                            endTime
                                        )
                                        else -> context.resources.getQuantityString(
                                            R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_until_sunday_fifth,
                                            interval,
                                            interval,
                                            startDate,
                                            endDate,
                                            startTime,
                                            endTime
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        }

        return buildAnnotatedString {
            append(
                result
                    .replace("[A]", "").replace("[/A]", "")
                    .replace("[B]", "").replace("[/B]", "")
            )
        }.toString()
    }
}