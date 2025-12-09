package mega.privacy.android.feature.myaccount.presentation.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuotaLevelMapperTest {

    private val underTest = QuotaLevelMapper()

    @ParameterizedTest(name = "usedPercentage: {0}, storageState: {1} -> expected: {2}")
    @MethodSource("quotaLevelMappings")
    fun `test quota level mapping`(
        usedPercentage: Int,
        storageState: StorageState?,
        expectedQuotaLevel: QuotaLevel,
    ) {
        val result = underTest.invoke(usedPercentage, storageState)
        assertThat(result).isEqualTo(expectedQuotaLevel)
    }

    @Test
    fun `test null storage state with low percentage returns success`() {
        val result = underTest.invoke(50, null)
        assertThat(result).isEqualTo(QuotaLevel.Success)
    }

    @Test
    fun `test null storage state with high percentage returns correct level`() {
        val result = underTest.invoke(85, null)
        assertThat(result).isEqualTo(QuotaLevel.Warning)
    }

    @Test
    fun `test null storage state with very high percentage returns error`() {
        val result = underTest.invoke(95, null)
        assertThat(result).isEqualTo(QuotaLevel.Error)
    }

    @Test
    fun `test red storage state overrides percentage for error level`() {
        val result = underTest.invoke(50, StorageState.Red)
        assertThat(result).isEqualTo(QuotaLevel.Error)
    }

    @Test
    fun `test orange storage state overrides percentage for warning level`() {
        val result = underTest.invoke(50, StorageState.Orange)
        assertThat(result).isEqualTo(QuotaLevel.Warning)
    }

    @Test
    fun `test edge cases for percentage thresholds`() {
        // Just below warning threshold
        val result79 = underTest.invoke(79, StorageState.Green)
        assertThat(result79).isEqualTo(QuotaLevel.Success)

        // Exactly at warning threshold
        val result80 = underTest.invoke(80, StorageState.Green)
        assertThat(result80).isEqualTo(QuotaLevel.Warning)

        // Just below error threshold
        val result89 = underTest.invoke(89, StorageState.Green)
        assertThat(result89).isEqualTo(QuotaLevel.Warning)

        // Exactly at error threshold
        val result90 = underTest.invoke(90, StorageState.Green)
        assertThat(result90).isEqualTo(QuotaLevel.Error)
    }

    companion object {
        @JvmStatic
        fun quotaLevelMappings(): Stream<Arguments> = Stream.of(
            // Success cases - low percentages with green or no storage state
            Arguments.of(0, StorageState.Green, QuotaLevel.Success),
            Arguments.of(50, StorageState.Green, QuotaLevel.Success),
            Arguments.of(79, StorageState.Green, QuotaLevel.Success),
            Arguments.of(50, null, QuotaLevel.Success),
            Arguments.of(79, null, QuotaLevel.Success),

            // Warning cases - percentage between 80-89 or orange storage state
            Arguments.of(80, StorageState.Green, QuotaLevel.Warning),
            Arguments.of(85, StorageState.Green, QuotaLevel.Warning),
            Arguments.of(89, StorageState.Green, QuotaLevel.Warning),
            Arguments.of(80, null, QuotaLevel.Warning),
            Arguments.of(85, null, QuotaLevel.Warning),
            Arguments.of(50, StorageState.Orange, QuotaLevel.Warning),
            Arguments.of(85, StorageState.Orange, QuotaLevel.Warning),

            // Error cases - percentage >= 90 or red storage state
            Arguments.of(90, StorageState.Green, QuotaLevel.Error),
            Arguments.of(95, StorageState.Green, QuotaLevel.Error),
            Arguments.of(100, StorageState.Green, QuotaLevel.Error),
            Arguments.of(90, null, QuotaLevel.Error),
            Arguments.of(95, null, QuotaLevel.Error),
            Arguments.of(50, StorageState.Red, QuotaLevel.Error),
            Arguments.of(95, StorageState.Red, QuotaLevel.Error),
        )
    }
}
