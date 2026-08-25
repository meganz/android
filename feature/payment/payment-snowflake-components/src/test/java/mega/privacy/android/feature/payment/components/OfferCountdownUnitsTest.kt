package mega.privacy.android.feature.payment.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class OfferCountdownUnitsTest {

    @Test
    fun `test that offerCountdownUnits splits the remaining time into days hours and minutes`() {
        val units = offerCountdownUnits(28.days + 12.hours + 2.minutes)

        assertThat(units.days).isEqualTo(28L)
        assertThat(units.hours).isEqualTo(12L)
        assertThat(units.minutes).isEqualTo(2L)
    }

    @Test
    fun `test that offerCountdownUnits zero pads each unit to two digits`() {
        val units = offerCountdownUnits(1.days + 2.hours + 3.minutes)

        assertThat(units.daysText).isEqualTo("01")
        assertThat(units.hoursText).isEqualTo("02")
        assertThat(units.minutesText).isEqualTo("03")
    }

    @Test
    fun `test that offerCountdownUnits drops the seconds of a partial minute`() {
        val units = offerCountdownUnits(5.minutes + 59.seconds)

        assertThat(units.minutesText).isEqualTo("05")
    }

    @Test
    fun `test that offerCountdownUnits reports one minute when under a minute is left`() {
        val units = offerCountdownUnits(30.seconds)

        assertThat(units.daysText).isEqualTo("00")
        assertThat(units.hoursText).isEqualTo("00")
        assertThat(units.minutesText).isEqualTo("01")
    }

    @Test
    fun `test that offerCountdownUnits reports one minute when only seconds are left`() {
        val units = offerCountdownUnits(1.seconds)

        assertThat(units.minutes).isEqualTo(1L)
    }

    @Test
    fun `test that offerCountdownUnits reports zeros when the offer has elapsed`() {
        val units = offerCountdownUnits(Duration.ZERO)

        assertThat(units.daysText).isEqualTo("00")
        assertThat(units.hoursText).isEqualTo("00")
        assertThat(units.minutesText).isEqualTo("00")
    }

    @Test
    fun `test that offerCountdownUnits reports zeros when the offer elapsed a while ago`() {
        val units = offerCountdownUnits(-2.hours)

        assertThat(units.daysText).isEqualTo("00")
        assertThat(units.hoursText).isEqualTo("00")
        assertThat(units.minutesText).isEqualTo("00")
    }
}
