package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Mapper for meeting last timestamp
 */
typealias MeetingLastTimestampMapper = (
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Boolean,
) -> @JvmSuppressWildcards String

/**
 * Mapper for scheduled meeting timestamp
 */
typealias ScheduledMeetingTimestampMapper = (
    @JvmSuppressWildcards MeetingRoomItem,
    @JvmSuppressWildcards Boolean,
) -> @JvmSuppressWildcards String?

/**
 * Convert meeting timestamp to readable form
 *
 * @param timeStamp Timestamp
 * @return          String formatted
 */
internal fun toLastTimeFormatted(timeStamp: Long, is24HourFormat: Boolean): String {
    val instant = Instant.ofEpochSecond(timeStamp)
    val hourFormatted = getFormattedHour(instant, is24HourFormat)
    val dateFormatted = DateTimeFormatter
        .ofPattern("dd MMM yyyy")
        .withZone(ZoneId.systemDefault())
        .format(instant)
    return "$dateFormatted $hourFormatted"
}

/**
 * Convert scheduled meeting timestamp to readable form
 *
 * @param meeting   Meeting room item
 * @return          String formatted
 */
internal fun toScheduledTimeFormatted(meeting: MeetingRoomItem, is24HourFormat: Boolean): String? {
    val startTimestamp = meeting.scheduledStartTimestamp ?: return null
    val endTimestamp = meeting.scheduledEndTimestamp ?: return null

    val startHour = getFormattedHour(Instant.ofEpochSecond(startTimestamp), is24HourFormat)
    val endHour = getFormattedHour(Instant.ofEpochSecond(endTimestamp), is24HourFormat)
    return "$startHour - $endHour"
}

/**
 * Get hour properly formatted
 *
 * @param instant           Time instant to be formatted
 * @param is24HourFormat    Flag to use 12h or 24h format
 * @return                  Hour formatted
 */
private fun getFormattedHour(instant: Instant, is24HourFormat: Boolean): String =
    DateTimeFormatter
        .ofPattern(if (is24HourFormat) "HH:mm" else "hh:mma")
        .withZone(ZoneId.systemDefault())
        .format(instant)
        .lowercase()
