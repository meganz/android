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
class TransferTypeIntMapperTest {
    private lateinit var underTest: TransferTypeIntMapper

    @BeforeAll
    fun setup() {
        underTest = TransferTypeIntMapper()
    }

    @ParameterizedTest(name = "invoked with {0} and returns {1}")
    @MethodSource("provideParameters")
    fun `test that transfer type int mapper maps correctly`(
        transferType: TransferType,
        transferInt: Int,
    ) {
        val actual = underTest(transferType)
        Truth.assertThat(actual).isEqualTo(transferInt)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            TransferType.DOWNLOAD,
            MegaTransfer.TYPE_DOWNLOAD,
        ),
        Arguments.of(
            TransferType.GENERAL_UPLOAD,
            MegaTransfer.TYPE_UPLOAD,
        ),
        Arguments.of(
            TransferType.CAMERA_UPLOAD,
            MegaTransfer.TYPE_UPLOAD,
        ),
        Arguments.of(
            TransferType.CHAT_UPLOAD,
            MegaTransfer.TYPE_UPLOAD,
        ),
        Arguments.of(
            TransferType.NONE,
            -1,
        ),
    )
}
