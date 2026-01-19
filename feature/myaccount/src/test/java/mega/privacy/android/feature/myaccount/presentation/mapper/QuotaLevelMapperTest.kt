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

    @ParameterizedTest(name = "storageState: {0} -> expected: {1}")
    @MethodSource("quotaLevelMappings")
    fun `test quota level mapping`(
        storageState: StorageState?,
        expectedQuotaLevel: QuotaLevel,
    ) {
        val result = underTest.invoke(storageState)
        assertThat(result).isEqualTo(expectedQuotaLevel)
    }

    @Test
    fun `test null storage state returns success`() {
        val result = underTest.invoke(null)
        assertThat(result).isEqualTo(QuotaLevel.Success)
    }

    @Test
    fun `test red storage state returns error`() {
        val result = underTest.invoke(StorageState.Red)
        assertThat(result).isEqualTo(QuotaLevel.Error)
    }

    @Test
    fun `test orange storage state returns warning`() {
        val result = underTest.invoke(StorageState.Orange)
        assertThat(result).isEqualTo(QuotaLevel.Warning)
    }

    @Test
    fun `test green storage state returns success`() {
        val result = underTest.invoke(StorageState.Green)
        assertThat(result).isEqualTo(QuotaLevel.Success)
    }

    @Test
    fun `test unknown storage state returns success`() {
        val result = underTest.invoke(StorageState.Unknown)
        assertThat(result).isEqualTo(QuotaLevel.Success)
    }

    @Test
    fun `test change storage state returns success`() {
        val result = underTest.invoke(StorageState.Change)
        assertThat(result).isEqualTo(QuotaLevel.Success)
    }

    @Test
    fun `test paywall storage state returns success`() {
        val result = underTest.invoke(StorageState.PayWall)
        assertThat(result).isEqualTo(QuotaLevel.Success)
    }

    companion object {
        @JvmStatic
        fun quotaLevelMappings(): Stream<Arguments> = Stream.of(
            // Success cases
            Arguments.of(null, QuotaLevel.Success),
            Arguments.of(StorageState.Unknown, QuotaLevel.Success),
            Arguments.of(StorageState.Green, QuotaLevel.Success),
            Arguments.of(StorageState.Change, QuotaLevel.Success),
            Arguments.of(StorageState.PayWall, QuotaLevel.Success),

            // Warning cases
            Arguments.of(StorageState.Orange, QuotaLevel.Warning),

            // Error cases
            Arguments.of(StorageState.Red, QuotaLevel.Error),
        )
    }
}
