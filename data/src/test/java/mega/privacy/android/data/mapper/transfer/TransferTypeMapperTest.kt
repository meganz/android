package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferTypeMapperTest {
    private lateinit var underTest: TransferTypeMapper

    @BeforeAll
    fun setup() {
        underTest = TransferTypeMapper()
    }

    @ParameterizedTest(name = "invoked with {0} and returns {1}")
    @MethodSource("provideParameters")
    fun `test that transfer type mapper returns correctly`(
        transferInt: Int,
        transferType: TransferType,
    ) {
        val actual = underTest(transferInt)
        Truth.assertThat(actual).isEqualTo(transferType)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            MegaTransfer.TYPE_DOWNLOAD,
            TransferType.TYPE_DOWNLOAD
        ),
        Arguments.of(
            MegaTransfer.TYPE_UPLOAD,
            TransferType.TYPE_UPLOAD
        ),
        Arguments.of(
            -1,
            TransferType.NONE
        ),
    )
}
