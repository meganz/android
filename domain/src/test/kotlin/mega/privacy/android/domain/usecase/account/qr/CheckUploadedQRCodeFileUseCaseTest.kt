package mega.privacy.android.domain.usecase.account.qr

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.QRCodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckUploadedQRCodeFileUseCaseTest {

    private lateinit var underTest: CheckUploadedQRCodeFileUseCase

    private val qrCodeRepository: QRCodeRepository = mock()
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = CheckUploadedQRCodeFileUseCase(
            qrCodeRepository = qrCodeRepository,
            getQRCodeFileUseCase = getQRCodeFileUseCase,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            qrCodeRepository,
            getQRCodeFileUseCase,
        )
    }

    @ParameterizedTest(
        name = "when qrCodeFile is {0}, uploadFileName is {1}, uploadFile is {2} " +
                "and the file should be deleted: {3}"
    )
    @MethodSource("provideParameters")
    fun `test that the uploaded file is or not deleted`(
        qrCodeFile: File?,
        uploadFileName: String,
        uploadFile: File?,
        expectedResult: Boolean,
    ) = runTest {
        whenever(getQRCodeFileUseCase()).thenReturn(qrCodeFile)
        whenever(qrCodeRepository.getQRFile(uploadFileName)).thenReturn(uploadFile)
        assertEquals(underTest(uploadFileName), expectedResult)
    }

    companion object {
        private const val qrCodeFileName = "test@mega.nzQR_code_image.jpg"
        private val qrCodeFile = getMockedFile(qrCodeFileName)
        private const val transferFileName = "testFile.jpg"
        private val transferFile = getMockedFile(transferFileName)

        private fun getMockedFile(fileName: String) = mock<File> {
            on { name }.thenReturn(fileName)
            on { exists() }.thenReturn(true)
            on { delete() }.thenReturn(true)
        }

        @JvmStatic
        private fun provideParameters(): Stream<Arguments?>? =
            Stream.of(
                Arguments.of(null, qrCodeFileName, null, false),
                Arguments.of(null, transferFileName, transferFile, true),
                Arguments.of(qrCodeFile, transferFileName, null, false),
                Arguments.of(qrCodeFile, qrCodeFileName, qrCodeFile, false),
                Arguments.of(qrCodeFile, transferFileName, transferFile, true),
            )
    }
}