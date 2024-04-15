package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CancelCreateAccountUseCaseTest {

    private lateinit var underTest: CancelCreateAccountUseCase

    private val accountRepository: AccountRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = CancelCreateAccountUseCase(accountRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(accountRepository)
    }

    @Test
    fun `test that the repository is called`() = runTest {
        underTest()

        verify(accountRepository).cancelCreateAccount()
    }
}
