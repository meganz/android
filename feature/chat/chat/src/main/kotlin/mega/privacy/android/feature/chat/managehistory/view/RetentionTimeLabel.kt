package mega.privacy.android.feature.chat.managehistory.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import mega.privacy.android.feature.chat.managehistory.model.ONE_MONTH
import mega.privacy.android.feature.chat.managehistory.model.ONE_WEEK
import mega.privacy.android.feature.chat.managehistory.model.ONE_YEAR
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Formats a retention time as a human readable duration, for example "1 day" or "2 weeks". Returns
 * null when message clearing is off or the period is not a whole number of a single unit.
 *
 * @param retentionTime period after which messages are deleted.
 */
@Composable
internal fun retentionTimeLabel(retentionTime: Duration): String? = when {
    retentionTime == Duration.ZERO -> null

    retentionTime.divisibleBy(ONE_YEAR) -> retentionTime.pluralLabel(
        unit = ONE_YEAR,
        pluralId = sharedR.plurals.contact_info_retention_time_years,
    )

    retentionTime.divisibleBy(ONE_MONTH) -> retentionTime.pluralLabel(
        unit = ONE_MONTH,
        pluralId = sharedR.plurals.contact_info_retention_time_months,
    )

    retentionTime.divisibleBy(ONE_WEEK) -> retentionTime.pluralLabel(
        unit = ONE_WEEK,
        pluralId = sharedR.plurals.contact_info_retention_time_weeks,
    )

    retentionTime.divisibleBy(1.days) -> retentionTime.pluralLabel(
        unit = 1.days,
        pluralId = sharedR.plurals.contact_info_retention_time_days,
    )

    retentionTime.divisibleBy(1.hours) -> retentionTime.pluralLabel(
        unit = 1.hours,
        pluralId = sharedR.plurals.contact_info_retention_time_hours,
    )

    else -> null
}

@Composable
private fun Duration.pluralLabel(unit: Duration, pluralId: Int): String {
    val quantity = countOf(unit)
    return pluralStringResource(pluralId, quantity, quantity)
}

private fun Duration.divisibleBy(unit: Duration): Boolean =
    inWholeSeconds % unit.inWholeSeconds == 0L

private fun Duration.countOf(unit: Duration): Int =
    (inWholeSeconds / unit.inWholeSeconds).toInt()
