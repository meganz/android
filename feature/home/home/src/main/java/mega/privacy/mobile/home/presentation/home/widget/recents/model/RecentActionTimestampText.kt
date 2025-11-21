package mega.privacy.mobile.home.presentation.home.widget.recents.model

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import mega.privacy.android.feature.home.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Represents timestamp that can be formatted to time and date strings in Composable
 * @param timestamp timestamp
 */
data class RecentActionTimestampText(
    val timestamp: Long,
) {
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

    /**
     * Format date from timestamp for header
     */
    @Composable
    fun formatDate(): String {
        val locale = Locale.current.platformLocale
        val zoneId = remember { ZoneId.systemDefault() }
        val timestampInstant = Instant.ofEpochSecond(timestamp)
        val timestampDate = timestampInstant.atZone(zoneId).toLocalDate()
        val todayDate = LocalDate.now(zoneId)
        val yesterdayDate = todayDate.minusDays(1)

        val dateTimeFormatter = remember(locale) {
            DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(
                    locale,
                    "EEEE, d MMM yyyy"
                )
            ).withLocale(locale)
        }

        return when (timestampDate) {
            todayDate -> {
                stringResource(R.string.label_today)
            }

            yesterdayDate -> {
                stringResource(R.string.label_yesterday)
            }

            else -> {
                timestampInstant.atZone(zoneId).format(dateTimeFormatter)
            }
        }
    }
}

