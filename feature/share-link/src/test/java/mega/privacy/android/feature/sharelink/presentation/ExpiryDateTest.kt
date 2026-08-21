package mega.privacy.android.feature.sharelink.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * The default timezone is pinned per test so these exercise real offsets rather than whatever the
 * build machine happens to be set to — the UTC-vs-local confusion this code exists to resolve is
 * invisible when both are the same.
 */
class ExpiryDateTest {

    @AfterEach
    fun tearDown() {
        TimeZone.setDefault(null)
    }

    private fun useTimeZone(id: String) = TimeZone.setDefault(TimeZone.getTimeZone(id))

    private fun utcMidnight(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(UTC).apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis

    private fun Long.localFields(): List<Int> =
        Calendar.getInstance().apply { timeInMillis = this@localFields }.let {
            listOf(
                it.get(Calendar.YEAR),
                it.get(Calendar.MONTH),
                it.get(Calendar.DAY_OF_MONTH),
                it.get(Calendar.HOUR_OF_DAY),
                it.get(Calendar.MINUTE),
                it.get(Calendar.SECOND),
            )
        }

    @ParameterizedTest
    @ValueSource(strings = [AUCKLAND, LOS_ANGELES, "UTC"])
    fun `test that endOfLocalDay returns the last second of the chosen day in local time`(zone: String) {
        useTimeZone(zone)

        val endOfDay = endOfLocalDay(utcMidnight(2026, Calendar.JULY, 29))

        assertThat(endOfDay.localFields())
            .isEqualTo(listOf(2026, Calendar.JULY, 29, 23, 59, 59))
    }

    @ParameterizedTest
    @ValueSource(strings = [AUCKLAND, LOS_ANGELES, "UTC"])
    fun `test that endOfLocalDay returns an instant in the future when today is chosen`(zone: String) {
        useTimeZone(zone)

        val endOfToday = endOfLocalDay(todayStartUtcMillis())

        assertThat(endOfToday).isGreaterThan(System.currentTimeMillis())
    }

    @ParameterizedTest
    @ValueSource(strings = [AUCKLAND, LOS_ANGELES, "UTC"])
    fun `test that utcMidnightOfLocalDay round-trips the day chosen in the picker`(zone: String) {
        useTimeZone(zone)
        val picked = utcMidnight(2026, Calendar.JULY, 29)

        val roundTripped = utcMidnightOfLocalDay(endOfLocalDay(picked))

        assertThat(roundTripped).isEqualTo(picked)
    }

    @ParameterizedTest
    @ValueSource(strings = [AUCKLAND, LOS_ANGELES, "UTC"])
    fun `test that todayStartUtcMillis makes the local today selectable`(zone: String) {
        useTimeZone(zone)

        val floor = todayStartUtcMillis()

        assertThat(floor).isEqualTo(utcMidnightOfLocalDay(System.currentTimeMillis()))
        assertThat(endOfLocalDay(floor).localFields().take(3))
            .isEqualTo(System.currentTimeMillis().localFields().take(3))
    }

    @Test
    fun `test that formatExpiryDate renders the local day when the day ends on the next UTC day`() {
        // In Los Angeles the last second of 29 July is already 30 July in UTC, so formatting in UTC
        // would show the user a day they did not pick.
        useTimeZone(LOS_ANGELES)
        val endOfDay = endOfLocalDay(utcMidnight(2026, Calendar.JULY, 29))
        val utcDay = Calendar.getInstance(UTC)
            .apply { timeInMillis = endOfDay }
            .get(Calendar.DAY_OF_MONTH)
        assertThat(utcDay).isEqualTo(30)

        val formatted = formatExpiryDate(endOfDay)

        assertThat(formatted)
            .isEqualTo(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(endOfDay)))
        assertThat(formatted).isNotEqualTo(
            DateFormat.getDateInstance(DateFormat.MEDIUM)
                .apply { timeZone = UTC }
                .format(Date(endOfDay))
        )
    }

    @Test
    fun `test that MAX_EXPIRY_SECONDS is the 32-bit signed time limit`() {
        assertThat(MAX_EXPIRY_SECONDS).isEqualTo(2_147_483_647L)
    }

    @Test
    fun `test that maxExpiryYear is 2038`() {
        useTimeZone(UTC.id)

        assertThat(maxExpiryYear()).isEqualTo(2038)
    }

    @Test
    fun `test that isSelectableExpiryDay accepts the last day inside the API limit`() {
        // The limit instant is 2038-01-19T03:14:07Z, so in UTC the whole of 18 January still fits.
        useTimeZone(UTC.id)

        assertThat(isSelectableExpiryDay(utcMidnight(2038, Calendar.JANUARY, 18))).isTrue()
    }

    @Test
    fun `test that isSelectableExpiryDay rejects the first day past the API limit`() {
        // The day QA picked: its end of day is past the limit, so the API refused the save.
        useTimeZone(UTC.id)

        assertThat(isSelectableExpiryDay(utcMidnight(2038, Calendar.JANUARY, 19))).isFalse()
    }

    @Test
    fun `test that isSelectableExpiryDay rejects a date far past the API limit`() {
        useTimeZone(UTC.id)

        assertThat(isSelectableExpiryDay(utcMidnight(2100, Calendar.DECEMBER, 31))).isFalse()
    }

    @Test
    fun `test that a timezone behind UTC loses the last day a UTC user keeps`() {
        // End of 18 January in Los Angeles falls on 19 January in UTC, past the limit — so the last
        // usable day there is a day earlier than for a UTC user.
        useTimeZone(LOS_ANGELES)

        assertThat(isSelectableExpiryDay(utcMidnight(2038, Calendar.JANUARY, 18))).isFalse()
        assertThat(isSelectableExpiryDay(utcMidnight(2038, Calendar.JANUARY, 17))).isTrue()
    }

    @Test
    fun `test that a timezone ahead of UTC keeps the last day`() {
        useTimeZone(AUCKLAND)

        assertThat(isSelectableExpiryDay(utcMidnight(2038, Calendar.JANUARY, 18))).isTrue()
        assertThat(isSelectableExpiryDay(utcMidnight(2038, Calendar.JANUARY, 19))).isFalse()
    }

    @Test
    fun `test that isSelectableExpiryDay rejects a day in the past`() {
        useTimeZone(UTC.id)

        assertThat(isSelectableExpiryDay(utcMidnight(2020, Calendar.JANUARY, 1))).isFalse()
    }

    @Test
    fun `test that every accepted day maps to an instant within the API limit`() {
        // The property the picker bound exists to guarantee: nothing it allows can be rejected for
        // being out of range.
        for (zone in listOf(UTC.id, AUCKLAND, LOS_ANGELES)) {
            useTimeZone(zone)
            for (day in 10..25) {
                val utcMidnightMillis = utcMidnight(2038, Calendar.JANUARY, day)
                if (isSelectableExpiryDay(utcMidnightMillis)) {
                    val expirySeconds = endOfLocalDay(utcMidnightMillis) / 1000
                    assertThat(expirySeconds).isAtMost(MAX_EXPIRY_SECONDS)
                }
            }
        }
    }

    private companion object {
        // A large positive offset and a negative one, where UTC-midnight storage breaks differently.
        const val AUCKLAND = "Pacific/Auckland"
        const val LOS_ANGELES = "America/Los_Angeles"
    }
}
