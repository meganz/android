package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDeleteQRCodeTest {

    private lateinit var underTest: DefaultDeleteQRCode

    private val accountRepository: AccountRepository = mock()
    private val getQRFile: GetQRFile = mock()

    @Before
    fun setup() {
        underTest = DefaultDeleteQRCode(
            accountRepository = accountRepository,
            getQRFile = getQRFile,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that QR code can be deleted successfully`() = runTest {
        val handle = 100L
        val qrFileName = "tester@mega.co.nzQR_code_image.jpg"

        underTest(handle, qrFileName)
        verify(accountRepository).deleteContactLink(handle)
        verify(getQRFile).invoke(qrFileName)
    }

    @Test(expected = MegaException::class)
    fun `test that exception is thrown if MegaApi to delete accountLink fails`() = runTest {
        val handle = 1000L
        val qrFileName = "tester@mega.co.nzQR_code_image.jpg"
        whenever(accountRepository.deleteContactLink(handle)).thenAnswer {
            throw MegaException(
                errorCode = -1,
                errorString = "error")
        }

        underTest(handle, qrFileName)
    }

}