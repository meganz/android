package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Get date formatted
 */
fun ChatScheduledMeetingOccurr.getDateFormatted(): String? {
    startDateTime?.let {
        return getDateFormatter().format(it.toZonedDateTime())
    }

    return null
}

/**
 * Get start [ZonedDateTime]
 */
fun ChatScheduledMeetingOccurr.getStartZoneDateTime(): ZonedDateTime? {
    return startDateTime?.let {
        ZonedDateTime.from(
            Instant.ofEpochSecond(it).atZone(ZoneId.of(timezone))
        )
    }
}

/**
 * Get end [ZonedDateTime]
 */
fun ChatScheduledMeetingOccurr.getEndZoneDateTime(): ZonedDateTime? {
    return endDateTime?.let {
        ZonedDateTime.from(
            Instant.ofEpochSecond(it).atZone(ZoneId.of(timezone))
        )
    }
}

/**
 * Get start time formatted
 *
 * @param is24HourFormat True, if it's 24 hour format.
 * @return Text of start time.
 */
fun ChatScheduledMeetingOccurr.getStartTime(is24HourFormat: Boolean): String =
    this.startDateTime?.let { startDate ->
        val hourFormatter = getHourFormatter(is24HourFormat)
        return hourFormatter.format(startDate.parseUTCDate())
    } ?: ""

private fun getHourFormatter(is24HourFormat: Boolean): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern(if (is24HourFormat) "HH:mm" else "hh:mma")
        .withZone(ZoneId.systemDefault())

/**
 * Get end time formatted
 *
 * @param is24HourFormat True, if it's 24 hour format.
 * @return Text of end time.
 */
fun ChatScheduledMeetingOccurr.getEndTime(is24HourFormat: Boolean): String =
    this.endDateTime?.let { endDate ->
        val hourFormatter = getHourFormatter(is24HourFormat)
        return hourFormatter.format(endDate.parseUTCDate())
    } ?: ""

/**
 * Get start date with weekday
 *
 * @return Text of start date
 */
fun ChatScheduledMeetingOccurr.getCompleteStartDate(): String =
    this.startDateTime?.let { start ->
        val dateFormatter = getCompleteDateFormatter()
        return dateFormatter.format(start.parseUTCDate())
    } ?: ""

private fun Long.parseUTCDate(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneId.systemDefault())

private fun getCompleteDateFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("E',' d MMM',' yyyy")
        .withZone(ZoneId.systemDefault())

/**
 * Get day and month formatted
 */
fun ChatScheduledMeetingOccurr.getDayAndMonth(): String? {
    startDateTime?.let {
        return getDayAndMonthFormatter().format(it.toZonedDateTime())
    }

    return null
}

/**
 * Get start time formatted
 */
fun ChatScheduledMeetingOccurr.getStartTimeFormatted(is24HourFormat: Boolean): String {
    val startTimestamp = startDateTime ?: return ""
    return getFormattedHour(Instant.ofEpochSecond(startTimestamp), is24HourFormat)
}

/**
 * Get end time formatted
 */
fun ChatScheduledMeetingOccurr.getEndTimeFormatted(is24HourFormat: Boolean): String {
    val endTimestamp = endDateTime ?: return ""
    return getFormattedHour(Instant.ofEpochSecond(endTimestamp), is24HourFormat)
}

/**
 * Get time formatted
 */
fun ChatScheduledMeetingOccurr.getTimeFormatted(is24HourFormat: Boolean): String {
    val startTimestamp = startDateTime ?: return ""
    val endTimestamp = endDateTime ?: return ""
    val startHour = getFormattedHour(Instant.ofEpochSecond(startTimestamp), is24HourFormat)
    val endHour = getFormattedHour(Instant.ofEpochSecond(endTimestamp), is24HourFormat)
    return "$startHour - $endHour"
}


private fun getFormattedHour(instant: Instant, is24HourFormat: Boolean): String =
    DateTimeFormatter
        .ofPattern(if (is24HourFormat) "HH:mm" else "hh:mm a")
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun Long.toZonedDateTime(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)

private fun getDateFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("EEEE',' d MMMM")
        .withZone(ZoneId.systemDefault())

private fun getDayAndMonthFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("d MMMM")
        .withZone(ZoneId.systemDefault())