package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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

/**
 * Get the appropriate day and time string for a scheduled meeting.
 *
 * @param scheduledMeeting  [ChatScheduledMeeting]
 * @param is24HourFormat    True, if it's 24 hour format. False, if not.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun getRecurringMeetingDateTime(
    scheduledMeeting: ChatScheduledMeeting,
    is24HourFormat: Boolean,
): String {
    val rules = scheduledMeeting.rules
    val startTime = scheduledMeeting.getStartTime(is24HourFormat)
    val endTime = scheduledMeeting.getEndTime(is24HourFormat)
    val startDate = scheduledMeeting.getStartDate()
    val endDate = scheduledMeeting.getEndDate()

    if (rules == null) {
        return getTextForOneOffMeeting(scheduledMeeting, startTime, endTime)
    }

    when (rules.freq) {
        OccurrenceFrequencyType.Invalid -> {
            return getTextForOneOffMeeting(scheduledMeeting, startTime, endTime)
        }
        OccurrenceFrequencyType.Daily -> return when {
            scheduledMeeting.isForever() -> stringResource(
                id = R.string.notification_subtitle_scheduled_meeting_recurring_daily_forever,
                startDate,
                startTime,
                endTime
            )
            else -> stringResource(
                id = R.string.notification_subtitle_scheduled_meeting_recurring_daily_until,
                startDate,
                endDate,
                startTime,
                endTime
            )
        }
        OccurrenceFrequencyType.Weekly -> {
            rules.weekDayList?.takeIf { it.isNotEmpty() }?.let { weekDaysList ->
                val interval = scheduledMeeting.getIntervalValue()
                when (weekDaysList.size) {
                    1 -> {
                        val weekDay = getWeekDay(weekDaysList.first())
                        return when {
                            scheduledMeeting.isForever() -> pluralStringResource(
                                R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_one_day_forever,
                                interval,
                                weekDay,
                                interval,
                                startDate,
                                startTime,
                                endTime
                            )
                            else -> pluralStringResource(
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
                        var weekdayStringList: String
                        mutableListOf<String>().apply {
                            weekDaysList.sorted().forEach { day ->
                                val index = weekDaysList.indexOf(day)
                                val lastPos = weekDaysList.size - 1
                                if (index != lastPos) {
                                    this@apply.add(getWeekDay(day))
                                }
                            }
                            val separator = ", "
                            weekdayStringList = this@apply.joinToString(separator)
                        }
                        val lastWeekDay = getWeekDay(weekDaysList.last())
                        return when {
                            scheduledMeeting.isForever() ->
                                pluralStringResource(
                                    R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_several_days_forever,
                                    interval,
                                    weekdayStringList,
                                    lastWeekDay,
                                    interval,
                                    startDate,
                                    startTime,
                                    endTime
                                )
                            else -> pluralStringResource(
                                R.plurals.notification_subtitle_scheduled_meeting_recurring_weekly_several_days_until,
                                interval,
                                weekdayStringList,
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
                return when {
                    scheduledMeeting.isForever() -> pluralStringResource(
                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_single_day_forever,
                        interval,
                        dayOfTheMonth,
                        interval,
                        startDate,
                        startTime,
                        endTime
                    )
                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_monday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_tuesday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_wednesday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_thursday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_friday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_saturday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_first,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_second,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_third,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_fourth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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
                                return when {
                                    scheduledMeeting.isForever() -> pluralStringResource(
                                        R.plurals.notification_subtitle_scheduled_meeting_recurring_monthly_ordinal_day_forever_sunday_fifth,
                                        interval,
                                        interval,
                                        startDate,
                                        startTime,
                                        endTime
                                    )
                                    else -> pluralStringResource(
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

    return ""
}

/**
 * Get Text of subtitle for one off meeting
 *
 * @param scheduledMeeting      [ChatScheduledMeeting]
 * @param startTime             Start time
 * @param endTime               End time
 * @return                      Text of one off meeting
 */
@Composable
private fun getTextForOneOffMeeting(
    scheduledMeeting: ChatScheduledMeeting,
    startTime: String,
    endTime: String,
): String =
    stringResource(
        id = when {
            scheduledMeeting.isToday() -> R.string.meetings_one_off_occurrence_info_today
            scheduledMeeting.isTomorrow() -> R.string.meetings_one_off_occurrence_info_tomorrow
            else -> R.string.notification_subtitle_scheduled_meeting_one_off
        },
        scheduledMeeting.getCompleteStartDate(),
        startTime,
        endTime
    )

/**
 * Get the string corresponding to the day of the week
 *
 * @param day [Weekday]
 * @return String of day of the week
 */
@Composable
private fun getWeekDay(day: Weekday): String =
    when (day) {
        Weekday.Monday -> stringResource(id = R.string.notification_scheduled_meeting_week_day_mon)
        Weekday.Tuesday -> stringResource(id = R.string.notification_scheduled_meeting_week_day_tue)
        Weekday.Wednesday -> stringResource(id = R.string.notification_scheduled_meeting_week_day_wed)
        Weekday.Thursday -> stringResource(id = R.string.notification_scheduled_meeting_week_day_thu)
        Weekday.Friday -> stringResource(id = R.string.notification_scheduled_meeting_week_day_fri)
        Weekday.Saturday -> stringResource(id = R.string.notification_scheduled_meeting_week_day_sat)
        Weekday.Sunday -> stringResource(id = R.string.notification_scheduled_meeting_week_day_sun)
    }