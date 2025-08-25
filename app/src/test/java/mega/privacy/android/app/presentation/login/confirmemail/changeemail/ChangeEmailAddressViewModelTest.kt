package mega.privacy.android.app.presentation.login.confirmemail.changeemail


import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.login.confirmemail.mapper.ResendSignUpLinkErrorMapper
import mega.privacy.android.app.presentation.login.confirmemail.model.ResendSignUpLinkError
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChangeEmailAddressViewModelTest {
    private lateinit var underTest: ChangeEmailAddressViewModel
    private val savedStateHandle = SavedStateHandle()

    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase = mock()
    private val isEmailValidUseCase = IsEmailValidUseCase()
    private val resendSignUpLinkErrorMapper = ResendSignUpLinkErrorMapper()

    @BeforeAll
    fun setUp() {
        underTest = ChangeEmailAddressViewModel(
            savedStateHandle = savedStateHandle,
            isEmailValidUseCase = isEmailValidUseCase,
            resendSignUpLinkUseCase = resendSignUpLinkUseCase,
            resendSignUpLinkErrorMapper = resendSignUpLinkErrorMapper
        )
    }

    @BeforeEach
    fun init() {
        reset(resendSignUpLinkUseCase)
    }

    @Test
    fun `test that when email valid should return true`() = runTest {
        underTest.validateEmail("lh+test2@mega.co.nz")

        underTest.uiState.test {
            assertThat(awaitItem().isEmailValid).isTrue()
        }
    }

    @Test
    fun `test that when email is invalid should return false`() = runTest {
        underTest.validateEmail("lh+test2.mega.co.nz")

        underTest.uiState.test {
            assertThat(awaitItem().isEmailValid).isFalse()
        }
    }

    @Test
    fun `changeEmailAddress triggers success event when resendSignUpLinkUseCase succeeds`() =
        runTest {
            val email = "valid.email@mega.co.nz"
            val fullName = "Full Name"
            savedStateHandle[EMAIL] = email
            savedStateHandle[FULL_NAME] = fullName
            whenever(resendSignUpLinkUseCase(email, fullName)).thenReturn(email)

            underTest.changeEmailAddress()

            underTest.uiState.test {
                assertThat(awaitItem().changeEmailAddressSuccessEvent).isEqualTo(triggered)
            }
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCreateAccountException")
    fun `test that the correct resend signup error is triggered for`(error: CreateAccountException) =
        runTest {
            val email = "unknown.error@mega.co.nz"
            val fullName = "Full Name"
            savedStateHandle[EMAIL] = email
            savedStateHandle[FULL_NAME] = fullName
            whenever(resendSignUpLinkUseCase(email, fullName)).thenAnswer { throw error }

            underTest.changeEmailAddress()

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
        val email = "unknown.error@mega.co.nz"
        val fullName = "Full Name"
        savedStateHandle[EMAIL] = email
        savedStateHandle[FULL_NAME] = fullName
        whenever(
            resendSignUpLinkUseCase(
                email,
                fullName
            )
        ).thenAnswer { throw CreateAccountException.AccountAlreadyExists }

        underTest.changeEmailAddress()
        underTest.onResendSignUpLinkErrorConsumed()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().resendSignUpLinkError).isEqualTo(consumed())
        }
    }
}
