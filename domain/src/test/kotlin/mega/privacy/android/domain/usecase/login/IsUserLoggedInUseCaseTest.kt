package mega.privacy.android.domain.usecase.login

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
 * Test class for [IsUserLoggedInUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsUserLoggedInUseCaseTest {

    private lateinit var underTest: IsUserLoggedInUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsUserLoggedInUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that the user is logged in`() = runTest {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        assertThat(underTest.invoke()).isTrue()
    }

    @Test
    fun `test that the user is not logged in`() = runTest {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
        assertThat(underTest.invoke()).isFalse()
    }
}