package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetQRCodeFileTest {

    private lateinit var underTest: DefaultGetQRCodeFile

    private val qrCodeRepository: QRCodeRepository = mock()
    private val accountRepository: AccountRepository = mock()

    @Before
    fun setup() {
        underTest = DefaultGetQRCodeFile(
            qrCodeRepository = qrCodeRepository,
            accountRepository = accountRepository,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that QR code file is null if account repository returns null email`() = runTest {
        whenever(accountRepository.accountEmail).thenReturn(null)
        assertThat(underTest()).isEqualTo(null)
    }

    @Test
    fun `test that QR code file is null if QRCodeRepository getQRFile returns null`() = runTest {
        val email = "a@b.c"
        whenever(accountRepository.accountEmail).thenReturn(email)
        whenever(qrCodeRepository.getQRFile(any())).thenReturn(null)
        assertThat(underTest()).isEqualTo(null)
    }

    @Test
    fun `test that QR code file is returned`() = runTest {
        val email = "a@b.c"
        val expectedFile: File = mock()
        whenever(accountRepository.accountEmail).thenReturn(email)
        whenever(qrCodeRepository.getQRFile(any())).thenReturn(expectedFile)
        assertThat(underTest()).isEqualTo(expectedFile)
    }
}