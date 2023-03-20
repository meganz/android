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
import mega.privacy.android.domain.usecase.BlockPasswordReminder
import mega.privacy.android.domain.usecase.IsCurrentPassword
import mega.privacy.android.domain.usecase.NotifyPasswordChecked
import mega.privacy.android.domain.usecase.SkipPasswordReminder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
internal class TestPasswordViewModelTest {
    private lateinit var underTest: TestPasswordViewModel
    private val savedStateHandle = SavedStateHandle()
    private val isCurrentPassword = mock<IsCurrentPassword>()
    private val skipPasswordReminder = mock<SkipPasswordReminder>()
    private val blockPasswordReminder = mock<BlockPasswordReminder>()
    private val notifyPasswordChecked = mock<NotifyPasswordChecked>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = TestPasswordViewModel(
            savedStateHandle = savedStateHandle,
            isCurrentPassword = isCurrentPassword,
            skipPasswordReminder = skipPasswordReminder,
            blockPasswordReminder = blockPasswordReminder,
            notifyPasswordChecked = notifyPasswordChecked
        )
    }

    @Test
    fun `test that checkForCurrentPassword should update state according to isCurrentPassword`() =
        runTest {
            val mockIsCurrentPassword = Random.nextBoolean()
            val mockPassword = "password"
            whenever(isCurrentPassword(mockPassword)).thenReturn(mockIsCurrentPassword)

            underTest.checkForCurrentPassword(mockPassword)

            underTest.uiState.test {
                assertThat(awaitItem().isCurrentPassword).isEqualTo(mockIsCurrentPassword)
            }
        }

    @Test
    fun `test that notifyPasswordReminderSkipped should update state to true when no errors detected`() =
        runTest {
            underTest.notifyPasswordReminderSkipped()

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(true)
            }
        }

    @Test
    fun `test that notifyPasswordReminderBlocked should update state to true when no errors detected`() =
        runTest {
            underTest.notifyPasswordReminderBlocked()

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(true)
            }
        }

    @Test
    fun `test that notifyPasswordReminderSucceed should update state to true when no errors detected`() =
        runTest {
            underTest.notifyPasswordReminderSucceed()

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(true)
            }
        }

    @Test
    fun `test that isPasswordReminderNotified should be false when notify password reminder returns error`() =
        runTest {
            whenever(skipPasswordReminder()).thenThrow(RuntimeException("Error"))

            underTest.notifyPasswordReminderSkipped()

            underTest.uiState.test {
                assertThat(awaitItem().isPasswordReminderNotified).isEqualTo(false)
            }
        }

    @Test
    fun `test that isCurrentPassword should reset to false when resetCurrentPassword is called`() =
        runTest {
            val mockPassword = "password"
            whenever(isCurrentPassword(mockPassword)).thenReturn(true)

            underTest.checkForCurrentPassword(mockPassword)
            underTest.uiState.test {
                assertThat(awaitItem().isCurrentPassword).isEqualTo(true)
            }

            underTest.resetCurrentPasswordState()
            underTest.uiState.test {
                assertThat(awaitItem().isCurrentPassword).isEqualTo(false)
            }
        }
}