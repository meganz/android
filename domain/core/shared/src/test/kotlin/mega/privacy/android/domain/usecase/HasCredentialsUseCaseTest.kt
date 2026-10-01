package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [HasCredentialsUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HasCredentialsUseCaseTest {

    private lateinit var underTest: HasCredentialsUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = HasCredentialsUseCase(
            accountRepository = accountRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that the correct credentials status is returned true`() = runTest {
        whenever(accountRepository.getAccountCredentials()).thenReturn(mock())
        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that the correct credentials status is returned false`() = runTest {
        whenever(accountRepository.getAccountCredentials()).thenReturn(null)
        assertThat(underTest()).isFalse()
    }
}
