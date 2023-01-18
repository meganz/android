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
) -> @JvmSuppressWildcards String

/**
 * Mapper for scheduled meeting timestamp
 */
typealias ScheduledMeetingTimestampMapper = (
    @JvmSuppressWildcards MeetingRoomItem,
) -> @JvmSuppressWildcards String?

/**
 * Convert meeting timestamp to readable form
 *
 * @param timeStamp Timestamp
 * @return          String formatted
 */
internal fun toLastTimeFormatted(timeStamp: Long): String =
    DateTimeFormatter
        .ofPattern("dd MMM yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(timeStamp))

/**
 * Convert scheduled meeting timestamp to readable form
 *
 * @param meeting   Meeting room item
 * @return          String formatted
 */
internal fun toScheduledTimeFormatted(meeting: MeetingRoomItem): String? {
    val startTimestamp = meeting.scheduledStartTimestamp ?: return null
    val endTimestamp = meeting.scheduledEndTimestamp ?: return null

    val hourFormat = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    return hourFormat.format(Instant.ofEpochSecond(startTimestamp)) +
            " - " +
            hourFormat.format(Instant.ofEpochSecond(endTimestamp))
}
