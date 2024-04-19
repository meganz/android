package mega.privacy.android.app.presentation.login.confirmemail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.account.CancelCreateAccountUseCase
import mega.privacy.android.domain.usecase.createaccount.MonitorAccountConfirmationUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfirmEmailViewModelTest {

    private val monitorAccountConfirmationUseCase: MonitorAccountConfirmationUseCase = mock()
    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase = mock()
    private val cancelCreateAccountUseCase: CancelCreateAccountUseCase = mock()
    private val snackBarHandler: SnackBarHandler = mock()

    private lateinit var underTest: ConfirmEmailViewModel

    private val email = "test@test.com"

    @BeforeEach
    fun setUp() = runTest {
        whenever(monitorAccountConfirmationUseCase()).thenReturn(flowOf(false))

        underTest = ConfirmEmailViewModel(
            monitorAccountConfirmationUseCase = monitorAccountConfirmationUseCase,
            resendSignUpLinkUseCase = resendSignUpLinkUseCase,
            cancelCreateAccountUseCase = cancelCreateAccountUseCase,
            snackBarHandler = snackBarHandler
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorAccountConfirmationUseCase,
            resendSignUpLinkUseCase,
            cancelCreateAccountUseCase,
            snackBarHandler
        )
    }

    @Test
    fun `test that the registered email is updated after successfully resending the sign up link`() =
        runTest {
            val fullName = "fullName"
            whenever(resendSignUpLinkUseCase(email = email, fullName = fullName)) doReturn email

            underTest.resendSignUpLink(email = email, fullName = fullName)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().registeredEmail).isEqualTo(email)
            }
        }

    @Test
    fun `test that the success snack bar is shown after successfully resending the sign up link`() =
        runTest {
            val fullName = "fullName"
            whenever(resendSignUpLinkUseCase(email = email, fullName = fullName)) doReturn email

            underTest.resendSignUpLink(email = email, fullName = fullName)

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.confirm_email_misspelled_email_sent,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }

    @Test
    fun `test that the error snack bar is shown when fails to resend the sign up link`() =
        runTest {
            val fullName = "fullName"
            val errorMessage = "errorMessage"
            whenever(
                resendSignUpLinkUseCase(
                    email = email,
                    fullName = fullName
                )
            ) doThrow MegaException(
                errorCode = 0,
                errorString = errorMessage
            )

            underTest.resendSignUpLink(email = email, fullName = fullName)

            verify(snackBarHandler).postSnackbarMessage(
                message = errorMessage,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }

    @Test
    fun `test that the registered email is updated after successfully cancelling the registration process`() =
        runTest {
            whenever(cancelCreateAccountUseCase()) doReturn email

            underTest.cancelCreateAccount()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().registeredEmail).isEqualTo(email)
            }
        }

    @Test
    fun `test that the success snack bar is shown after successfully cancelling the registration process`() =
        runTest {
            whenever(cancelCreateAccountUseCase()) doReturn email

            underTest.cancelCreateAccount()

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.confirm_email_misspelled_email_sent,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }

    @Test
    fun `test that the error snack bar is shown when fails to cancel the registration process`() =
        runTest {
            val errorMessage = "errorMessage"
            whenever(cancelCreateAccountUseCase()) doThrow MegaException(
                errorCode = 0,
                errorString = errorMessage
            )

            underTest.cancelCreateAccount()

            verify(snackBarHandler).postSnackbarMessage(
                message = errorMessage,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }
}
