package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.search.DateFilterOption
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test

class DateFilterOptionLongMapperTest {
    private val underTest: DateFilterOptionLongMapper = DateFilterOptionLongMapper()

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is today`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.Today
        val result = underTest(dateFilterOption, instant)
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        assertEquals(result.first, today.atStartOfDay(zone).toInstant().epochSecond)
        assertEquals(result.second, instant.epochSecond)
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is last 7 days`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.Last7Days
        val result = underTest(dateFilterOption, instant)
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        val oneWeekAgo = today.minusDays(7)
        assertEquals(result.first, oneWeekAgo.atStartOfDay(zone).toInstant().epochSecond)
        assertEquals(result.second, instant.epochSecond)
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is last 30 days`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.Last30Days
        val result = underTest(dateFilterOption, instant)
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        val oneMonthAgo = today.minusDays(30)
        assertEquals(result.first, oneMonthAgo.atStartOfDay(zone).toInstant().epochSecond)
        assertEquals(result.second, instant.epochSecond)
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is this year`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.ThisYear
        val result = underTest(dateFilterOption, instant)
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        val startOfYear = today.withDayOfYear(1)
        assertEquals(result.first, startOfYear.atStartOfDay(zone).toInstant().epochSecond)
        assertEquals(result.second, instant.epochSecond)
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is last year`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.LastYear
        val result = underTest(dateFilterOption, instant)
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        val startOfLastYear = today.withDayOfYear(1).minusYears(1)
        val endOfLastYear = startOfLastYear.withDayOfYear(startOfLastYear.lengthOfYear())
        assertEquals(result.first, startOfLastYear.atStartOfDay(zone).toInstant().epochSecond)
        assertEquals(result.second, endOfLastYear.atStartOfDay(zone).toInstant().epochSecond)
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is older`() {
        val instant = Instant.now()
        val dateFilterOption = null
        val result = underTest(dateFilterOption, instant)
        assertEquals(result.first, Long.MIN_VALUE)
        assertEquals(result.second, instant.epochSecond)
    }

}