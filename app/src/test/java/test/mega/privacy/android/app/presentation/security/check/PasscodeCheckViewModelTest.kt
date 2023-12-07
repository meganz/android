package test.mega.privacy.android.app.presentation.security.check

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.security.check.PasscodeCheckViewModel
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeLockStateUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import test.mega.privacy.android.app.extensions.asHotFlow
import test.mega.privacy.android.app.extensions.withCoroutineExceptions

@OptIn(ExperimentalCoroutinesApi::class)
class PasscodeCheckViewModelTest {
    private lateinit var underTest: PasscodeCheckViewModel

    private val monitorPasscodeLockStateUseCase = mock<MonitorPasscodeLockStateUseCase>()

    @BeforeEach
    internal fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = PasscodeCheckViewModel(
            monitorPasscodeLockStateUseCase = monitorPasscodeLockStateUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    internal fun `test that initial state is loading`() = runTest {
        monitorPasscodeLockStateUseCase.stub {
            on { invoke() }.thenReturn(flow { awaitCancellation() })
        }

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(PasscodeCheckState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that true locked state returns locked ui state`() = runTest {
        monitorPasscodeLockStateUseCase.stub {
            on { invoke() }.thenReturn(true.asHotFlow())
        }
        initViewModel()
        underTest.state.filterNot { it is PasscodeCheckState.Loading }.test {
            assertThat(awaitItem()).isEqualTo(PasscodeCheckState.Locked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that false locked state returns unlocked ui state`() = runTest {
        monitorPasscodeLockStateUseCase.stub {
            on { invoke() }.thenReturn(false.asHotFlow())
        }
        initViewModel()
        underTest.state.filterNot { it is PasscodeCheckState.Loading }.test {
            assertThat(awaitItem()).isEqualTo(PasscodeCheckState.UnLocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that exceptions thrown by monitor passcode use case are not propagated`() =
        withCoroutineExceptions {
            runTest {
                monitorPasscodeLockStateUseCase.stub {
                    on { invoke() }.thenAnswer { throw Exception("Broken use case") }
                }

                underTest.state.test {
                    assertThat(awaitItem()).isEqualTo(PasscodeCheckState.Loading)
                    assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
                }
            }
        }
}