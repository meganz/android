package mega.privacy.android.core.formatter

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.junit.rules.TestRule
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import mega.privacy.android.shared.resources.R as SharedR

/**
 * Runs under Robolectric because [formatSubscriptionDate] resolves its patterns through
 * `DateFormat.getBestDateTimePattern`, which returns null on a plain JVM.
 *
 * Every case is expressed relative to the current day rather than a fixed date, because the
 * function reads the clock itself and takes no date parameter.
 */
@RunWith(RobolectricTestRunner::class)
class DateFormatterTest {

    @get:Rule
    val midnightRollover = MidnightRolloverRule()

    @Test
    fun `test that formatSubscriptionDate returns the today sentence for the current day`() {
        assertThat(formatSubscriptionDate(middayIn(0), renewing = true))
            .isEqualTo(LocalizedText.StringRes(SharedR.string.account_info_renews_today))
        assertThat(formatSubscriptionDate(middayIn(0), renewing = false))
            .isEqualTo(LocalizedText.StringRes(SharedR.string.account_info_expires_today))
    }

    @Test
    fun `test that formatSubscriptionDate returns the tomorrow sentence with the date`() {
        val renews = formatSubscriptionDate(middayIn(1), renewing = true)

        assertThat(renews).isInstanceOf(LocalizedText.StringRes::class.java)
        with(renews as LocalizedText.StringRes) {
            assertThat(resId).isEqualTo(SharedR.string.account_info_renews_tomorrow_date)
            assertThat(formatArgs.single().toString()).contains(today().plusDays(1).year.toString())
        }
        assertThat(stringResOf(formatSubscriptionDate(middayIn(1), renewing = false)))
            .isEqualTo(SharedR.string.account_info_expires_tomorrow_date)
    }

    @Test
    fun `test that formatSubscriptionDate returns the expired yesterday sentence for the previous day`() {
        assertThat(formatSubscriptionDate(middayIn(-1), renewing = false))
            .isEqualTo(LocalizedText.StringRes(SharedR.string.account_info_expired_yesterday))
    }

    @Test
    fun `test that formatSubscriptionDate falls back to the renews on sentence for a past renewal`() {
        val actual = formatSubscriptionDate(middayIn(-1), renewing = true)

        assertThat(actual).isInstanceOf(LocalizedText.StringRes::class.java)
        with(actual as LocalizedText.StringRes) {
            assertThat(resId).isEqualTo(SharedR.string.account_info_renews_on_date)
            assertThat(formatArgs.single().toString()).startsWith(weekdayNameIn(-1))
        }
    }

    @Test
    fun `test that formatSubscriptionDate keeps the weekday for a date within the next week`() {
        val actual = formatSubscriptionDate(middayIn(3), renewing = true) as LocalizedText.StringRes

        assertThat(actual.resId).isEqualTo(SharedR.string.account_info_renews_on_date)
        assertThat(actual.formatArgs.single().toString()).startsWith(weekdayNameIn(3))
    }

    @Test
    fun `test that formatSubscriptionDate uses a plain date beyond the next week`() {
        val actual = formatSubscriptionDate(middayIn(30), renewing = false) as LocalizedText.StringRes

        assertThat(actual.resId).isEqualTo(SharedR.string.account_info_expires_on_date)
        with(actual.formatArgs.single().toString()) {
            assertThat(this).doesNotContain(weekdayNameIn(30))
            assertThat(this).contains(today().plusDays(30).year.toString())
        }
    }

    private fun stringResOf(text: LocalizedText): Int =
        (text as LocalizedText.StringRes).resId

    private fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())

    private fun weekdayNameIn(dayOffset: Long): String =
        today().plusDays(dayOffset).dayOfWeek
            .getDisplayName(TextStyle.FULL, Locale.getDefault())

    /** Midday [dayOffset] days from today, so time-zone shifts cannot move it onto another date. */
    private fun middayIn(dayOffset: Long): Long =
        today().plusDays(dayOffset)
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()
}

/**
 * Reruns a test once if the calendar day rolled over while it was running. [formatSubscriptionDate]
 * reads the clock itself, so a rollover between a test building its timestamps and the function
 * resolving "today" shifts every case by a day. A failure with the day unchanged is a real failure
 * and is rethrown untouched.
 */
class MidnightRolloverRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val startedOn = LocalDate.now(ZoneId.systemDefault())
                try {
                    base.evaluate()
                } catch (error: AssertionError) {
                    if (LocalDate.now(ZoneId.systemDefault()) == startedOn) throw error
                    base.evaluate()
                }
            }
        }
}
