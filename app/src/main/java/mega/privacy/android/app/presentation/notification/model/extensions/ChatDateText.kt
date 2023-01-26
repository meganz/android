package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingDateTimeAlert
import mega.privacy.android.domain.entity.UserAlert
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Chat Date text
 *
 */
internal fun UserAlert.chatDateText(): (Context) -> AnnotatedString? = { context ->
    if (this is ScheduledMeetingAlert && startDate != null && endDate != null) {
        val startDateTime = startDate?.toZonedDateTime()
        val endDateTime = endDate?.toZonedDateTime()
        val hourFormatter = getHourFormatter(context)

        AnnotatedString.Builder().apply {
            val dateText = getDateFormatter().format(startDateTime)
            if (this@chatDateText is UpdatedScheduledMeetingDateTimeAlert && hasDateChanged) {
                append(dateText.boldStyle())
            } else {
                append(dateText)
            }

            append(" · ")

            val timeText =
                "${hourFormatter.format(startDateTime)} - ${hourFormatter.format(endDateTime)}"
            if (this@chatDateText is UpdatedScheduledMeetingDateTimeAlert && hasTimeChanged) {
                append(timeText.boldStyle())
            } else {
                append(timeText)
            }
        }.toAnnotatedString()
    } else {
        null
    }
}

private fun Long.toZonedDateTime(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)

private fun String.boldStyle(): AnnotatedString =
    AnnotatedString(this, SpanStyle(fontWeight = FontWeight.Bold))

private fun getHourFormatter(context: Context): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern(
            if (DateFormat.is24HourFormat(context)) {
                "HH:mm"
            } else {
                "hh:mm a"
            }
        )
        .withZone(ZoneId.systemDefault())

private fun getDateFormatter(): DateTimeFormatter =
    DateTimeFormatter
        .ofPattern("E',' d MMM',' yyyy")
        .withZone(ZoneId.systemDefault())
