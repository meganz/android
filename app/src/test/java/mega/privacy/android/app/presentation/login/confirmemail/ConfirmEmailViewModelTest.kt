package mega.privacy.android.app.presentation.login.confirmemail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.account.CancelCreateAccountUseCase
import mega.privacy.android.domain.usecase.createaccount.MonitorAccountConfirmationUseCase
import mega.privacy.android.domain.usecase.login.SaveLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
    private val saveLastRegisteredEmailUseCase: SaveLastRegisteredEmailUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()

    private lateinit var underTest: ConfirmEmailViewModel

    private val email = "test@test.com"

    @BeforeEach
    fun setUp() = runTest {
        whenever(monitorAccountConfirmationUseCase()).thenReturn(flowOf(false))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))

        initializeUnderTest()
    }

    private fun initializeUnderTest() {
        underTest = ConfirmEmailViewModel(
            monitorAccountConfirmationUseCase = monitorAccountConfirmationUseCase,
            resendSignUpLinkUseCase = resendSignUpLinkUseCase,
            cancelCreateAccountUseCase = cancelCreateAccountUseCase,
            saveLastRegisteredEmailUseCase = saveLastRegisteredEmailUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorAccountConfirmationUseCase,
            resendSignUpLinkUseCase,
            cancelCreateAccountUseCase,
            saveLastRegisteredEmailUseCase,
            monitorConnectivityUseCase
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the online status is updated correctly`(isOnline: Boolean) = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(isOnline)

        initializeUnderTest()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isOnline).isEqualTo(isOnline)
        }
    }

    @Test
    fun `test that the success message is shown after successfully resending the sign up link`() =
        runTest {
            val fullName = "fullName"
            whenever(resendSignUpLinkUseCase(email = email, fullName = fullName)) doReturn email

            underTest.resendSignUpLink(email = email, fullName = fullName)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldShowSuccessMessage).isTrue()
            }
        }

    @Test
    fun `test that the error message is shown when fails to resend the sign up link`() =
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

            underTest.uiState.test {
                assertThat(expectMostRecentItem().errorMessage).isEqualTo(errorMessage)
            }
        }

    @Test
    fun `test that the registered email is updated after successfully cancelling the registration process`() =
        runTest {
            whenever(cancelCreateAccountUseCase()) doReturn email

            underTest.cancelCreateAccount()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().registeredEmail).isEqualTo(email)
            }
            verify(saveLastRegisteredEmailUseCase).invoke(email)
        }

    @Test
    fun `test that the success message is shown after successfully cancelling the registration process`() =
        runTest {
            whenever(cancelCreateAccountUseCase()) doReturn email

            underTest.cancelCreateAccount()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldShowSuccessMessage).isTrue()
            }
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

            underTest.uiState.test {
                assertThat(expectMostRecentItem().errorMessage).isEqualTo(errorMessage)
            }
        }

    @Test
    fun `test that the success message visibility is reset after being displayed`() = runTest {
        whenever(cancelCreateAccountUseCase()) doReturn email

        underTest.cancelCreateAccount()
        underTest.onSuccessMessageDisplayed()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowSuccessMessage).isFalse()
        }
    }

    @Test
    fun `test that the error message visibility is reset after being displayed`() = runTest {
        val errorMessage = "errorMessage"
        whenever(cancelCreateAccountUseCase()) doThrow MegaException(
            errorCode = 0,
            errorString = errorMessage
        )

        underTest.cancelCreateAccount()
        underTest.onErrorMessageDisplayed()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().errorMessage).isNull()
        }
    }
}
