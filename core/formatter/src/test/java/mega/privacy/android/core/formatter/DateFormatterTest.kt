package mega.privacy.android.core.formatter

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import mega.privacy.android.shared.resources.R as SharedR

/**
 * Runs under Robolectric because [formatSubscriptionDate] resolves its patterns through
 * `DateFormat.getBestDateTimePattern`, which returns null on a plain JVM.
 */
@RunWith(RobolectricTestRunner::class)
class DateFormatterTest {

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
        assertThat(actual.formatArgs.single().toString()).matches("[A-Z][a-z]{2} \\d{1,2}, \\d{4}")
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
