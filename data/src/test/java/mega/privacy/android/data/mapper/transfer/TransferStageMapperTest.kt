package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.TransferStage
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferStageMapperTest {
    private lateinit var underTest: TransferStageMapper

    @BeforeAll
    fun setup() {
        underTest = TransferStageMapper()
    }

    @ParameterizedTest(name = "invoked with {1} and returns {0}")
    @MethodSource("provideParameters")
    fun `test that transfer state int mapper maps correctly`(
        expected: TransferStage,
        transferStageInt: Int,
    ) {
        val actual = underTest(transferStageInt)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            TransferStage.STAGE_NONE,
            MegaTransfer.STAGE_NONE,
        ),
        Arguments.of(
            TransferStage.STAGE_SCANNING,
            MegaTransfer.STAGE_SCAN,
        ),
        Arguments.of(
            TransferStage.STAGE_TRANSFERRING_FILES,
            MegaTransfer.STAGE_TRANSFERRING_FILES,
        ),
        Arguments.of(
            TransferStage.STAGE_CREATING_TREE,
            MegaTransfer.STAGE_CREATE_TREE,
        ),
    )
}