package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.domain.entity.chat.ScheduledMeetingItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Check if the start day of a scheduled meeting is today
 *
 * @return True, if it's today. False, if not.
 */
fun ScheduledMeetingItem.isToday(): Boolean {
    this.getZoneStartTime()?.let {
        return it.toLocalDate() == LocalDate.now(ZoneId.systemDefault())
    }

    return false
}

/**
 * Check if the start day of a scheduled meeting is tomorrow
 *
 * @return True, if it's tomorrow. False, if not.
 */
fun ScheduledMeetingItem.isTomorrow(): Boolean {
    this.getZoneStartTime()?.let {
        return it.toLocalDate() == LocalDate.now(ZoneId.systemDefault()).plusDays(1)
    }

    return false
}

/**
 * Get start time formatted
 *
 * @param is24HourFormat True, if it's 24 hour format.
 * @return Text of start time.
 */
fun ScheduledMeetingItem.getStartTime(is24HourFormat: Boolean): String {
    this.startDateTime?.let {
        return getHourFormatter(Instant.ofEpochSecond(it), is24HourFormat).format(it.parseDate())
    }

    return ""
}

/**
 * Get end time formatted
 *
 * @param is24HourFormat True, if it's 24 hour format.
 * @return Text of end time.
 */
fun ScheduledMeetingItem.getEndTime(is24HourFormat: Boolean): String {
    this.endDateTime?.let {
        return getHourFormatter(Instant.ofEpochSecond(it), is24HourFormat).format(it.parseDate())
    }

    return ""
}

/**
 * Get start date with weekday
 *
 * @return Text of start date
 */
fun ScheduledMeetingItem.getCompleteStartDate(): String {
    val dateFormatter = getCompleteDateFormatter()

    this.startDateTime?.let { start ->
        return dateFormatter.format(start.parseDate())
    }

    return ""
}

/**
 * Get start date without weekday
 *
 * @return Text of start date
 */
fun ScheduledMeetingItem.getStartDate(): String {
    val dateFormatter = getDateFormatter()

    this.startDateTime?.let { start ->
        return dateFormatter.format(start.parseDate())
    }

    return ""
}

/**
 * Get end date without weekday
 *
 * @return Text of end date
 */
fun ScheduledMeetingItem.getEndDate(): String {
    val dateFormatter = getDateFormatter()

    this.rules?.let {
        getUntilDate(until = it.until)?.let { until ->
            return until
        }
    }

    this.endDateTime?.let { end ->
        return dateFormatter.format(end.parseDate())
    }

    return ""
}

/**
 * Get ZoneDateTime of start time
 *
 * @return [ZonedDateTime]
 */
fun ScheduledMeetingItem.getZoneStartTime(): ZonedDateTime? = startDateTime?.parseDate()

/**
 * True, the scheduled meeting has passed. False, otherwise.
 */
fun ScheduledMeetingItem.isPast(): Boolean =
    ZonedDateTime.now()
        .withZoneSameInstant(ZoneOffset.UTC)
        .isAfter(endDateTime?.parseDate())

private fun Long.parseDate(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)

private fun getUntilDate(until: Long): String? {
    val dateFormatter = getDateFormatter()
    if (until != 0L) {
        return dateFormatter.format(until.parseDate())
    }

    return null
}

private fun getHourFormatter(instant: Instant, is24HourFormat: Boolean): String =
    DateTimeFormatter
        .ofPattern(if (is24HourFormat) "HH:mm" else "hh:mma")
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun getCompleteDateFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("EEEE',' d MMM yyyy")
        .withZone(ZoneId.systemDefault())

private fun getDateFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("d MMM yyyy")
        .withZone(ZoneId.systemDefault())