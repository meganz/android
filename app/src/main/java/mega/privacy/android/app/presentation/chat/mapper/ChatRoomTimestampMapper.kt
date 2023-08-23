package mega.privacy.android.app.presentation.chat.mapper

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_12HOUR
import android.text.format.DateUtils.FORMAT_24HOUR
import android.text.format.DateUtils.FORMAT_SHOW_TIME
import android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY
import android.text.format.DateUtils.HOUR_IN_MILLIS
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.absoluteValue

/**
 * Mapper for chat room last timestamp
 */
class ChatRoomTimestampMapper @Inject constructor(
    deviceGateway: DeviceGateway,
    @ApplicationContext private val context: Context,
) {

    private val is24HourFormat by lazy { deviceGateway.is24HourFormat() }
    private val hourFormat by lazy { if (is24HourFormat) "HH:mm" else "hh:mma" }
    private val dateFormat by lazy { "dd MMMM yyyy $hourFormat" }
    private val headerDateFormat by lazy { "EEEE, d MMM" }
    private val waitingRoomFormat by lazy { "EEEE, d MMM yyyy" }

    /**
     * Convert chat room item last timestamp to readable form
     *
     * @param timeStamp Chat last timestamp
     * @return          Time formatted
     */
    @Suppress("DEPRECATION")
    fun getLastTimeFormatted(
        timeStamp: Long,
    ): String {
        val nowInstant = Instant.now()
        val timestampInstant = Instant.ofEpochSecond(timeStamp)

        return when {
            DateUtils.isToday(timestampInstant.toEpochMilli()) ->
                "${context.getString(R.string.label_today)} ${timestampInstant.getFormattedHour()}"

            Duration.between(timestampInstant, nowInstant).toDays().absoluteValue <= 7 ->
                DateUtils.getRelativeTimeSpanString(
                    timestampInstant.toEpochMilli(),
                    nowInstant.toEpochMilli(),
                    HOUR_IN_MILLIS,
                    FORMAT_SHOW_WEEKDAY or FORMAT_SHOW_TIME
                            or if (is24HourFormat) FORMAT_24HOUR else FORMAT_12HOUR
                ).toString()

            else ->
                DateTimeFormatter
                    .ofPattern(dateFormat)
                    .withZone(ZoneId.systemDefault())
                    .format(timestampInstant)
        }
    }

    /**
     * Convert meeting start/end timestamp to readable form
     *
     * @param meetingStartTimestamp     Meeting start time
     * @param meetingEndTimestamp       Meeting end time
     * @return                          Time formatted
     */
    fun getMeetingTimeFormatted(
        meetingStartTimestamp: Long,
        meetingEndTimestamp: Long,
    ): String {
        val startHour = Instant.ofEpochSecond(meetingStartTimestamp).getFormattedHour()
        val endHour = Instant.ofEpochSecond(meetingEndTimestamp).getFormattedHour()
        return "$startHour - $endHour"
    }

    /**
     * Get header time formatted given two [ChatRoomItem]
     *
     * @param currentItem   Current [ChatRoomItem]
     * @param previousItem  Previous [ChatRoomItem]
     * @return              Time formatted
     */
    fun getHeaderTimeFormatted(
        currentItem: ChatRoomItem,
        previousItem: ChatRoomItem?,
    ): String? =
        when {
            !currentItem.isPendingMeeting() && previousItem?.isPendingMeeting() == true ->
                context.getString(R.string.meetings_list_past_header)

            currentItem.isPendingMeeting() && !isSameDay(
                currentItem.scheduledStartTimestamp(),
                previousItem?.scheduledStartTimestamp()
            ) -> {
                val startTimestamp = currentItem.scheduledStartTimestamp()!!

                if (DateUtils.isToday(startTimestamp)) {
                    context.getString(R.string.label_today)
                } else {
                    DateTimeFormatter
                        .ofPattern(headerDateFormat)
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochSecond(startTimestamp))
                }
            }

            else -> null
        }

    /**
     * Get Date Time formatted for Waiting Rooms
     *
     * @param startTimestamp    Meeting start time
     * @param endTimestamp      Meeting end time
     * @return                  Time formatted
     */
    fun getWaitingRoomTimeFormatted(
        startTimestamp: Long,
        endTimestamp: Long,
    ): String {
        val time = getMeetingTimeFormatted(startTimestamp, endTimestamp)
        val date = DateTimeFormatter
            .ofPattern(waitingRoomFormat)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(startTimestamp))

        return "$date · $time"
    }

    private fun ChatRoomItem.scheduledStartTimestamp(): Long? =
        if (this is ChatRoomItem.MeetingChatRoomItem) this.scheduledStartTimestamp else null

    private fun isSameDay(timeStampA: Long?, timeStampB: Long?): Boolean =
        if (timeStampA == null || timeStampB == null) {
            false
        } else {
            val dayA = Instant.ofEpochSecond(timeStampA)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val dayB = Instant.ofEpochSecond(timeStampB)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            dayA.isEqual(dayB)
        }

    /**
     * Get hour properly formatted
     */
    private fun Instant.getFormattedHour(): String =
        DateTimeFormatter
            .ofPattern(hourFormat)
            .withZone(ZoneId.systemDefault())
            .format(this)
            .lowercase()
}
