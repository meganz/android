package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.account.ConfirmCancelAccountException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [ConfirmCancelAccountUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConfirmCancelAccountUseCaseTest {
    private lateinit var underTest: ConfirmCancelAccountUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ConfirmCancelAccountUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that confirm cancel account is invoked`() = runTest {
        val cancellationLink = "cancellation/link"
        val accountPassword = "accountPassword"
        underTest.invoke(
            cancellationLink = cancellationLink,
            accountPassword = accountPassword,
        )
        verify(accountRepository).confirmCancelAccount(
            cancellationLink = cancellationLink,
            accountPassword = accountPassword,
        )
    }

    @Test
    fun `test that an incorrect password exception is thrown when the repository throws an incorrect password exception`() =
        runTest {
            whenever(
                accountRepository.confirmCancelAccount(
                    cancellationLink = "cancellation/link",
                    accountPassword = "accountPassword",
                )
            ).thenAnswer {
                throw ConfirmCancelAccountException.IncorrectPassword(
                    errorCode = 56,
                    errorString = "Incorrect password",
                )
            }
            assertThrows<ConfirmCancelAccountException.IncorrectPassword> {
                underTest.invoke(
                    cancellationLink = "cancellation/link",
                    accountPassword = "accountPassword",
                )
            }
        }

    @Test
    fun `test that an unknown exception is thrown when the repository throws an unknown exception`() =
        runTest {
            whenever(
                accountRepository.confirmCancelAccount(
                    cancellationLink = "cancellation/link",
                    accountPassword = "accountPassword",
                )
            ).thenAnswer {
                throw ConfirmCancelAccountException.Unknown(
                    errorCode = 60,
                    errorString = "An unexpected error occurred",
                )
            }
            assertThrows<ConfirmCancelAccountException.Unknown> {
                underTest.invoke(
                    cancellationLink = "cancellation/link",
                    accountPassword = "accountPassword",
                )
            }
        }
}