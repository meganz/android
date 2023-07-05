package mega.privacy.android.app.presentation.extensions.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Check if there is no end date
 *
 * @return True if there is no until value. False otherwise.
 */
fun ChatScheduledRules.isForever(): Boolean = this.until == 0L

/**
 * Get Zoned date time of until value
 *
 * @return zoned date time of until value
 */
fun ChatScheduledRules.getUntilZonedDateTime(): ZonedDateTime? = when {
    this.isForever() -> null
    else -> this.until.parseUTCDate()
}

private fun Long.parseUTCDate(): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneId.systemDefault())