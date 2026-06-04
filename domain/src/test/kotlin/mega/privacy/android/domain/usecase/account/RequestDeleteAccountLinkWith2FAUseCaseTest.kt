package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RequestDeleteAccountLinkWith2FAUseCaseTest {
    private lateinit var underTest: RequestDeleteAccountLinkWith2FAUseCase
    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RequestDeleteAccountLinkWith2FAUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that invoke forwards the pin to the repository`() = runTest {
        val pin = "123456"
        underTest(pin)
        verify(accountRepository).requestDeleteAccountLinkWith2FA(eq(pin))
    }
}
