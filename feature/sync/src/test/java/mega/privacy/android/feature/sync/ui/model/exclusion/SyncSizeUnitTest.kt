package mega.privacy.android.feature.sync.ui.model.exclusion

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SyncSizeUnitTest {

    @Test
    fun `test that toBytes scales by the binary factor the SDK uses`() {
        assertThat(SyncSizeUnit.Bytes.toBytes(1)).isEqualTo(1L)
        assertThat(SyncSizeUnit.Kilobytes.toBytes(1)).isEqualTo(1_024L)
        assertThat(SyncSizeUnit.Megabytes.toBytes(100)).isEqualTo(104_857_600L)
        assertThat(SyncSizeUnit.Gigabytes.toBytes(2)).isEqualTo(2_147_483_648L)
    }

    @ParameterizedTest
    @EnumSource(SyncSizeUnit::class)
    fun `test that toBytes holds the largest value the unit can express`(unit: SyncSizeUnit) {
        val largest = Long.MAX_VALUE / unit.multiplierBytes

        assertThat(unit.toBytes(largest)).isEqualTo(largest * unit.multiplierBytes)
    }

    @ParameterizedTest
    @EnumSource(SyncSizeUnit::class)
    fun `test that toBytes returns null one step past the largest value`(unit: SyncSizeUnit) {
        val pastLargest = Long.MAX_VALUE / unit.multiplierBytes + 1

        assertThat(unit.toBytes(pastLargest)).isNull()
    }

    @ParameterizedTest
    @EnumSource(
        value = SyncSizeUnit::class,
        names = ["Kilobytes", "Megabytes", "Gigabytes"],
    )
    fun `test that toBytes returns null when a scaled value would not fit a Long`(
        unit: SyncSizeUnit,
    ) {
        assertThat(unit.toBytes(Long.MAX_VALUE)).isNull()
    }

    @ParameterizedTest
    @EnumSource(SyncSizeUnit::class)
    fun `test that toBytes refuses a negative value rather than scaling it`(unit: SyncSizeUnit) {
        assertThat(unit.toBytes(-1)).isNull()
        assertThat(unit.toBytes(Long.MIN_VALUE)).isNull()
    }
}
