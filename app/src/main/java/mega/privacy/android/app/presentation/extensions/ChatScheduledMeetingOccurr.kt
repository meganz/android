package mega.privacy.android.app.presentation.extensions

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
 * Get time formatted
 */
fun ChatScheduledMeetingOccurr.getTimeFormatted(is24HourFormat: Boolean): String? {
    val startTimestamp = startDateTime ?: return null
    val endTimestamp = endDateTime ?: return null
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