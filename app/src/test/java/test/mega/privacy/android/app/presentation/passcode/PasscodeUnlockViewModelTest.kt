package test.mega.privacy.android.app.presentation.passcode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.passcode.PasscodeUnlockViewModel
import mega.privacy.android.domain.entity.passcode.UnlockPasscodeRequest
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeAttemptsUseCase
import mega.privacy.android.domain.usecase.passcode.UnlockPasscodeUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.extensions.withCoroutineExceptions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasscodeUnlockViewModelTest {
    private lateinit var underTest: PasscodeUnlockViewModel
    private val unlockPasscodeUseCase = mock<UnlockPasscodeUseCase>()
    private val monitorPasscodeAttemptsUseCase = mock<MonitorPasscodeAttemptsUseCase>()

    @BeforeAll
    internal fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }


    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeUnlockViewModel(
            monitorPasscodeAttemptsUseCase = monitorPasscodeAttemptsUseCase,
            unlockPasscodeUseCase = unlockPasscodeUseCase,
        )
    }

    @AfterEach
    internal fun cleanup() {
        Mockito.reset(monitorPasscodeAttemptsUseCase)
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    internal fun `test that initial state has no attempts and does not show the logout warning`() =
        runTest {
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual.failedAttempts).isEqualTo(0)
                assertThat(actual.logoutWarning).isFalse()
            }
        }

    @Test
    internal fun `test that failed attempts count is updated when the count changes`() =
        runTest {
            val expected = 1
            monitorPasscodeAttemptsUseCase.stub {
                on { invoke() }.thenReturn(flow { emit(expected) })
            }
            setUp()

            underTest.state.test {
                assertThat(awaitItem().failedAttempts).isEqualTo(expected)
            }
        }

    @Test
    internal fun `test that logout warning becomes true after 5 incorrect attempts`() = runTest {
        monitorPasscodeAttemptsUseCase.stub {
            on { invoke() }.thenReturn(flow { emit(5) })
        }
        setUp()

        underTest.state.test {
            assertThat(awaitItem().logoutWarning).isEqualTo(true)
        }
    }

    @Test
    internal fun `test that an exception from the unlock use case does not get propagated`() =
        withCoroutineExceptions {
            runTest {
                unlockPasscodeUseCase.stub {
                    onBlocking { invoke(any()) }.thenAnswer { throw Exception("Unlock threw an exception") }
                }
                underTest.state.test {
                    awaitItem()
                    underTest.unlockWithPasscode("passcode")
                    val events = cancelAndConsumeRemainingEvents()
                    assertThat(events).isEmpty()
                }
            }
        }

    @Test
    internal fun `test that calling unlock with a password also calls the unlock use case`() =
        runTest {
            Mockito.clearInvocations(unlockPasscodeUseCase)
            val expected = "correct"

            underTest.unlockWithPassword(expected)

            verify(unlockPasscodeUseCase).invoke(UnlockPasscodeRequest.PasswordRequest(expected))
        }

    @Test
    internal fun `test that an exception from the monitor attempts use case does not get propagated`() =
        withCoroutineExceptions {
            runTest {
                monitorPasscodeAttemptsUseCase.stub {
                    on { invoke() }.thenAnswer { throw Exception("Monitor threw an exception") }
                }
                underTest = PasscodeUnlockViewModel(
                    monitorPasscodeAttemptsUseCase = monitorPasscodeAttemptsUseCase,
                    unlockPasscodeUseCase = unlockPasscodeUseCase
                )
                underTest.state.test {
                    awaitItem()
                    val events = cancelAndConsumeRemainingEvents()
                    assertThat(events).isEmpty()
                }
            }
        }
}