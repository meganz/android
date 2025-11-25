package mega.privacy.mobile.home.presentation.home.widget.recents.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId

/**
 * Represents timestamp that can be formatted to time and date strings in Composable
 * @param timestamp timestamp
 */
data class RecentsTimestampText(
    val timestamp: Long,
) {
    /**
     * Timestamp of date only (time set to 00:00:00)
     */
    val dateOnlyTimestamp = Instant.ofEpochSecond(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toEpochSecond()

    /**
     * Format time from timestamp for bucket item
     */
    @Composable
    fun formatTime(): String {
        val locale = Locale.current.platformLocale
        val dateFormat = remember(locale) {
            SimpleDateFormat.getTimeInstance(
                SimpleDateFormat.SHORT,
                locale
            )
        }
        val instant = Instant.ofEpochSecond(timestamp)
        val date = java.util.Date.from(instant)
        return dateFormat.format(date)
    }
}