package mega.privacy.android.feature.payment.model

import java.time.temporal.ChronoUnit

/**
 * Period of a one-off (non-recurring) plan, expressed as a whole [value] of a single [unit] chosen to
 * be the largest that fits the plan's duration (e.g. 12 [ChronoUnit.MONTHS], 5 [ChronoUnit.DAYS]).
 *
 * @property value the number of [unit]s
 * @property unit the time unit the [value] is expressed in (MONTHS, DAYS, HOURS or MINUTES)
 */
data class PlanPeriod(
    val value: Int,
    val unit: ChronoUnit,
)
