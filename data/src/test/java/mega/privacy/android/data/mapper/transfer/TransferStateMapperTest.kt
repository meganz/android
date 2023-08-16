package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.TransferState
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferStateMapperTest {

    private lateinit var underTest: TransferStateMapper

    @BeforeAll
    fun setup() {
        underTest = TransferStateMapper()
    }

    @ParameterizedTest(name = "invoked with {0} and returns {1}")
    @MethodSource("provideParameters")
    fun `test that transfer state mapper maps correctly`(
        transferStateInt: Int,
        transferState: TransferState,
    ) {
        val actual = underTest(transferStateInt)
        Truth.assertThat(actual).isEqualTo(transferState)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            MegaTransfer.STATE_NONE,
            TransferState.STATE_NONE
        ),
        Arguments.of(
            MegaTransfer.STATE_QUEUED,
            TransferState.STATE_QUEUED
        ),
        Arguments.of(
            MegaTransfer.STATE_ACTIVE,
            TransferState.STATE_ACTIVE
        ),
        Arguments.of(
            MegaTransfer.STATE_PAUSED,
            TransferState.STATE_PAUSED
        ),
        Arguments.of(
            MegaTransfer.STATE_RETRYING,
            TransferState.STATE_RETRYING
        ),
        Arguments.of(
            MegaTransfer.STATE_COMPLETING,
            TransferState.STATE_COMPLETING
        ),
        Arguments.of(
            MegaTransfer.STATE_COMPLETED,
            TransferState.STATE_COMPLETED
        ),
        Arguments.of(
            MegaTransfer.STATE_CANCELLED,
            TransferState.STATE_CANCELLED
        ),
        Arguments.of(
            MegaTransfer.STATE_FAILED,
            TransferState.STATE_FAILED
        ),
        Arguments.of(
            -1,
            TransferState.STATE_NONE
        ),
    )
}
