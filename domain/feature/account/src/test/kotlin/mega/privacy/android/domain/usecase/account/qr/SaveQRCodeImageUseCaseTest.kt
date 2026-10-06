package mega.privacy.android.domain.usecase.account.qr

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.QRCodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveQRCodeImageUseCaseTest {

    private lateinit var underTest: SaveQRCodeImageUseCase

    private val getQRCodeFileUseCase = mock<GetQRCodeFileUseCase>()
    private val qrCodeRepository = mock<QRCodeRepository>()

    private val imageBytes = byteArrayOf(1, 2, 3)

    @BeforeAll
    fun setUp() {
        underTest = SaveQRCodeImageUseCase(
            getQRCodeFileUseCase = getQRCodeFileUseCase,
            qrCodeRepository = qrCodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getQRCodeFileUseCase, qrCodeRepository)
    }

    @Test
    fun `test that invoke writes the image to the QR file and returns it`() = runTest {
        val qrFile = mock<File>()
        whenever(getQRCodeFileUseCase()).thenReturn(qrFile)

        val result = underTest(imageBytes)

        assertThat(result).isEqualTo(qrFile)
        verify(qrCodeRepository).writeQRFile(qrFile, imageBytes)
    }

    @Test
    fun `test that invoke returns null without writing when there is no QR file`() = runTest {
        whenever(getQRCodeFileUseCase()).thenReturn(null)

        val result = underTest(imageBytes)

        assertThat(result).isNull()
        verify(qrCodeRepository, never()).writeQRFile(any(), any())
    }
}
