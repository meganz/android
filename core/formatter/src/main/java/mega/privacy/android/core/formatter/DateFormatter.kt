package mega.privacy.android.core.formatter

import android.text.format.DateFormat
import android.text.format.DateFormat.getBestDateTimePattern
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.shared.resources.R as SharedR
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

/**
 * Formats a subscription renewal or expiry date as a complete sentence: "Renews today",
 * "Expires tomorrow, Aug 25, 2026", "Renews on Thursday, Aug 27, 2026" within the coming week, or
 * "Renews on Sep 23, 2026" beyond it.
 *
 * The wording is a whole sentence rather than a template plus a separate date, because "on" only
 * belongs in front of an actual date — not in front of "today" or "tomorrow".
 *
 * A renewal date in the past keeps the neutral "Renews on <date>" wording: the subscription is
 * still marked valid at that point, so the renewal has not gone through yet and cannot be stated
 * as done. Only an expiry can, hence "Expired yesterday".
 *
 * @param timestamp Timestamp in seconds of the renewal or expiry date.
 * @param renewing True for a renewal notice, false for an expiry notice.
 * @return The sentence as a [LocalizedText]; resolve it with `.text` in a composable or
 * `get(context)` elsewhere.
 */
fun formatSubscriptionDate(timestamp: Long, renewing: Boolean): LocalizedText {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochSecond(timestamp).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)
    val nextWeek = today.plusWeeks(1)

    return when {
        date == today -> LocalizedText.StringRes(
            if (renewing) SharedR.string.account_info_renews_today
            else SharedR.string.account_info_expires_today
        )

        date == tomorrow -> LocalizedText.StringRes(
            if (renewing) SharedR.string.account_info_renews_tomorrow_date
            else SharedR.string.account_info_expires_tomorrow_date,
            listOf(timestamp.formatWith("d MMM yyyy"))
        )

        !renewing && date == yesterday ->
            LocalizedText.StringRes(SharedR.string.account_info_expired_yesterday)

        else -> LocalizedText.StringRes(
            if (renewing) SharedR.string.account_info_renews_on_date
            else SharedR.string.account_info_expires_on_date,
            listOf(
                if (date.isBefore(nextWeek)) timestamp.formatWith("EEEE, d MMM yyyy")
                else timestamp.formatWith("MMM d, yyyy")
            )
        )
    }
}

private fun Long.formatWith(skeleton: String): String =
    DateTimeFormatter.ofPattern(getBestDateTimePattern(getUserLocale(), skeleton))
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(this))

private fun getUserLocale(): Locale = Locale.getDefault()

fun formatModifiedDate(locale: Locale, modificationTime: Long): String {
    val dateFormat = SimpleDateFormat(
        DateFormat.getBestDateTimePattern(
            locale, "d MMM yyyy HH:mm"
        ),
        locale
    )
    val cal = Calendar.getInstance().apply {
        timeInMillis = modificationTime * 1000
    }
    return dateFormat.format(cal.time)
}
