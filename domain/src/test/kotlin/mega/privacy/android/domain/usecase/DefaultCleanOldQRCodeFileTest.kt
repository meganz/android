package mega.privacy.android.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCleanOldQRCodeFileTest {

    private lateinit var underTest: DefaultCleanOldQRCodeFile
    private val accountRepository: AccountRepository = mock()
    private val qrCodeRepository: QRCodeRepository = mock()
    private val testCoroutineDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest = DefaultCleanOldQRCodeFile(
            accountRepository = accountRepository,
            qrCodeRepository = qrCodeRepository,
            ioDispatcher = testCoroutineDispatcher,
        )
    }

    @Test
    fun `test that repository methods are called for clean old QR code file`() = runTest {
        val myEmail = "myEmail"
        val oldQRImageFileName = "QRcode.jpg"
        whenever(accountRepository.accountEmail).thenReturn(myEmail)
        underTest()
        verify(accountRepository).accountEmail
        verify(qrCodeRepository).getQRFile(myEmail + oldQRImageFileName)
    }

    @Test
    fun `test that QRCodeRepository getQRFile is not invoked if account email is null`() = runTest {
        whenever(accountRepository.accountEmail).thenReturn(null)
        underTest()
        verify(accountRepository).accountEmail
        verify(qrCodeRepository, never()).getQRFile(any())
    }
}