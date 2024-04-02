package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.search.DateFilterOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject


/**
 * Date filter option mapper to map [DateFilterOption] to a Long Interval
 */
class DateFilterOptionLongMapper @Inject constructor() {
    /**
     *  Invoke
     *
     *  @param dateFilterOption filter option
     *  @return pair of long to indicate lower and upper limits
     */
    operator fun invoke(
        dateFilterOption: DateFilterOption?,
        now: Instant = Instant.now(),
    ): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        return when (dateFilterOption) {
            DateFilterOption.Today -> {
                val startOfDay = today.atStartOfDay(zone).toInstant()
                startOfDay.epochSecond to now.epochSecond
            }

            DateFilterOption.Last7Days -> {
                val oneWeekAgo = today.minusDays(7)
                val startOfLastWeek = oneWeekAgo.atStartOfDay(zone).toInstant()
                startOfLastWeek.epochSecond to now.epochSecond
            }

            DateFilterOption.Last30Days -> {
                val oneMonthAgo = today.minusDays(30)
                val startOfLastMonth = oneMonthAgo.atStartOfDay(zone).toInstant()
                startOfLastMonth.epochSecond to now.epochSecond
            }

            DateFilterOption.ThisYear -> {
                val startOfYear = LocalDate.of(today.year, 1, 1)
                val startOfThisYear = startOfYear.atStartOfDay(zone).toInstant()
                startOfThisYear.epochSecond to now.epochSecond
            }

            DateFilterOption.LastYear -> {
                val startOfLastYear = LocalDate.of(today.year - 1, 1, 1)
                val startOfLastYearInstant = startOfLastYear.atStartOfDay(zone).toInstant()
                val endOfLastYear = LocalDate.of(today.year - 1, 12, 31)
                val endOfLastYearInstant = endOfLastYear.atStartOfDay(zone).toInstant()
                startOfLastYearInstant.epochSecond to endOfLastYearInstant.epochSecond
            }

            else -> {
                Long.MIN_VALUE to now.epochSecond
            }
        }
    }
}
