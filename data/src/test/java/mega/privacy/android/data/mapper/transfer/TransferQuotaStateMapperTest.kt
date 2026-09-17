package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferQuotaStateMapperTest {
    private lateinit var underTest: TransferQuotaStateMapper

    @BeforeAll
    fun setup() {
        underTest = TransferQuotaStateMapper()
    }

    @ParameterizedTest(name = "invoked with {1} and returns {0}")
    @MethodSource("provideParameters")
    fun `test that transfer quota state mapper maps correctly`(
        expected: TransferQuotaState,
        transferQuotaStateInt: Int,
    ) {
        val actual = underTest(transferQuotaStateInt)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that an unrecognised value maps to Unknown`() {
        Truth.assertThat(underTest(Int.MAX_VALUE)).isEqualTo(TransferQuotaState.Unknown)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            TransferQuotaState.Unknown,
            MegaApiJava.TRANSFER_QUOTA_STATE_UNKNOWN,
        ),
        Arguments.of(
            TransferQuotaState.Green,
            MegaApiJava.TRANSFER_QUOTA_STATE_GREEN,
        ),
        Arguments.of(
            TransferQuotaState.Orange,
            MegaApiJava.TRANSFER_QUOTA_STATE_ORANGE,
        ),
        Arguments.of(
            TransferQuotaState.Red,
            MegaApiJava.TRANSFER_QUOTA_STATE_RED,
        ),
    )
}
