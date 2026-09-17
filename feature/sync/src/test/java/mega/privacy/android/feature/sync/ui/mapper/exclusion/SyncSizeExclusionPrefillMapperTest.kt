package mega.privacy.android.feature.sync.ui.mapper.exclusion

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncSizeExclusionPrefillMapperTest {

    private val underTest = SyncSizeExclusionPrefillMapper()

    @Test
    fun `test that invoke opens an empty form when the file declares no bound`() {
        val actual = underTest(SyncSizeExclusion())

        assertThat(actual.condition).isNull()
        assertThat(actual.showsBounds).isFalse()
        assertThat(actual.firstBound.isEmpty).isTrue()
        assertThat(actual.secondBound.isEmpty).isTrue()
    }

    @Test
    fun `test that invoke prefills bigger than from the larger bound alone`() {
        val actual = underTest(SyncSizeExclusion(excludeLargerThanBytes = 100 * 1024 * 1024))

        assertThat(actual.condition).isEqualTo(SyncSizeCondition.BiggerThan)
        assertThat(actual.firstBound.value).isEqualTo("100")
        assertThat(actual.firstBound.unit).isEqualTo(SyncSizeUnit.Megabytes)
        assertThat(actual.showsSecondBound).isFalse()
    }

    @Test
    fun `test that invoke prefills smaller than from the smaller bound alone`() {
        val actual = underTest(SyncSizeExclusion(excludeSmallerThanBytes = 2 * 1024))

        assertThat(actual.condition).isEqualTo(SyncSizeCondition.SmallerThan)
        assertThat(actual.firstBound.value).isEqualTo("2")
        assertThat(actual.firstBound.unit).isEqualTo(SyncSizeUnit.Kilobytes)
        assertThat(actual.showsSecondBound).isFalse()
    }

    @Test
    fun `test that invoke prefills not between with the smaller bound first`() {
        val actual = underTest(
            SyncSizeExclusion(
                excludeSmallerThanBytes = 1024,
                excludeLargerThanBytes = 2L * 1024 * 1024 * 1024,
            )
        )

        assertThat(actual.condition).isEqualTo(SyncSizeCondition.NotBetween)
        assertThat(actual.firstBound.value).isEqualTo("1")
        assertThat(actual.firstBound.unit).isEqualTo(SyncSizeUnit.Kilobytes)
        assertThat(actual.secondBound.value).isEqualTo("2")
        assertThat(actual.secondBound.unit).isEqualTo(SyncSizeUnit.Gigabytes)
        assertThat(actual.showsRangeError).isFalse()
    }

    @ParameterizedTest(name = "{0} bytes reads back as {1} {2}")
    @MethodSource("bounds")
    fun `test that invoke reads a bound in the largest unit dividing it exactly`(
        bytes: Long,
        expectedValue: String,
        expectedUnit: SyncSizeUnit,
    ) {
        val actual = underTest(SyncSizeExclusion(excludeLargerThanBytes = bytes)).firstBound

        assertThat(actual.value).isEqualTo(expectedValue)
        assertThat(actual.unit).isEqualTo(expectedUnit)
        assertThat(actual.unit.toBytes(actual.value.toLong())).isEqualTo(bytes)
    }

    private fun bounds(): Stream<Arguments> = Stream.of(
        Arguments.of(0L, "0", SyncSizeUnit.Bytes),
        Arguments.of(1L, "1", SyncSizeUnit.Bytes),
        Arguments.of(1023L, "1023", SyncSizeUnit.Bytes),
        Arguments.of(1024L, "1", SyncSizeUnit.Kilobytes),
        Arguments.of(1536L, "1536", SyncSizeUnit.Bytes),
        Arguments.of(1024L * 1024, "1", SyncSizeUnit.Megabytes),
        Arguments.of(1024L * 1024 * 1024, "1", SyncSizeUnit.Gigabytes),
        Arguments.of(3L * 1024 * 1024 * 1024, "3", SyncSizeUnit.Gigabytes),
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE.toString(), SyncSizeUnit.Bytes),
    )

    @Test
    fun `test that invoke reports a pair leaving out every file rather than repairing it`() {
        val actual = underTest(
            SyncSizeExclusion(excludeSmallerThanBytes = 1024, excludeLargerThanBytes = 512),
        )

        assertThat(actual.condition).isEqualTo(SyncSizeCondition.NotBetween)
        assertThat(actual.firstBound.value).isEqualTo("1")
        assertThat(actual.secondBound.value).isEqualTo("512")
        assertThat(actual.showsRangeError).isTrue()
    }

    @Test
    fun `test that invoke keeps a zero bound the form cannot express`() {
        val actual = underTest(SyncSizeExclusion(excludeLargerThanBytes = 0))

        assertThat(actual.firstBound.value).isEqualTo("0")
        assertThat(actual.hasUnusableBound).isTrue()
    }
}
