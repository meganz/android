package test.mega.privacy.android.app.presentation.testpassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.testpassword.TestPasswordViewModel
import mega.privacy.android.app.presentation.testpassword.model.PasswordState
import mega.privacy.android.domain.usecase.BlockPasswordReminderUseCase
import mega.privacy.android.domain.usecase.IsCurrentPasswordUseCase
import mega.privacy.android.domain.usecase.NotifyPasswordCheckedUseCase
import mega.privacy.android.domain.usecase.SkipPasswordReminderUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class TestPasswordViewModelTest {
    private lateinit var underTest: TestPasswordViewModel
    private val savedStateHandle = SavedStateHandle()
    private val isCurrentPasswordUseCase = mock<IsCurrentPasswordUseCase>()
    private val skipPasswordReminderUseCase = mock<SkipPasswordReminderUseCase>()
    private val blockPasswordReminderUseCase = mock<BlockPasswordReminderUseCase>()
    private val notifyPasswordCheckedUseCase = mock<NotifyPasswordCheckedUseCase>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = TestPasswordViewModel(
            savedStateHandle = savedStateHandle,
            isCurrentPasswordUseCase = isCurrentPasswordUseCase,
            skipPasswordReminderUseCase = skipPasswordReminderUseCase,
            blockPasswordReminderUseCase = blockPasswordReminderUseCase,
            notifyPasswordCheckedUseCase = notifyPasswordCheckedUseCase
        )
    }

    @Test
    fun `test that checkForCurrentPassword should update state according to isCurrentPassword`() {
        fun verify(isCurrentPassword: Boolean, expected: PasswordState) = runTest {
            val mockPassword = "password"
            whenever(isCurrentPasswordUseCase(mockPassword)).thenReturn(isCurrentPassword)

            underTest.checkForCurrentPassword(mockPassword)

            underTest.uiState.test {
                assertThat(awaitItem().isCurrentPassword).isEqualTo(expected)
            }
        }

        verify(true, PasswordState.True)
        verify(false, PasswordState.False)
    }

    @Test
    fun `test that notifyPasswordReminderSkipped should update state to true when no errors detected`() =
        runTest {
            underTest.notifyPasswordReminderSkipped()

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(PasswordState.True)
            }
        }

    @Test
    fun `test that notifyPasswordReminderBlocked should update state to true when no errors detected`() =
        runTest {
            underTest.notifyPasswordReminderBlocked()

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(PasswordState.True)
            }
        }

    @Test
    fun `test that notifyPasswordReminderSucceed should update state to true when no errors detected`() =
        runTest {
            underTest.notifyPasswordReminderSucceed()

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(PasswordState.True)
            }
        }

    @Test
    fun `test that isPasswordReminderNotified should be false when notify password reminder returns error`() =
        runTest {
            whenever(skipPasswordReminderUseCase()).thenThrow(RuntimeException("Error"))

            underTest.notifyPasswordReminderSkipped()

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(PasswordState.False)
            }
        }

    @Test
    fun `test that isCurrentPassword should reset to false when resetCurrentPasswordState is called`() =
        runTest {
            val mockPassword = "password"
            whenever(isCurrentPasswordUseCase(mockPassword)).thenReturn(true)

            underTest.checkForCurrentPassword(mockPassword)
            underTest.uiState.test {
                assertThat(awaitItem().isCurrentPassword).isNotEqualTo(PasswordState.Initial)
            }

            underTest.resetCurrentPasswordState()
            underTest.uiState.test {
                assertThat(awaitItem().isCurrentPassword).isEqualTo(PasswordState.Initial)
            }
        }

    @Test
    fun `test that isPasswordReminderNotified should reset to false when resetPasswordReminderState is called`() =
        runTest {
            val mockPassword = "password"

            underTest.notifyPasswordReminderSkipped()
            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isNotEqualTo(PasswordState.Initial)
            }

            underTest.resetPasswordReminderState()
            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(PasswordState.Initial)
            }
        }
}