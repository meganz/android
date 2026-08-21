package mega.privacy.android.feature.sharelink.presentation

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * A link expires at the end of the day the user picked, in their own timezone — matching the legacy
 * Get link screen, which sets the expiry to 23:59 local time.
 *
 * The date picker works in a different space: it returns, and pre-selects from, midnight UTC of the
 * chosen day. [endOfLocalDay] and [utcMidnightOfLocalDay] convert between the two so that the value
 * held in the UI state and sent to the SDK is always a true instant.
 */
internal val UTC: TimeZone = TimeZone.getTimeZone("UTC")

/** Formats a link expiry [millis] instant as a date in the user's timezone. */
internal fun formatExpiryDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

/**
 * Converts a date picker result — midnight UTC of the chosen day — to the instant at which that day
 * ends locally.
 */
internal fun endOfLocalDay(utcMidnightMillis: Long): Long {
    val picked = Calendar.getInstance(UTC).apply { timeInMillis = utcMidnightMillis }
    return Calendar.getInstance().apply {
        clear()
        set(
            picked.get(Calendar.YEAR),
            picked.get(Calendar.MONTH),
            picked.get(Calendar.DAY_OF_MONTH),
            23,
            59,
            59,
        )
    }.timeInMillis
}

/**
 * Converts a stored expiry instant back to midnight UTC of the local day it falls on, so the date
 * picker pre-selects the day the user actually chose.
 */
internal fun utcMidnightOfLocalDay(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(UTC).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0,
        )
    }.timeInMillis
}

/** Today in the user's timezone, in the date picker's midnight-UTC space. */
internal fun todayStartUtcMillis(): Long = utcMidnightOfLocalDay(System.currentTimeMillis())

/**
 * The last instant the API accepts as a link expiry: 2^31-1 seconds since the epoch,
 * 2038-01-19T03:14:07Z — the 32-bit signed time limit.
 *
 * Anything beyond is rejected server-side. The SDK itself is 64-bit clean (`m_time_t` is
 * `int64_t`), so there is no constant to borrow; the boundary was established from the API
 * refusing 2038-01-19 and 2100-12-31 (TestRail T21407205).
 */
internal const val MAX_EXPIRY_SECONDS: Long = Int.MAX_VALUE.toLong()

private const val MAX_EXPIRY_MILLIS: Long = MAX_EXPIRY_SECONDS * 1000L

/**
 * Whether the day the picker reports as [utcMidnightMillis] can be used as a link expiry: not in
 * the past, and within the API's limit.
 *
 * The upper end is compared as an *instant*, not a calendar date, because the expiry sent to the
 * SDK is the end of the chosen local day — so which day is the last usable one depends on the
 * user's timezone. Behind UTC, the end of a local day lands at a later instant, so those users lose
 * a day that users at or ahead of UTC keep.
 */
internal fun isSelectableExpiryDay(utcMidnightMillis: Long): Boolean =
    utcMidnightMillis >= todayStartUtcMillis() &&
            endOfLocalDay(utcMidnightMillis) <= MAX_EXPIRY_MILLIS

/**
 * The last year that contains a selectable expiry day, so the picker's year list stops there
 * instead of running to 2100.
 */
internal fun maxExpiryYear(): Int = Calendar.getInstance().apply {
    timeInMillis = MAX_EXPIRY_MILLIS
}.get(Calendar.YEAR)
