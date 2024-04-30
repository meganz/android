package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AreCredentialsNullUseCaseTest {
    private lateinit var underTest: AreCredentialsNullUseCase
    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = AreCredentialsNullUseCase(accountRepository = accountRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(accountRepository)
    }

    @Test
    fun `test that false is returned`() =
        runTest {
            val userCredentials = mock<UserCredentials>()
            whenever(accountRepository.getAccountCredentials()).thenReturn(userCredentials)
            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that true is returned`() =
        runTest {
            whenever(accountRepository.getAccountCredentials()).thenReturn(null)
            assertThat(underTest()).isTrue()
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest()
            verify(accountRepository).getAccountCredentials()
        }
}