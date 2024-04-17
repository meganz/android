package mega.privacy.android.data.mapper.search

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.search.DateFilterOption
import java.time.Instant
import java.time.LocalDate
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
        assertThat(result.first).isEqualTo(today.atStartOfDay(zone).toInstant().epochSecond)
        assertThat(result.second).isEqualTo(instant.epochSecond)
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is last 7 days`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.Last7Days
        val result = underTest(dateFilterOption, instant)
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        val oneWeekAgo = today.minusDays(7)
        assertThat(result.first).isEqualTo(oneWeekAgo.atStartOfDay(zone).toInstant().epochSecond)
        assertThat(result.second).isEqualTo(instant.epochSecond)
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is last 30 days`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.Last30Days
        val result = underTest(dateFilterOption, instant)
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        val oneMonthAgo = today.minusDays(30)
        assertThat(result.first).isEqualTo(oneMonthAgo.atStartOfDay(zone).toInstant().epochSecond)
        assertThat(result.second).isEqualTo(instant.epochSecond)
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is this year`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.ThisYear
        val result = underTest(dateFilterOption, instant)
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        val startOfYear = today.withDayOfYear(1)
        assertThat(result.first).isEqualTo(startOfYear.atStartOfDay(zone).toInstant().epochSecond)
        assertThat(result.second).isEqualTo(instant.epochSecond)
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
        assertThat(result.first).isEqualTo(
            startOfLastYear.atStartOfDay(zone).toInstant().epochSecond
        )
        assertThat(result.second).isEqualTo(
            endOfLastYear.atStartOfDay(zone).toInstant().epochSecond
        )
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is older`() {
        val instant = Instant.now()
        val dateFilterOption = DateFilterOption.Older
        val zone = ZoneId.systemDefault()
        val today = instant.atZone(zone).toLocalDate()
        val endOfLastLastYear = LocalDate.of(today.year - 2, 12, 31)
        val result = underTest(dateFilterOption, instant)
        assertThat(result.first).isEqualTo(Long.MIN_VALUE)
        assertThat(result.second).isEqualTo(
            endOfLastLastYear.atStartOfDay(zone).toInstant().epochSecond
        )
    }

    @Test
    fun `test that invoke returns pair of long to indicate lower and upper limits when selected date filter is unknown`() {
        val instant = Instant.now()
        val dateFilterOption = null
        val result = underTest(dateFilterOption, instant)
        assertThat(result.first).isEqualTo(Long.MIN_VALUE)
        assertThat(result.second).isEqualTo(instant.epochSecond)
    }
}
