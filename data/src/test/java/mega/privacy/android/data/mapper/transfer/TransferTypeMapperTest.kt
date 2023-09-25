package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.TransferAppData
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

    @ParameterizedTest(name = "invoked with {0} and {2}: returns {1}")
    @MethodSource("provideParameters")
    fun `test that transfer type mapper returns correctly`(
        transferInt: Int,
        transferType: TransferType,
        appData: List<TransferAppData>,
    ) {
        val actual = underTest(transferInt, appData)
        Truth.assertThat(actual).isEqualTo(transferType)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            MegaTransfer.TYPE_DOWNLOAD,
            TransferType.DOWNLOAD,
            emptyList<TransferAppData>(),
        ),
        Arguments.of(
            MegaTransfer.TYPE_UPLOAD,
            TransferType.GENERAL_UPLOAD,
            emptyList<TransferAppData>(),
        ),
        Arguments.of(
            MegaTransfer.TYPE_UPLOAD,
            TransferType.CAMERA_UPLOAD,
            listOf(TransferAppData.CameraUpload),
        ),
        Arguments.of(
            MegaTransfer.TYPE_UPLOAD,
            TransferType.CHAT_UPLOAD,
            listOf(TransferAppData.ChatUpload(1L)),
        ),
        Arguments.of(
            -1,
            TransferType.NONE,
            emptyList<TransferAppData>(),
        ),
    )
}
