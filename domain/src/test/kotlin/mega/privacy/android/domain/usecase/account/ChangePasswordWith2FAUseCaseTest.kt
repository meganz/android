package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChangePasswordWith2FAUseCaseTest {
    private lateinit var underTest: ChangePasswordWith2FAUseCase
    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ChangePasswordWith2FAUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that invoke forwards the new password and pin and returns the repository result`() =
        runTest {
            val password = "new-pass"
            val pin = "123456"
            whenever(accountRepository.changePasswordWith2FA(eq(password), eq(pin)))
                .thenReturn(true)

            val result = underTest(password, pin)

            assertThat(result).isTrue()
        }
}
