package mega.privacy.android.app.utils

import android.content.Context
import android.text.format.DateFormat.getBestDateTimePattern
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING
import mega.privacy.android.app.utils.TimeUtils.DATE
import mega.privacy.android.app.utils.TimeUtils.DATE_LONG_FORMAT
import mega.privacy.android.app.utils.TimeUtils.TIME
import mega.privacy.android.app.utils.TimeUtils.lastGreenDate
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Time utils
 */
object TimeUtils {

    /**
     * Date Long Format
     */
    const val DATE_LONG_FORMAT: Int = 0

    /**
     * Date Short Format
     */
    const val DATE_SHORT_FORMAT: Int = 1

    /**
     * Date MM DD YYYY Format
     */
    const val DATE_MM_DD_YYYY_FORMAT: Int = 3

    private const val TIME: Int = 0
    private const val DATE: Int = TIME + 1
    private const val TIME_OF_CHANGE: Int = 8
    private const val INITIAL_PERIOD_TIME: Int = 0

    /**
     * Comparator for [Calendar] instances that can compare either by time or by date.
     *
     * @property type The comparison mode. Either [TIME] (compares hour and considers calendars
     * equal when within 3 minutes of each other) or [DATE] (compares year, month and day).
     */
    private class CalendarComparator(private val type: Int) : Comparator<Calendar> {

        /**
         * Calculates the absolute difference in days between two [Calendar] instances.
         *
         * @param c1 First calendar.
         * @param c2 Second calendar.
         * @return The number of days between the two calendars.
         */
        fun calculateDifferenceDays(c1: Calendar, c2: Calendar): Long {
            val diff = abs(c1.timeInMillis - c2.timeInMillis)
            return diff / (24 * 60 * 60 * 1000)
        }

        /**
         * Compare
         *
         * @param c1
         * @param c2
         */
        override fun compare(c1: Calendar, c2: Calendar): Int {
            if (type == TIME) {
                return if (c1.get(Calendar.HOUR) != c2.get(Calendar.HOUR)) {
                    c1.get(Calendar.HOUR) - c2.get(Calendar.HOUR)
                } else {
                    val milliseconds1 = c1.timeInMillis
                    val milliseconds2 = c2.timeInMillis

                    val diff = milliseconds2 - milliseconds1
                    val diffMinutes = abs(diff / (60 * 1000))

                    if (diffMinutes < 3) 0 else 1
                }
            } else if (type == DATE) {
                if (c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR))
                    return c1.get(Calendar.YEAR) - c2.get(Calendar.YEAR)
                if (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH))
                    return c1.get(Calendar.MONTH) - c2.get(Calendar.MONTH)
                return c1.get(Calendar.DAY_OF_MONTH) - c2.get(Calendar.DAY_OF_MONTH)
            }
            return -1
        }
    }


    /**
     * Gets a date formatted string from a timestamp.
     *
     * @param timestamp Timestamp in seconds to get the date formatted string.
     * @param format    Date format.
     * @param humanized Use humanized date format (i.e. today, yesterday or week day).
     * @param context   Context used to retrieve localized strings.
     * @return The date formatted string.
     */
    @JvmStatic
    @JvmOverloads
    fun formatDate(
        timestamp: Long,
        format: Int,
        humanized: Boolean = true,
        context: Context,
    ): String {
        val timestampDateTime = ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp),
            ZoneOffset.UTC
        )

        val dateTimeFormatter: DateTimeFormatter = when (format) {
            DATE_SHORT_FORMAT -> DateTimeFormatter.ofPattern(
                getBestDateTimePattern(getUserLocale(), "EEE d MMM")
            )

            DATE_MM_DD_YYYY_FORMAT -> DateTimeFormatter.ofPattern(
                getBestDateTimePattern(getUserLocale(), "MMM d, yyyy")
            )

            DATE_LONG_FORMAT -> DateTimeFormatter.ofLocalizedDateTime(
                FormatStyle.LONG,
                FormatStyle.SHORT
            )

            else -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
        }

        if (humanized) {
            val todayDate = LocalDate.now(ZoneId.systemDefault())
            val timestampDate = timestampDateTime
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDate()

            // Check if date is today, yesterday, tomorrow or less of a week
            if (timestampDate.equals(todayDate)) {
                return context.getString(R.string.label_today)
            } else if (timestampDate == todayDate.minusDays(1)) {
                return context.getString(R.string.label_yesterday)
            } else if (timestampDate == todayDate.plusDays(1)) {
                val tomorrowFormat = DateTimeFormatter.ofPattern(
                    getBestDateTimePattern(getUserLocale(), "d MMM yyyy")
                ).withZone(ZoneId.systemDefault())
                return context.getString(
                    R.string.tomorrow_date,
                    tomorrowFormat.format(timestampDateTime)
                )
            } else if (timestampDate.isBefore(
                    todayDate.plusWeeks(
                        1
                    )
                )
            ) {
                val futureFormat = DateTimeFormatter.ofPattern(
                    getBestDateTimePattern(getUserLocale(), "EEEE, d MMM yyyy")
                ).withZone(ZoneId.systemDefault())
                return futureFormat.format(timestampDateTime)
            }
        }

        return dateTimeFormatter.withZone(ZoneId.systemDefault())
            .withLocale(getUserLocale())
            .format(timestampDateTime)
    }

    /**
     * Formats a timestamp as a full date and time string ("d MMM yyyy HH:mm").
     *
     * @param timestamp Timestamp in seconds.
     * @return The formatted date and time string.
     */
    @JvmStatic
    fun formatLongDateTime(timestamp: Long): String {
        val df: DateFormat = SimpleDateFormat(
            getBestDateTimePattern(getUserLocale(), "d MMM yyyy HH:mm"),
            getUserLocale()
        )
        val cal = Util.calculateDateFromTimestamp(timestamp)
        val date = cal.time
        return df.format(date)
    }

    /**
     * Formats a timestamp as a short, locale-aware time string.
     *
     * @param ts Timestamp in seconds.
     * @return The formatted time string (e.g. "10:23 AM").
     */
    @JvmStatic
    fun formatTime(ts: Long): String {
        val df: DateFormat = SimpleDateFormat
            .getTimeInstance(SimpleDateFormat.SHORT, getUserLocale())
        val cal = Util.calculateDateFromTimestamp(ts)
        val tz = cal.timeZone
        df.timeZone = tz
        val date = cal.time
        return df.format(date)
    }

    /**
     * Builds a localized "last seen" string for a contact based on how many minutes ago they
     * were last online. The result is formatted as one of:
     * - "Last seen a long time ago" when the user was seen more than 65535 minutes ago.
     * - "Last seen today at HH:mm" when the last green time is on the current day.
     * - "Last seen day at HH:mm" otherwise.
     *
     * @param context Context used to retrieve localized strings.
     * @param minutesAgo Minutes elapsed since the user was last online.
     * @return The formatted last seen string.
     */
    @JvmStatic
    fun lastGreenDate(context: Context, minutesAgo: Int): String {
        val calGreen = Calendar.getInstance()
        calGreen.add(Calendar.MINUTE, -minutesAgo)

        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance()
        calYesterday.add(Calendar.DATE, -1)
        val tc = CalendarComparator(DATE)
        val ts = calGreen.timeInMillis
        Timber.d("Ts last green: %s", ts)
        return if (minutesAgo >= 65535) {
            context.getString(R.string.last_seen_long_time_ago)
        } else if (tc.compare(calGreen, calToday) == 0) {
            val tz = calGreen.timeZone

            val df: DateFormat = SimpleDateFormat("HH:mm", getUserLocale())
            df.timeZone = tz

            val time = df.format(calGreen.time)

            context.getString(R.string.last_seen_today, time)
        } else {
            val tz = calGreen.timeZone

            var df: DateFormat = SimpleDateFormat("HH:mm", getUserLocale())
            df.timeZone = tz

            val time = df.format(calGreen.time)

            df = SimpleDateFormat(
                getBestDateTimePattern(getUserLocale(), "dd MMM"),
                getUserLocale()
            )
            val day = df.format(calGreen.time)

            context.getString(R.string.last_seen_general, day, time)
        }
    }

    /**
     * Returns the same value as [lastGreenDate] with the `\[A]` and `[/A]` styling tags stripped.
     *
     * @param context Context used to retrieve localized strings.
     * @param minutesAgo Minutes elapsed since the user was last online.
     * @return The unstyled last seen string.
     */
    @JvmStatic
    fun unformattedLastGreenDate(context: Context, minutesAgo: Int): String {
        return lastGreenDate(context, minutesAgo)
            .replace("[A]", "")
            .replace("[/A]", "")
    }

    /**
     * Formats a timestamp as a humanized date and time string.
     *
     * Renders the date as "Today", "Yesterday", a day of the week (when within a week) or a
     * locale-specific full date, followed by the formatted time.
     *
     * @param context Context used to retrieve localized strings.
     * @param ts Timestamp in seconds.
     * @param format Either [DATE_LONG_FORMAT] (date and time) or any other value (date only).
     * @return The formatted date and time string.
     */
    @JvmStatic
    fun formatDateAndTime(context: Context, ts: Long, format: Int): String {
        val df: DateFormat = if (format == DATE_LONG_FORMAT) {
            SimpleDateFormat.getDateTimeInstance(
                SimpleDateFormat.LONG,
                SimpleDateFormat.SHORT,
                getUserLocale()
            )
        } else {
            SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, getUserLocale())
        }

        val cal = Util.calculateDateFromTimestamp(ts)

        //Compare to yesterday
        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance()
        calYesterday.add(Calendar.DATE, -1)
        val tc = CalendarComparator(DATE)
        return if (tc.compare(cal, calToday) == 0) {
            val time = formatTime(ts)
            context.getString(R.string.label_today) + " " + time
        } else if (tc.compare(cal, calYesterday) == 0) {
            val time = formatTime(ts)
            context.getString(R.string.label_yesterday) + " " + time
        } else {
            if (tc.calculateDifferenceDays(cal, calToday) < 7) {
                val date = cal.time
                val dayWeek = SimpleDateFormat(
                    getBestDateTimePattern(getUserLocale(), "EEEE"),
                    getUserLocale()
                ).format(date)
                val time = formatTime(ts)
                "$dayWeek $time"
            } else {
                val tz = cal.timeZone
                df.timeZone = tz
                val date = cal.time
                df.format(date)
            }
        }
    }

    /**
     * Formats a timestamp as a default locale date and time string.
     *
     * @param date Timestamp in seconds.
     * @return The formatted date and time string.
     */
    @JvmStatic
    fun getDateString(date: Long): String =
        DateFormat.getDateTimeInstance().format(Date(date * 1000))

    /**
     * Formats a timestamp as a bucket header label.
     *
     * Returns "Today" or "Yesterday" when applicable; otherwise the full date in the
     * locale-specific "EEEE, d MMM yyyy" pattern.
     *
     * @param ts Timestamp in seconds.
     * @param context Context used to retrieve localized strings.
     * @return The formatted bucket date string.
     */
    @JvmStatic
    fun formatBucketDate(ts: Long, context: Context): String {
        val cal = Util.calculateDateFromTimestamp(ts)
        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance()
        calYesterday.add(Calendar.DATE, -1)
        val tc = CalendarComparator(DATE)

        return if (tc.compare(cal, calToday) == 0) {
            context.getString(R.string.label_today)
        } else if (tc.compare(cal, calYesterday) == 0) {
            context.getString(R.string.label_yesterday)
        } else {
            val date = cal.time
            SimpleDateFormat(
                getBestDateTimePattern(
                    getUserLocale(),
                    "EEEE, d MMM yyyy"
                ),
                getUserLocale()
            ).format(date)
        }
    }

    /**
     * Formats a "recently watched" date relative to the current day.
     *
     * Returns "Today" or "Yesterday" when applicable; otherwise the locale-specific
     * "EEE, dd MMM yyyy" pattern.
     *
     * @param days Number of days since the epoch.
     * @param context Context used to retrieve localized strings.
     * @return The formatted recently watched date string.
     */
    @JvmStatic
    fun formatRecentlyWatchedDate(days: Long, context: Context): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = TimeUnit.DAYS.toMillis(days)
        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance()
        calYesterday.add(Calendar.DATE, -1)
        val tc = CalendarComparator(DATE)

        return if (tc.compare(cal, calToday) == 0) {
            context.getString(R.string.label_today)
        } else if (tc.compare(cal, calYesterday) == 0) {
            context.getString(R.string.label_yesterday)
        } else {
            val date = cal.time
            SimpleDateFormat(
                getBestDateTimePattern(getUserLocale(), "EEE, dd MMM yyyy"),
                getUserLocale()
            ).format(date)
        }
    }

    /**
     * Get minutes and seconds from milliseconds
     *
     * @param milliseconds Time in milliseconds
     * @return Time in minutes and seconds
     */
    @JvmStatic
    fun getMinutesAndSecondsFromMilliseconds(milliseconds: Long): String {
        return SimpleDateFormat("mm:ss", getUserLocale())
            .format(Date(milliseconds))
    }

    /**
     * Gets video duration time from a duration received.
     *
     * @param duration Duration in seconds.
     * @return The time string.
     */
    @JvmStatic
    @Deprecated("Use DurationInSecondsTextMapper instead.")
    fun getVideoDuration(duration: Int): String {
        if (duration > 0) {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            val seconds = duration % 60

            return if (hours > 0) {
                String.format(getUserLocale(), "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(getUserLocale(), "%d:%02d", minutes, seconds)
            }
        }

        return ""
    }

    /**
     * Method for obtaining the appropriate String depending on the current time.
     *
     * @param option Selected mute type.
     * @param context Context used to retrieve localized strings.
     * @return The right string.
     */
    @JvmStatic
    fun getCorrectStringDependingOnCalendar(option: String, context: Context): String {
        val calendar = getCalendarSpecificTime(option)
        val tz = calendar.timeZone

        val df: DateFormat = SimpleDateFormat(
            getBestDateTimePattern(getUserLocale(), "HH:mm"),
            getUserLocale()
        )
        df.timeZone = tz

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val time = df.format(calendar.time)

        return if (option == NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING)
            context.resources.getQuantityString(
                R.plurals.success_muting_chat_until_specific_time,
                hour,
                time
            )
        else
            context.resources.getQuantityString(
                R.plurals.success_muting_chat_until_specific_date_and_time,
                hour,
                context.getString(R.string.label_tomorrow).lowercase(),
                time
            )
    }

    /**
     * Method for obtaining the appropriate String depending on the option selected.
     *
     * @param timestamp The time in minutes that notifications of a chat or all chats are muted.
     * @param context Context used to retrieve localized strings.
     * @return The right string
     */
    @JvmStatic
    fun getCorrectStringDependingOnOptionSelected(timestamp: Long, context: Context): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp * 1000

        val calToday = Calendar.getInstance()
        calToday.timeInMillis = System.currentTimeMillis()

        val calTomorrow = Calendar.getInstance()
        calTomorrow.add(Calendar.DATE, +1)

        val df: DateFormat =
            SimpleDateFormat(getBestDateTimePattern(getUserLocale(), "HH:mm"), getUserLocale())

        val tz = cal.timeZone
        df.timeZone = tz

        return context.resources.getQuantityString(
            R.plurals.chat_notifications_muted_until_specific_time,
            cal.get(Calendar.HOUR_OF_DAY),
            df.format(cal.time)
        )
    }

    private fun getUserLocale(): Locale = Locale.getDefault()

    /**
     * Method for obtaining a calendar depending on the type of silencing chosen.
     *
     * @param option Selected mute type.
     * @return The Calendar.
     */
    @JvmStatic
    fun getCalendarSpecificTime(option: String): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.HOUR, TIME_OF_CHANGE)
        calendar.set(Calendar.AM_PM, Calendar.AM)

        if (option == NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar
    }

    /**
     * Method to know if the silencing should be until this morning.
     *
     * @return True if it is. False it is not.
     */
    @JvmStatic
    fun isUntilThisMorning(): Boolean {
        val cal = Calendar.getInstance()
        cal.time = Date()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return hour < TIME_OF_CHANGE || (hour == TIME_OF_CHANGE && minute == INITIAL_PERIOD_TIME)
    }

    /**
     * Converts seconds time into a humanized format string.
     * - If time is greater than a DAY, the formatted string will be "X day(s)".
     * - If time is lower than a DAY and greater than an HOUR, the formatted string will be "Xh Ym".
     * - If time is lower than an HOUR and greater than a MINUTE, the formatted string will be "Xm Ys".
     * - If time is lower than a MINUTE, the formatted string will be "Xs".
     *
     * @param time Time in seconds to get the formatted string.
     * @return The humanized format string.
     */
    @JvmStatic
    fun getHumanizedTime(time: Long): String {
        val context = MegaApplication.getInstance().applicationContext
        if (time <= 0) {
            return context.getString(R.string.label_time_in_seconds, 0)
        }

        val days = TimeUnit.SECONDS.toDays(time)
        val hours = TimeUnit.SECONDS.toHours(time) - TimeUnit.DAYS.toHours(days)
        val minutes =
            TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.DAYS.toMinutes(days) + TimeUnit.HOURS.toMinutes(
                hours
            ))
        val seconds =
            TimeUnit.SECONDS.toSeconds(time) - (TimeUnit.DAYS.toSeconds(days) + TimeUnit.HOURS.toSeconds(
                hours
            ) + TimeUnit.MINUTES.toSeconds(minutes))

        return if (days > 0) {
            context.resources.getQuantityString(
                R.plurals.label_time_in_days_full,
                days.toInt(),
                days.toInt()
            )
        } else if (hours > 0) {
            context.getString(R.string.label_time_in_hours, hours) + " " +
                    context.getString(R.string.label_time_in_minutes, minutes)
        } else if (minutes > 0) {
            context.getString(R.string.label_time_in_minutes, minutes) + " " +
                    context.getString(R.string.label_time_in_seconds, seconds)
        } else {
            context.getString(R.string.label_time_in_seconds, seconds)
        }
    }

    /**
     * Converts milliseconds time into a humanized format string.
     * - If time is greater than a DAY, the formatted string will be "X day(s)".
     * - If time is lower than a DAY and greater than an HOUR, the formatted string will be "Xh Ym".
     * - If time is lower than an HOUR and greater than a MINUTE, the formatted string will be "Xm Ys".
     * - If time is lower than a MINUTE, the formatted string will be "Xs".
     *
     * @param time Time in milliseconds to get the formatted string.
     * @return The humanized format string.
     */
    @JvmStatic
    fun getHumanizedTimeMs(time: Long): String {
        return getHumanizedTime(TimeUnit.MILLISECONDS.toSeconds(time))
    }

}
