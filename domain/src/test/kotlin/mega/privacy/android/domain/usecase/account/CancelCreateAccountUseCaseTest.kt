package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
        val email = "test@test.com"
        whenever(accountRepository.cancelCreateAccount()).thenReturn(email)

        val actual = underTest()

        assertThat(actual).isEqualTo(email)
        verify(accountRepository).cancelCreateAccount()
    }
}
