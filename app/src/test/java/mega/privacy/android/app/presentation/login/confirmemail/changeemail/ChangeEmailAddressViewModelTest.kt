package mega.privacy.android.app.presentation.login.confirmemail.changeemail

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.login.confirmemail.mapper.ResendSignUpLinkErrorMapper
import mega.privacy.android.app.presentation.login.confirmemail.model.ResendSignUpLinkError
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.usecase.IsEmailValidUseCase
import mega.privacy.android.domain.usecase.login.confirmemail.ResendSignUpLinkUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChangeEmailAddressViewModelTest {
    private lateinit var underTest: ChangeEmailAddressViewModel
    private var savedStateHandle = SavedStateHandle()

    private val resendSignUpLinkUseCase: ResendSignUpLinkUseCase = mock()
    private val isEmailValidUseCase = IsEmailValidUseCase()
    private val resendSignUpLinkErrorMapper = ResendSignUpLinkErrorMapper()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        reset(resendSignUpLinkUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel(
        email: String? = null,
        fullName: String? = null,
    ) {
        savedStateHandle = SavedStateHandle.Companion.invoke(
            route = ChangeEmailAddressScreen(
                email = email,
                fullName = fullName
            )
        )

        underTest = ChangeEmailAddressViewModel(
            savedStateHandle = savedStateHandle,
            isEmailValidUseCase = isEmailValidUseCase,
            resendSignUpLinkUseCase = resendSignUpLinkUseCase,
            resendSignUpLinkErrorMapper = resendSignUpLinkErrorMapper
        )
    }

    @Test
    fun `test that when email valid should return true`() = runTest {
        initViewModel()

        underTest.validateEmail("lh+test2@mega.co.nz")

        underTest.uiState.test {
            assertThat(awaitItem().isEmailValid).isTrue()
        }
    }

    @Test
    fun `test that when email is invalid should return false`() = runTest {
        initViewModel()

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
            initViewModel(email = email, fullName = fullName)
            whenever(resendSignUpLinkUseCase(email, fullName)).thenReturn(email)

            underTest.changeEmailAddress()

            underTest.uiState.test {
                assertThat(awaitItem().changeEmailAddressSuccessEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that AccountAlreadyExists error is triggered`() = runTest {
        val email = "unknown.error@mega.co.nz"
        val fullName = "Full Name"
        initViewModel(email = email, fullName = fullName)
        whenever(resendSignUpLinkUseCase(email, fullName)).thenAnswer {
            throw CreateAccountException.AccountAlreadyExists
        }

        underTest.changeEmailAddress()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().resendSignUpLinkError).isEqualTo(
                triggered(ResendSignUpLinkError.AccountExists)
            )
        }
    }

    @Test
    fun `test that TooManyAttemptsException error is triggered`() = runTest {
        val email = "unknown.error@mega.co.nz"
        val fullName = "Full Name"
        initViewModel(email = email, fullName = fullName)
        whenever(resendSignUpLinkUseCase(email, fullName)).thenAnswer {
            throw CreateAccountException.TooManyAttemptsException
        }

        underTest.changeEmailAddress()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().resendSignUpLinkError).isEqualTo(
                triggered(ResendSignUpLinkError.TooManyAttempts)
            )
        }
    }

    @Test
    fun `test that Unknown error is triggered`() = runTest {
        val email = "unknown.error@mega.co.nz"
        val fullName = "Full Name"
        initViewModel(email = email, fullName = fullName)
        whenever(resendSignUpLinkUseCase(email, fullName)).thenAnswer {
            throw CreateAccountException.Unknown(MegaException(1, null))
        }

        underTest.changeEmailAddress()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().resendSignUpLinkError).isEqualTo(
                triggered(ResendSignUpLinkError.Unknown)
            )
        }
    }

    @Test
    fun `test that the resend signup link error is successfully consumed`() = runTest {
        val email = "unknown.error@mega.co.nz"
        val fullName = "Full Name"
        initViewModel(email = email, fullName = fullName)
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
