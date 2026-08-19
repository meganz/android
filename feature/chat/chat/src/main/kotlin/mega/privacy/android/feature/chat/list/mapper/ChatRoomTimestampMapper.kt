package mega.privacy.android.feature.chat.list.mapper

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
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

    private fun getFormattedHour(timestamp: Long): String =
        DateTimeFormatter
            .ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mma")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(timestamp))
            .lowercase()

    private companion object {
        const val MILLISECONDS_IN_SECOND = 1000L
    }
}
