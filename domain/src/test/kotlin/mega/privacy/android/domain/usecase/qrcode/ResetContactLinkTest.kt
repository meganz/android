package mega.privacy.android.domain.usecase.qrcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ResetContactLinkUseCaseTest {

    private lateinit var underTest: ResetContactLinkUseCase
    private val accountRepository: AccountRepository = mock()

    @Before
    fun setup() {
        underTest = ResetContactLinkUseCase(
            accountRepository = accountRepository,
        )
    }

    @Test
    fun `test that reset QR code works`() = runTest {
        underTest()
        verify(accountRepository).createContactLink(true)
    }

}