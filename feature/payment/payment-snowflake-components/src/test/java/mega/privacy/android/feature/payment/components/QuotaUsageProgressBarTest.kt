package mega.privacy.android.feature.payment.components

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class QuotaUsageProgressBarTest {

    @ParameterizedTest
    @EnumSource(QuotaUsageLevel::class)
    fun `test that escalateIfFull returns Error when the bar is full`(level: QuotaUsageLevel) {
        assertThat(level.escalateIfFull(100f)).isEqualTo(QuotaUsageLevel.Error)
    }

    @ParameterizedTest
    @EnumSource(QuotaUsageLevel::class)
    fun `test that escalateIfFull returns Error when the bar is over full`(level: QuotaUsageLevel) {
        assertThat(level.escalateIfFull(120f)).isEqualTo(QuotaUsageLevel.Error)
    }

    @ParameterizedTest
    @EnumSource(QuotaUsageLevel::class)
    fun `test that escalateIfFull keeps the level when the bar is not full`(level: QuotaUsageLevel) {
        assertThat(level.escalateIfFull(99f)).isEqualTo(level)
    }

    @Test
    fun `test that escalateIfFull keeps the level when the bar is empty`() {
        assertThat(QuotaUsageLevel.Warning.escalateIfFull(0f)).isEqualTo(QuotaUsageLevel.Warning)
    }
}
