package mega.privacy.android.app.presentation.login.confirmemail.changeemail


import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChangeEmailAddressViewModelTest {
    private lateinit var underTest: ChangeEmailAddressViewModel
    private val savedStateHandle = SavedStateHandle()

    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase = mock()
    private val isEmailValidUseCase = IsEmailValidUseCase()

    @BeforeAll
    fun setUp() {
        underTest = ChangeEmailAddressViewModel(
            savedStateHandle = savedStateHandle,
            isEmailValidUseCase = isEmailValidUseCase,
            resendSignUpLinkUseCase = resendSignUpLinkUseCase
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

    @Test
    fun `changeEmailAddress triggers account exist event when resendSignUpLinkUseCase throws AccountAlreadyExists`() =
        runTest {
            val email = "existing.email@mega.co.nz"
            val fullName = "Full Name"
            savedStateHandle[EMAIL] = email
            savedStateHandle[FULL_NAME] = fullName
            whenever(resendSignUpLinkUseCase(email, fullName))
                .thenAnswer {
                    throw CreateAccountException.AccountAlreadyExists
                }

            underTest.changeEmailAddress()

            underTest.uiState.test {
                assertThat(awaitItem().accountExistEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `changeEmailAddress triggers general error event when resendSignUpLinkUseCase throws unknown exception`() =
        runTest {
            val email = "unknown.error@mega.co.nz"
            val fullName = "Full Name"
            savedStateHandle[EMAIL] = email
            savedStateHandle[FULL_NAME] = fullName
            whenever(resendSignUpLinkUseCase(email, fullName)).thenThrow(RuntimeException())

            underTest.changeEmailAddress()

            underTest.uiState.test {
                assertThat(awaitItem().generalErrorEvent).isEqualTo(triggered)
            }
        }
}