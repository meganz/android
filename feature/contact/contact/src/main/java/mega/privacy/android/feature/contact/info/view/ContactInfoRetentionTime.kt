package mega.privacy.android.feature.contact.info.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Formats a chat history retention time as a human readable duration, e.g. "1 day" or "2 weeks".
 * Returns null when history clearing is disabled or the value does not match a whole time unit.
 *
 * @param timeInSeconds retention time in seconds.
 */
@Composable
internal fun getRetentionTimeString(timeInSeconds: Long): String? = when {
    timeInSeconds == 0L -> null

    timeInSeconds % SECONDS_IN_YEAR == 0L ->
        stringResource(sharedR.string.contact_info_retention_time_year)

    timeInSeconds % SECONDS_IN_MONTH_30 == 0L -> {
        val numberOfMonths = (timeInSeconds / SECONDS_IN_MONTH_30).toInt()
        pluralStringResource(
            sharedR.plurals.contact_info_retention_time_months,
            numberOfMonths,
            numberOfMonths,
        )
    }

    timeInSeconds % SECONDS_IN_WEEK == 0L -> {
        val numberOfWeeks = (timeInSeconds / SECONDS_IN_WEEK).toInt()
        pluralStringResource(
            sharedR.plurals.contact_info_retention_time_weeks,
            numberOfWeeks,
            numberOfWeeks,
        )
    }

    timeInSeconds % SECONDS_IN_DAY == 0L -> {
        val numberOfDays = (timeInSeconds / SECONDS_IN_DAY).toInt()
        pluralStringResource(
            sharedR.plurals.contact_info_retention_time_days,
            numberOfDays,
            numberOfDays,
        )
    }

    timeInSeconds % SECONDS_IN_HOUR == 0L -> {
        val numberOfHours = (timeInSeconds / SECONDS_IN_HOUR).toInt()
        pluralStringResource(
            sharedR.plurals.contact_info_retention_time_hours,
            numberOfHours,
            numberOfHours,
        )
    }

    else -> null
}

internal const val SECONDS_IN_HOUR = 60L * 60
internal const val SECONDS_IN_DAY = SECONDS_IN_HOUR * 24
internal const val SECONDS_IN_WEEK = SECONDS_IN_DAY * 7
internal const val SECONDS_IN_MONTH_30 = SECONDS_IN_DAY * 30
internal const val SECONDS_IN_YEAR = SECONDS_IN_DAY * 365
