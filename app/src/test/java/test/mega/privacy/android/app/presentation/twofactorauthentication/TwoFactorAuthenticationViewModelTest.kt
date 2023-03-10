package test.mega.privacy.android.app.presentation.twofactorauthentication

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.twofactorauthentication.TwoFactorAuthenticationViewModel
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.domain.exception.EnableMultiFactorAuthException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import mega.privacy.android.domain.usecase.GetMultiFactorAuthCode
import mega.privacy.android.domain.usecase.IsMasterKeyExported
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
    private val isMasterKeyExported = mock<IsMasterKeyExported>()
    private val getMultiFactorAuthCode = mock<GetMultiFactorAuthCode>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = TwoFactorAuthenticationViewModel(
            enableMultiFactorAuth,
            isMasterKeyExported,
            getMultiFactorAuthCode
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that authenticationState should be AuthenticationPassed when submitting multi factor authentication code is successful`() =
        runTest {
            whenever(enableMultiFactorAuth(any())).thenReturn(true)
            underTest.submitMultiFactorAuthPin("")
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.authenticationState).isEqualTo(AuthenticationState.AuthenticationPassed)
            }
        }

    @Test
    fun `test that authenticationState should be AuthenticationFailed when wrong multi factor authentication pin get submitted`() =
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
                    throw MegaException(
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

    @Test
    fun `test that isMasterKeyExported should be true when getting master key status is successful`() =
        runTest {
            whenever(isMasterKeyExported()).thenReturn(true)
            underTest.getMasterKeyStatus()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isMasterKeyExported).isEqualTo(true)
            }
        }

    @Test
    fun `test that isMasterKeyExported should be false when getting master key status returns error`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(isMasterKeyExported()).thenAnswer {
                throw MegaException(
                    errorCode = fakeErrorCode,
                    errorString = ""
                )
            }
            underTest.getMasterKeyStatus()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isMasterKeyExported).isEqualTo(false)
            }
        }

    @Test
    fun `test that dismissRecoveryKey state should be true when isMasterKeyExported is successful AND request access equals 1`() =
        runTest {
            whenever(isMasterKeyExported()).thenReturn(true)
            underTest.getMasterKeyStatus()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.dismissRecoveryKey).isEqualTo(true)
            }
        }

    @Test
    fun `test that dismissRecoveryKey state should be false when isMasterKeyExported is successful AND request access NOT equals 1`() =
        runTest {
            whenever(isMasterKeyExported()).thenReturn(false)
            underTest.getMasterKeyStatus()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.dismissRecoveryKey).isEqualTo(false)
            }
        }

    @Test
    fun `test that authentication code should not be null when getting multi factor authentication is successful`() =
        runTest {
            val expectedAuthCode = "123456789"
            whenever(getMultiFactorAuthCode()).thenReturn(expectedAuthCode)
            underTest.getAuthenticationCode()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.seed).isEqualTo(expectedAuthCode)
            }
        }

    @Test
    fun `test that authentication code should be null when getting multi factor authentication is not successful`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(getMultiFactorAuthCode()).thenAnswer {
                throw MegaException(
                    errorCode = fakeErrorCode,
                    errorString = ""
                )
            }
            underTest.getAuthenticationCode()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.seed).isNull()
            }
        }
}
