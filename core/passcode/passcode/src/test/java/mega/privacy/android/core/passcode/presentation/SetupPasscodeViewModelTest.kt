package mega.privacy.android.core.passcode.presentation

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.passcode.presentation.mapper.PasscodeTypeMapper
import mega.privacy.android.core.passcode.presentation.mapper.PasscodeUITypeToDomainMapper
import mega.privacy.android.core.passcode.presentation.model.PasscodeEntryStage
import mega.privacy.android.core.passcode.presentation.model.PasscodeTypeOption
import mega.privacy.android.core.passcode.presentation.model.PasscodeUIType
import mega.privacy.android.core.passcode.presentation.model.SetupPasscodeUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.entity.passcode.SetPasscodeRequest
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTimeOutUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTypeUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeTimeoutUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetupPasscodeViewModelTest {
    private lateinit var underTest: SetupPasscodeViewModel

    private val monitorPasscodeTypeUseCase = mock<MonitorPasscodeTypeUseCase>()
    private val monitorPasscodeTimeOutUseCase = mock<MonitorPasscodeTimeOutUseCase>()
    private val setPasscodeUseCase = mock<SetPasscodeUseCase>()
    private val setPasscodeTimeoutUseCase = mock<SetPasscodeTimeoutUseCase>()
    private val passcodeTypeMapper = PasscodeTypeMapper()
    private val passcodeUITypeToDomainMapper = PasscodeUITypeToDomainMapper()

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            monitorPasscodeTypeUseCase,
            monitorPasscodeTimeOutUseCase,
            setPasscodeUseCase,
            setPasscodeTimeoutUseCase,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        stubStoredType(PasscodeType.Pin(4))
        initViewModel()

        assertThat(underTest.uiState.value).isEqualTo(SetupPasscodeUiState.Loading)
    }

    @Test
    fun `test that passcode type is taken from the stored passcode type`() = runTest {
        stubStoredType(PasscodeType.Pin(6))
        initViewModel()
        subscribe()

        assertThat(currentState().passcodeType).isEqualTo(PasscodeUIType.Pin(false, 6))
    }

    @Test
    fun `test that passcode type defaults to a four digit pin when no passcode type is set`() =
        runTest {
            stubStoredType(null)
            initViewModel()
            subscribe()

            assertThat(currentState().passcodeType).isEqualTo(PasscodeUIType.Pin(false, 4))
        }

    @Test
    fun `test that onPasscodeEntered moves to the confirmation stage`() = runTest {
        stubStoredType(PasscodeType.Pin(4))
        initViewModel()
        subscribe()

        underTest.onPasscodeEntered("1234")

        assertThat(currentState().stage)
            .isEqualTo(PasscodeEntryStage.Confirmation("1234", showMismatchError = false))
    }

    @Test
    fun `test that onPasscodeEntered saves the passcode when the confirmation matches`() = runTest {
        stubStoredType(PasscodeType.Pin(4))
        stubTimeout(PasscodeTimeout.Immediate)
        initViewModel()
        subscribe()

        underTest.onPasscodeEntered("1234")
        underTest.onPasscodeEntered("1234")
        advanceUntilIdle()

        verify(setPasscodeUseCase).invoke(
            SetPasscodeRequest(type = PasscodeType.Pin(4), passcode = "1234")
        )
    }

    @Test
    fun `test that onPasscodeEntered shows a mismatch error when the confirmation does not match`() =
        runTest {
            stubStoredType(PasscodeType.Pin(4))
            initViewModel()
            subscribe()

            underTest.onPasscodeEntered("1234")
            underTest.onPasscodeEntered("5678")
            advanceUntilIdle()

            assertThat(currentState().stage)
                .isEqualTo(PasscodeEntryStage.Confirmation("1234", showMismatchError = true))
            verifyNoInteractions(setPasscodeUseCase)
        }

    @Test
    fun `test that onPasscodeEntered saves the passcode once when the confirmation is delivered twice`() =
        runTest {
            stubStoredType(PasscodeType.Pin(4))
            stubTimeout(PasscodeTimeout.Immediate)
            initViewModel()
            subscribe()

            underTest.onPasscodeEntered("1234")
            underTest.onPasscodeEntered("1234")
            underTest.onPasscodeEntered("1234")
            advanceUntilIdle()

            verify(setPasscodeUseCase, times(1)).invoke(any())
            assertThat(currentState().stage).isEqualTo(PasscodeEntryStage.Saving)
        }

    @Test
    fun `test that onPasscodeTypeSelected resets the stage to the first entry`() = runTest {
        stubStoredType(PasscodeType.Pin(4))
        initViewModel()
        subscribe()
        underTest.onPasscodeEntered("1234")

        underTest.onPasscodeTypeSelected(PasscodeTypeOption.SixDigits)
        advanceUntilIdle()

        assertThat(currentState().stage).isEqualTo(PasscodeEntryStage.FirstEntry)
        assertThat(currentState().passcodeType).isEqualTo(PasscodeUIType.Pin(false, 6))
    }

    @Test
    fun `test that the passcode is saved without a mismatch when the type is changed part way through`() =
        runTest {
            stubStoredType(PasscodeType.Pin(4))
            stubTimeout(PasscodeTimeout.Immediate)
            initViewModel()
            subscribe()

            underTest.onPasscodeEntered("1234")
            underTest.onPasscodeTypeSelected(PasscodeTypeOption.SixDigits)
            underTest.onPasscodeTypeSelected(PasscodeTypeOption.FourDigits)
            underTest.onPasscodeEntered("1111")
            underTest.onPasscodeEntered("1111")
            advanceUntilIdle()

            verify(setPasscodeUseCase, times(1)).invoke(
                SetPasscodeRequest(type = PasscodeType.Pin(4), passcode = "1111")
            )
            assertThat(currentState().stage).isEqualTo(PasscodeEntryStage.Saving)
        }

    @Test
    fun `test that the biometric passcode type is preserved when the passcode is changed`() =
        runTest {
            stubStoredType(PasscodeType.Biometric(PasscodeType.Pin(4)))
            stubTimeout(PasscodeTimeout.Immediate)
            initViewModel()
            subscribe()

            underTest.onPasscodeEntered("1234")
            underTest.onPasscodeEntered("1234")
            advanceUntilIdle()

            verify(setPasscodeUseCase).invoke(
                SetPasscodeRequest(
                    type = PasscodeType.Biometric(PasscodeType.Pin(4)),
                    passcode = "1234",
                )
            )
        }

    @Test
    fun `test that the biometric passcode type is preserved when the passcode type is changed`() =
        runTest {
            stubStoredType(PasscodeType.Biometric(PasscodeType.Pin(4)))
            stubTimeout(PasscodeTimeout.Immediate)
            initViewModel()
            subscribe()

            underTest.onPasscodeTypeSelected(PasscodeTypeOption.Alphanumeric)
            underTest.onPasscodeEntered("letmein")
            underTest.onPasscodeEntered("letmein")
            advanceUntilIdle()

            verify(setPasscodeUseCase).invoke(
                SetPasscodeRequest(
                    type = PasscodeType.Biometric(PasscodeType.Password),
                    passcode = "letmein",
                )
            )
        }

    @Test
    fun `test that the default timeout is set when no timeout exists`() = runTest {
        stubStoredType(PasscodeType.Pin(4))
        stubTimeout(null)
        initViewModel()
        subscribe()

        underTest.onPasscodeEntered("1234")
        underTest.onPasscodeEntered("1234")
        advanceUntilIdle()

        verify(setPasscodeTimeoutUseCase).invoke(PasscodeTimeout.DEFAULT)
    }

    @Test
    fun `test that the default timeout is not set when a timeout already exists`() = runTest {
        stubStoredType(PasscodeType.Pin(4))
        stubTimeout(PasscodeTimeout.Immediate)
        initViewModel()
        subscribe()

        underTest.onPasscodeEntered("1234")
        underTest.onPasscodeEntered("1234")
        advanceUntilIdle()

        verify(setPasscodeTimeoutUseCase, never()).invoke(any())
    }

    @Test
    fun `test that the passcode saved event is triggered when the passcode is saved`() = runTest {
        stubStoredType(PasscodeType.Pin(4))
        stubTimeout(PasscodeTimeout.Immediate)
        initViewModel()
        subscribe()

        underTest.onPasscodeEntered("1234")
        underTest.onPasscodeEntered("1234")
        advanceUntilIdle()

        assertThat(currentState().passcodeSavedEvent).isEqualTo(StateEvent.Triggered)
    }

    @Test
    fun `test that the stage returns to the first entry when saving fails`() = runTest {
        stubStoredType(PasscodeType.Pin(4))
        setPasscodeUseCase.stub {
            onBlocking { invoke(any()) }.thenThrow(RuntimeException("Save failed"))
        }
        initViewModel()
        subscribe()

        underTest.onPasscodeEntered("1234")
        underTest.onPasscodeEntered("1234")
        advanceUntilIdle()

        assertThat(currentState().stage).isEqualTo(PasscodeEntryStage.FirstEntry)
    }

    private fun stubStoredType(type: PasscodeType?) {
        monitorPasscodeTypeUseCase.stub { on { invoke() }.thenReturn(flowOf(type)) }
    }

    private fun stubTimeout(timeout: PasscodeTimeout?) {
        monitorPasscodeTimeOutUseCase.stub { on { invoke() }.thenReturn(flowOf(timeout)) }
    }

    private fun initViewModel() {
        underTest = SetupPasscodeViewModel(
            monitorPasscodeTypeUseCase = monitorPasscodeTypeUseCase,
            monitorPasscodeTimeOutUseCase = monitorPasscodeTimeOutUseCase,
            setPasscodeUseCase = setPasscodeUseCase,
            setPasscodeTimeoutUseCase = setPasscodeTimeoutUseCase,
            passcodeTypeMapper = passcodeTypeMapper,
            passcodeUITypeToDomainMapper = passcodeUITypeToDomainMapper,
        )
    }

    /**
     * Keeps [SetupPasscodeViewModel.uiState] hot for the duration of the test so that its value
     * reflects each interaction, rather than stopping between assertions.
     */
    private fun TestScope.subscribe() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            underTest.uiState.collect {}
        }
    }

    private fun currentState() = underTest.uiState.value as SetupPasscodeUiState.Data
}
