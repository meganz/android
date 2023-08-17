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
class TransferStateIntMapperTest {

    private lateinit var underTest: TransferStateIntMapper

    @BeforeAll
    fun setup() {
        underTest = TransferStateIntMapper()
    }

    @ParameterizedTest(name = "invoked with {0} and returns {1}")
    @MethodSource("provideParameters")
    fun `test that transfer state int mapper maps correctly`(
        transferState: TransferState,
        transferStateInt: Int,
    ) {
        val actual = underTest(transferState)
        Truth.assertThat(actual).isEqualTo(transferStateInt)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            TransferState.STATE_NONE,
            MegaTransfer.STATE_NONE,
        ),
        Arguments.of(
            TransferState.STATE_QUEUED,
            MegaTransfer.STATE_QUEUED,
        ),
        Arguments.of(
            TransferState.STATE_ACTIVE,
            MegaTransfer.STATE_ACTIVE,
        ),
        Arguments.of(
            TransferState.STATE_PAUSED,
            MegaTransfer.STATE_PAUSED,
        ),
        Arguments.of(
            TransferState.STATE_RETRYING,
            MegaTransfer.STATE_RETRYING,
        ),
        Arguments.of(
            TransferState.STATE_COMPLETING,
            MegaTransfer.STATE_COMPLETING,
        ),
        Arguments.of(
            TransferState.STATE_COMPLETED,
            MegaTransfer.STATE_COMPLETED,
        ),
        Arguments.of(
            TransferState.STATE_CANCELLED,
            MegaTransfer.STATE_CANCELLED,
        ),
        Arguments.of(
            TransferState.STATE_FAILED,
            MegaTransfer.STATE_FAILED,
        ),
    )
}
