package mega.privacy.android.app.presentation.login.confirmemail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.login.confirmemail.mapper.ResendSignUpLinkErrorMapper
import mega.privacy.android.app.presentation.login.confirmemail.model.ResendSignUpLinkError
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.CancelCreateAccountUseCase
import mega.privacy.android.domain.usecase.createaccount.MonitorAccountConfirmationUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfirmEmailViewModelTest {

    private val monitorAccountConfirmationUseCase: MonitorAccountConfirmationUseCase = mock()
    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase = mock()
    private val cancelCreateAccountUseCase: CancelCreateAccountUseCase = mock()
    private val saveLastRegisteredEmailUseCase: SaveLastRegisteredEmailUseCase = mock()
    private val monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase = mock()
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }

    private val resendSignUpLinkErrorMapper = ResendSignUpLinkErrorMapper()

    private lateinit var underTest: ConfirmEmailViewModel

    private val email = "test@test.com"
    private val fullName = "Test User"

    @BeforeEach
    fun setUp() = runTest {
        whenever(monitorAccountConfirmationUseCase()).thenReturn(flowOf(false))
        whenever(monitorEphemeralCredentialsUseCase()).thenReturn(emptyFlow())

        initializeUnderTest()
    }

    private fun initializeUnderTest() {
        underTest = ConfirmEmailViewModel(
            monitorAccountConfirmationUseCase = monitorAccountConfirmationUseCase,
            resendSignUpLinkUseCase = resendSignUpLinkUseCase,
            cancelCreateAccountUseCase = cancelCreateAccountUseCase,
            saveLastRegisteredEmailUseCase = saveLastRegisteredEmailUseCase,
            monitorEphemeralCredentialsUseCase = monitorEphemeralCredentialsUseCase,
            monitorThemeModeUseCase = monitorThemeModeUseCase,
            resendSignUpLinkErrorMapper = resendSignUpLinkErrorMapper
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorAccountConfirmationUseCase,
            resendSignUpLinkUseCase,
            cancelCreateAccountUseCase,
            saveLastRegisteredEmailUseCase,
        )
    }

    @Test
    fun `test that the registered email is updated after successfully resending the sign up link`() =
        runTest {
            whenever(resendSignUpLinkUseCase(email = email, fullName = fullName)) doReturn email

            underTest.resendSignUpLink(email = email, fullName = fullName)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().registeredEmail).isEqualTo(email)
            }
        }

    @Test
    fun `test that the success message is shown after successfully resending the sign up link`() =
        runTest {
            whenever(resendSignUpLinkUseCase(email = email, fullName = fullName)) doReturn email

            underTest.resendSignUpLink(email = email, fullName = fullName)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldShowSuccessMessage).isTrue()
            }
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCreateAccountException")
    fun `test that the correct resend signup error is triggered for`(error: CreateAccountException) =
        runTest {
            whenever(
                resendSignUpLinkUseCase(
                    email = email,
                    fullName = fullName
                )
            ) doAnswer { throw error }

            underTest.resendSignUpLink(email = email, fullName = fullName)

            underTest.uiState.test {
                val expected = when (error) {
                    is CreateAccountException.AccountAlreadyExists -> {
                        ResendSignUpLinkError.AccountExists
                    }

                    is CreateAccountException.TooManyAttemptsException -> {
                        ResendSignUpLinkError.TooManyAttempts
                    }

                    else -> {
                        ResendSignUpLinkError.Unknown
                    }
                }
                assertThat(expectMostRecentItem().resendSignUpLinkError).isEqualTo(
                    triggered(
                        expected
                    )
                )
            }
        }

    private fun provideCreateAccountException() = Stream.of(
        Arguments.of(CreateAccountException.AccountAlreadyExists),
        Arguments.of(CreateAccountException.TooManyAttemptsException),
        Arguments.of(CreateAccountException.Unknown(MegaException(1, null)))
    )

    @Test
    fun `test that the resend signup link error is successfully consumed`() = runTest {
        whenever(
            resendSignUpLinkUseCase(
                email = email,
                fullName = fullName
            )
        ) doAnswer { throw CreateAccountException.AccountAlreadyExists }

        underTest.resendSignUpLink(email = email, fullName = fullName)
        underTest.onResendSignUpLinkErrorConsumed()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().resendSignUpLinkError).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that isCreatingAccountCancelled is updated after successfully cancelling the registration process`() =
        runTest {
            whenever(cancelCreateAccountUseCase()) doReturn email

            underTest.cancelCreateAccount()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().isCreatingAccountCancelled).isEqualTo(true)
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
                assertThat(expectMostRecentItem().message).isEqualTo(errorMessage)
            }
        }

    @Test
    fun `test that isCreatingAccountCancelled is reset after being handled`() = runTest {
        whenever(cancelCreateAccountUseCase()) doReturn email

        underTest.cancelCreateAccount()
        underTest.onHandleCancelCreateAccount()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isCreatingAccountCancelled).isFalse()
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
            assertThat(expectMostRecentItem().message).isNull()
        }
    }

    @Test
    fun `test that name and email update correctly`() = runTest {
        val ephemeralCredentials = EphemeralCredentials(
            email = "email",
            password = "password",
            session = "session",
            firstName = "firstName",
            lastName = "lastName"
        )
        whenever(monitorEphemeralCredentialsUseCase()).thenReturn(
            flowOf(ephemeralCredentials)
        )

        initializeUnderTest()

        underTest.uiState.test {
            val uiState = expectMostRecentItem()
            assertThat(uiState.firstName).isEqualTo(ephemeralCredentials.firstName)
            assertThat(uiState.registeredEmail).isEqualTo(ephemeralCredentials.email)
        }
    }
}
