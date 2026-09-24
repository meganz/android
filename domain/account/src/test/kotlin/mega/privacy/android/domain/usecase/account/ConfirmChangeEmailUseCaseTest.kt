package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.account.ConfirmChangeEmailException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [ConfirmChangeEmailUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConfirmChangeEmailUseCaseTest {
    private lateinit var underTest: ConfirmChangeEmailUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ConfirmChangeEmailUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }

    @Test
    fun `test that the new email address is returned when the use case is invoked`() = runTest {
        val expectedEmail = "newemail@gmail.com"

        whenever(
            accountRepository.confirmChangeEmail(
                changeEmailLink = any(),
                accountPassword = any(),
            )
        ).thenReturn(expectedEmail)

        val actualEmail = underTest.invoke(
            changeEmailLink = "change/email/link",
            accountPassword = "accountPassword",
        )
        assertThat(actualEmail).isEqualTo(expectedEmail)
    }

    @Test
    fun `test that an email already in use exception is thrown`() = runTest {
        whenever(
            accountRepository.confirmChangeEmail(
                changeEmailLink = any(),
                accountPassword = any(),
            )
        ).thenAnswer {
            throw ConfirmChangeEmailException.EmailAlreadyInUse(
                errorCode = 20,
                errorString = "The email is already in use",
            )
        }
        assertThrows<ConfirmChangeEmailException.EmailAlreadyInUse> {
            underTest.invoke(
                changeEmailLink = "change/email/link",
                accountPassword = "accountPassword",
            )
        }
    }

    @Test
    fun `test that an incorrect password exception is thrown`() = runTest {
        whenever(
            accountRepository.confirmChangeEmail(
                changeEmailLink = any(),
                accountPassword = any(),
            )
        ).thenAnswer {
            throw ConfirmChangeEmailException.IncorrectPassword(
                errorCode = 25,
                errorString = "Incorrect password",
            )
        }
        assertThrows<ConfirmChangeEmailException.IncorrectPassword> {
            underTest.invoke(
                changeEmailLink = "change/email/link",
                accountPassword = "accountPassword",
            )
        }
    }

    @Test
    fun `test that an unknown exception is thrown`() = runTest {
        whenever(
            accountRepository.confirmChangeEmail(
                changeEmailLink = any(),
                accountPassword = any(),
            )
        ).thenAnswer {
            throw ConfirmChangeEmailException.IncorrectPassword(
                errorCode = 25,
                errorString = "Incorrect password",
            )
        }
        assertThrows<ConfirmChangeEmailException.IncorrectPassword> {
            underTest.invoke(
                changeEmailLink = "change/email/link",
                accountPassword = "accountPassword",
            )
        }
    }
}