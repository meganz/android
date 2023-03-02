package test.mega.privacy.android.app.presentation.twofactorauthentication

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.twofactorauthentication.TwoFactorAuthenticationViewModel
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.domain.exception.EnableMultiFactorAuthException
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
internal class TwoFactorAuthenticationViewModelTest {
    private lateinit var underTest: TwoFactorAuthenticationViewModel
    private val enableMultiFactorAuth = mock<EnableMultiFactorAuth>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = TwoFactorAuthenticationViewModel(enableMultiFactorAuth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that when user submit multi factor authentication pin code successfully should return AuthenticationPassed state`() =
        runTest {
            whenever(enableMultiFactorAuth(any())).thenReturn(true)
            underTest.submitMultiFactorAuthPin("")
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.authenticationState).isEqualTo(AuthenticationState.AuthenticationPassed)
            }
        }

    @Test
    fun `test that when user submit wrong multi factor authentication pin should return AuthenticationFailed state`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(enableMultiFactorAuth(any()))
                .thenAnswer {
                    throw EnableMultiFactorAuthException(
                        errorCode = fakeErrorCode,
                        errorString = ""
                    )
                }
            underTest.submitMultiFactorAuthPin("")
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.authenticationState).isEqualTo(AuthenticationState.AuthenticationFailed)
            }
        }

    @Test
    fun `test that when multi factor authentication returns error should return AuthenticationError state`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(enableMultiFactorAuth(any()))
                .thenAnswer {
                    throw EnableMultiFactorAuthException(
                        errorCode = fakeErrorCode,
                        errorString = ""
                    )
                }
            underTest.submitMultiFactorAuthPin("")
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.authenticationState).isEqualTo(AuthenticationState.AuthenticationError)
            }
        }
}
