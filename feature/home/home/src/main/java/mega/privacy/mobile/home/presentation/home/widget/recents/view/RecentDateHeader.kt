package mega.privacy.mobile.home.presentation.home.widget.recents.view

import android.text.format.DateFormat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.home.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun RecentDateHeader(timestamp: Long) {
    MegaText(
        text = FormatRecentsDate(timestamp),
        textColor = TextColor.Secondary,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(DATE_HEADER_TEST_TAG)
    )
}

/**
 * Format date from timestamp for header
 */
@Composable
private fun FormatRecentsDate(timestamp: Long): String {
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

internal const val DATE_HEADER_TEST_TAG = "recents_widget:date_header"

@CombinedThemePreviews
@Composable
private fun RecentDateHeaderTodayPreview() {
    AndroidThemeForPreviews {
        RecentDateHeader(timestamp = System.currentTimeMillis() / 1000)
    }
}

@CombinedThemePreviews
@Composable
private fun RecentDateHeaderYesterdayPreview() {
    AndroidThemeForPreviews {
        val yesterdayTimestamp = System.currentTimeMillis() / 1000 - 86400 // 24 hours ago
        RecentDateHeader(timestamp = yesterdayTimestamp)
    }
}

@CombinedThemePreviews
@Composable
private fun RecentDateHeaderOtherDatePreview() {
    AndroidThemeForPreviews {
        val otherDateTimestamp = System.currentTimeMillis() / 1000 - 172800 // 2 days ago
        RecentDateHeader(timestamp = otherDateTimestamp)
    }
}

