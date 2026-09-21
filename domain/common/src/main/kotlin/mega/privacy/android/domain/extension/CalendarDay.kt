package mega.privacy.android.domain.extension

import java.time.Instant
import java.time.ZoneId

/**
 * The calendar day this epoch-milliseconds timestamp falls on, in the device time zone. Use it for
 * limits defined as a fixed 00:00-23:59 window rather than a rolling period.
 */
fun Long.toEpochDay(): Long =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
