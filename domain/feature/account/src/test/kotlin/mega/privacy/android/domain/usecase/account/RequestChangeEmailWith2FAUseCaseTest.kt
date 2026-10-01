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
internal class RequestChangeEmailWith2FAUseCaseTest {
    private lateinit var underTest: RequestChangeEmailWith2FAUseCase
    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RequestChangeEmailWith2FAUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that invoke forwards the email and pin to the repository`() = runTest {
        val email = "new@mega.nz"
        val pin = "123456"
        underTest(email, pin)
        verify(accountRepository).requestChangeEmailWith2FA(eq(email), eq(pin))
    }
}
