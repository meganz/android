package mega.privacy.android.app.presentation.passcode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.app.extensions.withCoroutineExceptions
import mega.privacy.android.app.presentation.passcode.mapper.PasscodeTypeMapper
import mega.privacy.android.app.presentation.passcode.model.PasscodeUIType
import mega.privacy.android.app.presentation.passcode.model.PasscodeUnlockState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.entity.passcode.UnlockPasscodeRequest
import mega.privacy.android.domain.exception.security.NoPasscodeTypeSetException
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeAttemptsUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTypeUseCase
import mega.privacy.android.domain.usecase.passcode.UnlockPasscodeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasscodeUnlockViewModelTest {
    private lateinit var underTest: PasscodeUnlockViewModel
    private val unlockPasscodeUseCase = mock<UnlockPasscodeUseCase>()
    private val monitorPasscodeAttemptsUseCase = mock<MonitorPasscodeAttemptsUseCase>()
    private val monitorPasscodeTypeUseCase = mock<MonitorPasscodeTypeUseCase>()
    private val passcodeTypeMapper = mock<PasscodeTypeMapper>()
    private val monitorThemeModeUseCase = mock<MonitorThemeModeUseCase>()

    @AfterEach
    internal fun cleanup() {
        Mockito.reset(
            unlockPasscodeUseCase,
            monitorPasscodeAttemptsUseCase,
            monitorPasscodeTypeUseCase,
            passcodeTypeMapper,
            monitorThemeModeUseCase,
        )
    }

    @Test
    internal fun `test that initial state is loading`() =
        runTest {
            initViewModel(
                monitorPasscodeAttemptsUseCaseStub = monitorPasscodeAttemptsUseCase.stub {
                    on { invoke() }.thenReturn(
                        flow { awaitCancellation() })
                },
            )
            underTest.state.test {
                val actual = awaitItem()
                assertThat(actual).isInstanceOf(PasscodeUnlockState.Loading::class.java)
                assertThat((actual as PasscodeUnlockState.Loading).themeMode).isEqualTo(ThemeMode.System)
            }
        }

    @Test
    internal fun `test that failed attempts count is updated when the count changes`() =
        runTest {
            val expected = 1

            initViewModel(
                monitorPasscodeAttemptsUseCaseStub = monitorPasscodeAttemptsUseCase.stub {
                    on { invoke() }.thenReturn(expected.asHotFlow())
                }
            )

            underTest.state.filterIsInstance<PasscodeUnlockState.Data>().test {
                val state = awaitItem()
                assertThat(state.failedAttempts).isEqualTo(expected)
                assertThat(state.themeMode).isEqualTo(ThemeMode.System)
            }
        }

    @Test
    internal fun `test that logout warning becomes true after 5 incorrect attempts`() = runTest {
        initViewModel(
            monitorPasscodeAttemptsUseCaseStub = monitorPasscodeAttemptsUseCase.stub {
                on { invoke() }.thenReturn(5.asHotFlow())
            }
        )

        underTest.state.filterIsInstance<PasscodeUnlockState.Data>().test {
            val state = awaitItem()
            assertThat(state.logoutWarning).isEqualTo(true)
            assertThat(state.themeMode).isEqualTo(ThemeMode.System)
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
            val expected = "correct"

            initViewModel()

            underTest.unlockWithPassword(expected)

            verify(unlockPasscodeUseCase).invoke(UnlockPasscodeRequest.PasswordRequest(expected))
        }

    @Test
    internal fun `test that an exception from the monitor attempts use case does not get propagated`() =
        withCoroutineExceptions {
            runTest {


                initViewModel(
                    monitorPasscodeAttemptsUseCaseStub = monitorPasscodeAttemptsUseCase.stub {
                        on { invoke() }.thenAnswer { throw Exception("Monitor threw an exception") }
                    }
                )

                underTest.state.test {
                    awaitItem()
                    val events = cancelAndConsumeRemainingEvents()
                    assertThat(events).isEmpty()
                }
            }
        }

    @Test
    internal fun `test that passcode type is mapped and returned`() = runTest {
        val expected = PasscodeUIType.Alphanumeric(false)

        initViewModel(
            passcodeTypeMapperStub = passcodeTypeMapper.stub {
                on { invoke(any()) }.thenReturn(expected)
            }
        )

        underTest.state.filterIsInstance<PasscodeUnlockState.Data>().test {
            val state = awaitItem()
            assertThat(state.passcodeType).isEqualTo(expected)
            assertThat(state.themeMode).isEqualTo(ThemeMode.System)
        }
    }

    @Test
    internal fun `test that an exception from the get passcode type is not propagated`() =
        withCoroutineExceptions {
            runTest {
                initViewModel(
                    monitorPasscodeTypeUseCaseStub = monitorPasscodeTypeUseCase.stub {
                        onBlocking { invoke() }.thenAnswer { throw NoPasscodeTypeSetException() }
                    }
                )

                underTest.state.test {
                    val state = awaitItem()
                    assertThat(state).isInstanceOf(PasscodeUnlockState.Loading::class.java)
                    assertThat((state as PasscodeUnlockState.Loading).themeMode).isEqualTo(ThemeMode.System)
                }
            }
        }

    @Test
    internal fun `test that calling unlock with a biometrics calls the unlock use case`() =
        runTest {

            initViewModel()

            underTest.unlockWithBiometrics()

            verify(unlockPasscodeUseCase).invoke(UnlockPasscodeRequest.BiometricRequest)
        }

    @Test
    internal fun `test that theme mode changes are reflected in state`() = runTest {
        val initialThemeMode = ThemeMode.Light
        val newThemeMode = ThemeMode.Dark

        val themeModeFlow = MutableStateFlow(initialThemeMode)
        initViewModel(
            monitorThemeModeUseCaseStub = monitorThemeModeUseCase.stub {
                on { invoke() }.thenReturn(themeModeFlow)
            }
        )

        underTest.state.filterIsInstance<PasscodeUnlockState.Data>().test {
            val initialState = awaitItem()
            assertThat(initialState.themeMode).isEqualTo(initialThemeMode)

            // Change theme mode
            themeModeFlow.emit(newThemeMode)

            val updatedState = awaitItem()
            assertThat(updatedState.themeMode).isEqualTo(newThemeMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that system theme mode is returned if monitor fails`() = runTest {
        initViewModel(
            monitorThemeModeUseCaseStub = monitorThemeModeUseCase.stub {
                on { invoke() }.thenAnswer { throw Exception("Monitor theme mode failed") }
            }
        )

        underTest.state.filterIsInstance<PasscodeUnlockState.Data>().test {
            val state = awaitItem()
            // Should default to System theme mode on error
            assertThat(state.themeMode).isEqualTo(ThemeMode.System)
        }
    }

    private fun initViewModel(
        monitorPasscodeAttemptsUseCaseStub: MonitorPasscodeAttemptsUseCase =
            monitorPasscodeAttemptsUseCase.stub {
                on { invoke() }.thenReturn(0.asHotFlow())
            },
        unlockPasscodeUseCaseStub: UnlockPasscodeUseCase = unlockPasscodeUseCase,
        monitorPasscodeTypeUseCaseStub: MonitorPasscodeTypeUseCase =
            monitorPasscodeTypeUseCase.stub {
                on { invoke() }.thenReturn(PasscodeType.Password.asHotFlow())
            },
        passcodeTypeMapperStub: PasscodeTypeMapper = passcodeTypeMapper.stub {
            on { invoke(any()) }.thenReturn(PasscodeUIType.Alphanumeric(false))
        },
        monitorThemeModeUseCaseStub: MonitorThemeModeUseCase =
            monitorThemeModeUseCase.stub {
                on { invoke() }.thenReturn(flowOf(ThemeMode.System))
            },
    ) {
        underTest = PasscodeUnlockViewModel(
            monitorPasscodeAttemptsUseCase = monitorPasscodeAttemptsUseCaseStub,
            unlockPasscodeUseCase = unlockPasscodeUseCaseStub,
            monitorPasscodeTypeUseCase = monitorPasscodeTypeUseCaseStub,
            passcodeTypeMapper = passcodeTypeMapperStub,
            monitorThemeModeUseCase = monitorThemeModeUseCaseStub,
        )
    }
}