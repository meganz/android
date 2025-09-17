package mega.privacy.android.app.presentation.testpassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity.Companion.KEY_IS_LOGOUT
import mega.privacy.android.app.presentation.testpassword.TestPasswordViewModel
import mega.privacy.android.app.presentation.testpassword.model.PasswordState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.BlockPasswordReminderUseCase
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.IsCurrentPasswordUseCase
import mega.privacy.android.domain.usecase.NotifyPasswordCheckedUseCase
import mega.privacy.android.domain.usecase.SetMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.SkipPasswordReminderUseCase
import mega.privacy.android.domain.usecase.account.ExportRecoveryKeyUseCase
import mega.privacy.android.domain.usecase.account.GetPrintRecoveryKeyFileUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.verification.VerificationMode
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
internal class TestPasswordViewModelTest {
    private lateinit var underTest: TestPasswordViewModel
    private val savedStateHandle = SavedStateHandle()
    private val setMasterKeyExportedUseCase = mock<SetMasterKeyExportedUseCase>()
    private val getExportMasterKeyUseCase = mock<GetExportMasterKeyUseCase>()
    private val isCurrentPasswordUseCase = mock<IsCurrentPasswordUseCase>()
    private val skipPasswordReminderUseCase = mock<SkipPasswordReminderUseCase>()
    private val blockPasswordReminderUseCase = mock<BlockPasswordReminderUseCase>()
    private val notifyPasswordCheckedUseCase = mock<NotifyPasswordCheckedUseCase>()
    private val logoutUseCase = mock<LogoutUseCase>()
    private val getPrintRecoveryKeyFileUseCase = mock<GetPrintRecoveryKeyFileUseCase>()
    private val exportRecoveryKeyUseCase = mock<ExportRecoveryKeyUseCase>()

    @BeforeEach
    fun setup() {
        init()
    }

    private fun init() {
        underTest = TestPasswordViewModel(
            savedStateHandle = savedStateHandle,
            getExportMasterKeyUseCase = getExportMasterKeyUseCase,
            setMasterKeyExportedUseCase = setMasterKeyExportedUseCase,
            isCurrentPasswordUseCase = isCurrentPasswordUseCase,
            skipPasswordReminderUseCase = skipPasswordReminderUseCase,
            blockPasswordReminderUseCase = blockPasswordReminderUseCase,
            notifyPasswordCheckedUseCase = notifyPasswordCheckedUseCase,
            logoutUseCase = logoutUseCase,
            getPrintRecoveryKeyFileUseCase = getPrintRecoveryKeyFileUseCase,
            ioDispatcher = dispatcher,
            exportRecoveryKeyUseCase = exportRecoveryKeyUseCase,
        )
    }

    @Test
    fun `test that isCurrentPassword should be updated to true if password state is true`() =
        runTest {
            val mockPassword = "password"
            whenever(isCurrentPasswordUseCase(mockPassword)).thenReturn(true)

            underTest.checkForCurrentPassword(mockPassword)

            underTest.uiState.test {
                assertThat(awaitItem().isCurrentPassword).isEqualTo(PasswordState.True)
            }
        }

    @Test
    fun `test that isCurrentPassword should be updated to false and wrongPasswordAttempts should be updated when password state is false`() =
        runTest {
            val mockPassword = "password"
            whenever(isCurrentPasswordUseCase(mockPassword)).thenReturn(false)
            val expectedAttempts = underTest.uiState.value.wrongPasswordAttempts + 1

            underTest.checkForCurrentPassword(mockPassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isCurrentPassword).isEqualTo(PasswordState.False)
                assertThat(state.wrongPasswordAttempts).isEqualTo(expectedAttempts)
            }
        }

    @Test
    fun `test that isUserExhaustedPasswordAttempts should be updated to true and reset attempts to 0 when wrongPasswordAttempts has reach 3 times`() =
        runTest {
            val mockPassword = "password"
            whenever(isCurrentPasswordUseCase(mockPassword)).thenReturn(false)

            underTest.checkForCurrentPassword(mockPassword)
            underTest.checkForCurrentPassword(mockPassword)
            underTest.checkForCurrentPassword(mockPassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isUserExhaustedPasswordAttempts).isEqualTo(triggered)
                assertThat(state.wrongPasswordAttempts).isEqualTo(0)
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
    fun `test that loading should be true and notifyPasswordCheckedUseCase should be called when logout is true and notifyPasswordReminderSucceeded called`() {
        verifyPasswordReminderSucceeded(isLogout = true, expected = true)
    }

    @Test
    fun `test that loading should be false and notifyPasswordCheckedUseCase should be called when logout is false and notifyPasswordReminderSucceeded called`() {
        verifyPasswordReminderSucceeded(isLogout = false, expected = false)
    }

    private fun verifyPasswordReminderSucceeded(isLogout: Boolean, expected: Boolean) = runTest {
        savedStateHandle[KEY_IS_LOGOUT] = isLogout
        init()

        underTest.notifyPasswordReminderSucceeded()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isEqualTo(expected)
            assertThat(state.isUserLogout).isInstanceOf(StateEventWithContentTriggered::class.java)
        }

        verify(notifyPasswordCheckedUseCase).invoke()
    }

    @Test
    fun `test that loading should be false when notifyPasswordReminderSucceeded catch an error`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(notifyPasswordCheckedUseCase()).thenAnswer {
                throw MegaException(
                    errorCode = fakeErrorCode,
                    errorString = "error"
                )
            }

            underTest.notifyPasswordReminderSucceeded()

            underTest.uiState.test {
                assertThat(awaitItem().isLoading).isEqualTo(false)
            }
        }

    @Test
    fun `test that loading should be true and skipPasswordReminderUseCase should be called when logout is true and dismissPasswordReminder called`() {
        verifyDismissPasswordReminder(isLogout = true, expected = true)
    }

    @Test
    fun `test that loading should be false and skipPasswordReminderUseCase should be called when logout is false and dismissPasswordReminder called`() {
        verifyDismissPasswordReminder(isLogout = false, expected = false)
    }

    private fun verifyDismissPasswordReminder(isLogout: Boolean, expected: Boolean) = runTest {
        savedStateHandle[KEY_IS_LOGOUT] = isLogout
        init()

        underTest.dismissPasswordReminder(isLogout)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isEqualTo(expected)
            assertThat(state.isUserLogout).isInstanceOf(StateEventWithContentTriggered::class.java)
        }

        verify(skipPasswordReminderUseCase).invoke()
    }

    @Test
    fun `test that blockPasswordReminderUseCase should be called once if calling notifyPasswordReminderSucceeded with isBlocked is true`() {
        verifySuccessOnBlockPasswordReminder(isBlocked = true, expected = times(1))
    }

    @Test
    fun `test that blockPasswordReminderUseCase should never be called if calling notifyPasswordReminderSucceeded with isBlocked is false`() {
        verifySuccessOnBlockPasswordReminder(isBlocked = false, expected = never())
    }

    private fun verifySuccessOnBlockPasswordReminder(
        isBlocked: Boolean,
        expected: VerificationMode,
    ) =
        runTest {
            underTest.setPasswordReminderBlocked(isBlocked)
            underTest.notifyPasswordReminderSucceeded()

            verify(notifyPasswordCheckedUseCase, times(1)).invoke()
            verify(blockPasswordReminderUseCase, expected).invoke()
        }

    @Test
    fun `test that blockPasswordReminderUseCase should be called once if calling dismissPasswordReminder with isBlocked is true`() {
        verifyDismissOnBlockPasswordReminder(isBlocked = true, expected = times(1))
    }

    @Test
    fun `test that blockPasswordReminderUseCase should never be called if calling dismissPasswordReminder with isBlocked is false`() {
        verifyDismissOnBlockPasswordReminder(isBlocked = false, expected = never())
    }

    private fun verifyDismissOnBlockPasswordReminder(
        isBlocked: Boolean,
        expected: VerificationMode,
    ) =
        runTest {
            underTest.setPasswordReminderBlocked(isBlocked)
            underTest.dismissPasswordReminder(true)

            verify(skipPasswordReminderUseCase, times(1)).invoke()
            verify(blockPasswordReminderUseCase, expected).invoke()
        }

    @Test
    fun `test that isUITestPasswordMode should be true when switchToTestPasswordLayout called`() =
        runTest {
            underTest.switchToTestPasswordLayout()

            underTest.uiState.test {
                assertThat(awaitItem().isUITestPasswordMode).isEqualTo(true)
            }
        }

    @Test
    fun `test that setMasterKeyExported should NOT trigger when recovery key is empty, and vice versa`() {
        val fakeRecoveryKey = "JALSJLKNDnsnda12738"

        fun verify(key: String?, expectedInvocation: Int) = runTest {
            whenever(getExportMasterKeyUseCase()).thenReturn(key)

            underTest.getRecoveryKey()

            verify(setMasterKeyExportedUseCase, times(expectedInvocation)).invoke()
        }

        verify(key = null, expectedInvocation = 0)
        verify(key = fakeRecoveryKey, expectedInvocation = 1)
    }

    @Test
    fun `test that printRecoveryKey event is triggered when printRecoveryKey is invoked`() =
        runTest {
            val file = mock<File>()
            whenever(getPrintRecoveryKeyFileUseCase()).thenReturn(file)
            underTest.printRecoveryKey()
            underTest.uiState.test {
                val result = awaitItem()
                assertThat(result.printRecoveryKey).isInstanceOf(StateEventWithContentTriggered::class.java)
            }
        }

    @Test
    fun `test that printRecoveryKey event is consumed when resetPrintRecoveryKey is invoked`() =
        runTest {
            underTest.resetPrintRecoveryKey()
            underTest.uiState.test {
                val result = awaitItem()
                assertThat(result.printRecoveryKey).isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    fun `test that notifyPasswordCheckedUseCase is called when exportRecoveryKey succeeds`() =
        runTest {
            val uri = "testUri"
            whenever(exportRecoveryKeyUseCase(uri)).thenReturn(true)
            underTest.exportRecoveryKey(uri)
            verify(notifyPasswordCheckedUseCase).invoke()
        }

    @Test
    fun `test that notifyPasswordCheckedUseCase is not called when exportRecoveryKey fails`() =
        runTest {
            val uri = "testUri"
            whenever(exportRecoveryKeyUseCase(uri)).thenReturn(false)
            underTest.exportRecoveryKey(uri)
            verify(notifyPasswordCheckedUseCase, never()).invoke()
        }

    // Tests for dismissPasswordReminderAndFinish method with timeout protection

    @Test
    fun `test that dismissPasswordReminderAndFinish triggers logout event and calls skipPasswordReminderUseCase`() =
        runTest {
            // Given
            // No special setup needed - default state

            // When
            underTest.dismissPasswordReminderAndFinish()

            // Then
            underTest.uiState.test {
                val state = awaitItem() // Get the final state after method execution
                
                // Verify final state
                assertThat(state.isLoading).isFalse()
                assertThat(state.isUserLogout).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.isUserLogout as StateEventWithContentTriggered).content).isFalse()
            }

            verify(skipPasswordReminderUseCase).invoke()
        }

    @Test
    fun `test that dismissPasswordReminderAndFinish calls blockPasswordReminderUseCase when password reminder is blocked`() =
        runTest {
            // Given
            underTest.setPasswordReminderBlocked(true)

            // When
            underTest.dismissPasswordReminderAndFinish()

            // Then
            underTest.uiState.test {
                val state = awaitItem() // Get the final state after method execution
                assertThat(state.isUserLogout).isInstanceOf(StateEventWithContentTriggered::class.java)
            }

            verify(skipPasswordReminderUseCase).invoke()
            verify(blockPasswordReminderUseCase).invoke()
        }

    @Test
    fun `test that dismissPasswordReminderAndFinish does not call blockPasswordReminderUseCase when password reminder is not blocked`() =
        runTest {
            // Given
            underTest.setPasswordReminderBlocked(false)

            // When
            underTest.dismissPasswordReminderAndFinish()

            // Then
            underTest.uiState.test {
                val state = awaitItem() // Get the final state after method execution
                assertThat(state.isUserLogout).isInstanceOf(StateEventWithContentTriggered::class.java)
            }

            verify(skipPasswordReminderUseCase).invoke()
            verify(blockPasswordReminderUseCase, never()).invoke()
        }

    @Test
    fun `test that dismissPasswordReminderAndFinish handles skipPasswordReminderUseCase failure gracefully`() =
        runTest {
            // Given
            whenever(skipPasswordReminderUseCase()).thenAnswer {
                throw MegaException(errorCode = 123, errorString = "Network error")
            }

            // When
            underTest.dismissPasswordReminderAndFinish()

            // Then
            underTest.uiState.test {
                val state = awaitItem() // Get the final state after method execution
                assertThat(state.isLoading).isFalse()
                assertThat(state.isUserLogout).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.isUserLogout as StateEventWithContentTriggered).content).isFalse()
            }

            verify(skipPasswordReminderUseCase).invoke()
        }

    @Test
    fun `test that dismissPasswordReminderAndFinish handles timeout gracefully`() =
        runTest {
            // Given
            // Mock skipPasswordReminderUseCase to actually suspend longer than timeout (100ms)
            whenever(skipPasswordReminderUseCase()).thenAnswer {
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.delay(200) // 200ms > 100ms timeout
                }
                Unit
            }

            // When
            underTest.dismissPasswordReminderAndFinish(timeoutMs = 100) // Use short timeout for fast testing

            // Then
            underTest.uiState.test {
                val state = awaitItem() // Get the final state after method execution
                assertThat(state.isLoading).isFalse()
                assertThat(state.isUserLogout).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.isUserLogout as StateEventWithContentTriggered).content).isFalse()
            }

            verify(skipPasswordReminderUseCase).invoke()
        }

    @Test
    fun `test that dismissPasswordReminderAndFinish handles blockPasswordReminderUseCase failure gracefully`() =
        runTest {
            // Given
            underTest.setPasswordReminderBlocked(true)
            whenever(blockPasswordReminderUseCase()).thenAnswer {
                throw MegaException(errorCode = 456, errorString = "Block reminder error")
            }

            // When
            underTest.dismissPasswordReminderAndFinish()

            // Then
            underTest.uiState.test {
                val state = awaitItem() // Get the final state after method execution
                assertThat(state.isLoading).isFalse()
                assertThat(state.isUserLogout).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((state.isUserLogout as StateEventWithContentTriggered).content).isFalse()
            }

            verify(skipPasswordReminderUseCase).invoke()
            verify(blockPasswordReminderUseCase).invoke()
        }

    companion object {
        private val dispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(dispatcher)
    }
}