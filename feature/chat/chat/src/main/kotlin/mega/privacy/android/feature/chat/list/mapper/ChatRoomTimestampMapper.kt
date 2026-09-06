package mega.privacy.android.feature.chat.list.mapper

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.shared.resources.R as sharedR
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Maps chat room timestamps to localised, human readable labels for the chat list.
 */
internal class ChatRoomTimestampMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Format the last activity timestamp of a chat room.
     *
     * @param timestamp Last activity time in epoch seconds.
     */
    fun getLastTimeFormatted(timestamp: Long): String =
        DateUtils.getRelativeDateTimeString(
            context,
            timestamp * MILLISECONDS_IN_SECOND,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0,
        ).toString()

    /**
     * Format a scheduled meeting start and end time as an hour range.
     *
     * @param startTimestamp Meeting start time in epoch seconds.
     * @param endTimestamp Meeting end time in epoch seconds.
     */
    fun getMeetingTimeFormatted(startTimestamp: Long, endTimestamp: Long): String =
        "${getFormattedHour(startTimestamp)} - ${getFormattedHour(endTimestamp)}"

    /**
     * Resolve the section header shown above a meeting row, based on the current and previous item.
     *
     * Meetings are grouped into a "Past meetings" section, followed by day sections for the
     * upcoming pending meetings ("Today" or the formatted date).
     *
     * @param currentItem Meeting item the header would sit above.
     * @param previousItem Meeting item immediately before [currentItem], or null for the first row.
     * @return The header label, or null when [currentItem] does not start a new section.
     */
    fun getHeaderTimeFormatted(
        currentItem: ChatRoomItem,
        previousItem: ChatRoomItem?,
    ): String? = when {
        !currentItem.isPendingMeeting() && previousItem?.isPendingMeeting() == true ->
            context.getString(sharedR.string.meetings_list_past_header)

        currentItem.isPendingMeeting() && !isSameDay(
            currentItem.scheduledStartTimestamp(),
            previousItem?.scheduledStartTimestamp(),
        ) -> {
            val startTimestamp = currentItem.scheduledStartTimestamp() ?: return null
            if (isToday(startTimestamp)) {
                context.getString(sharedR.string.meetings_list_header_today)
            } else {
                DateTimeFormatter
                    .ofPattern(HEADER_DATE_FORMAT)
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(startTimestamp))
            }
        }

        else -> null
    }

    private fun ChatRoomItem.scheduledStartTimestamp(): Long? =
        (this as? ChatRoomItem.MeetingChatRoomItem)?.scheduledStartTimestamp

    private fun isToday(timestamp: Long): Boolean =
        timestamp.toLocalDate().isEqual(LocalDate.now(ZoneId.systemDefault()))

    private fun isSameDay(firstTimestamp: Long?, secondTimestamp: Long?): Boolean {
        if (firstTimestamp == null || secondTimestamp == null) return false
        return firstTimestamp.toLocalDate().isEqual(secondTimestamp.toLocalDate())
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochSecond(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

    private fun getFormattedHour(timestamp: Long): String =
        DateTimeFormatter
            .ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mma")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(timestamp))
            .lowercase()

    private companion object {
        const val MILLISECONDS_IN_SECOND = 1000L
        const val HEADER_DATE_FORMAT = "EEEE, d MMM"
    }
}
