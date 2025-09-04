package mega.privacy.android.app.presentation.settings.passcode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.app.presentation.settings.passcode.mapper.TimeoutOptionMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.passcode.DisableBiometricPasscodeUseCase
import mega.privacy.android.domain.usecase.passcode.DisablePasscodeUseCase
import mega.privacy.android.domain.usecase.passcode.EnableBiometricsUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTimeOutUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTypeUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeTimeoutUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PasscodeSettingsViewModelTest {
    private lateinit var underTest: PasscodeSettingsViewModel

    private val monitorPasscodeLockPreferenceUseCase = mock<MonitorPasscodeLockPreferenceUseCase>()
    private val monitorPasscodeTypeUseCase = mock<MonitorPasscodeTypeUseCase>()
    private val monitorPasscodeTimeOutUseCase = mock<MonitorPasscodeTimeOutUseCase>()
    private val timeoutOptionMapper = TimeoutOptionMapper()
    private val disablePasscodeUseCase = mock<DisablePasscodeUseCase>()
    private val disableBiometricPasscodeUseCase = mock<DisableBiometricPasscodeUseCase>()
    private val enableBiometricsUseCase = mock<EnableBiometricsUseCase>()
    private val setPasscodeTimeoutUseCase = mock<SetPasscodeTimeoutUseCase>()

    @BeforeEach
    internal fun setUp() {
        reset(
            monitorPasscodeLockPreferenceUseCase,
            monitorPasscodeTypeUseCase,
            setPasscodeTimeoutUseCase,
        )

        stubFlows()
    }

    private fun initUnderTest() {
        underTest = PasscodeSettingsViewModel(
            monitorPasscodeLockPreferenceUseCase = monitorPasscodeLockPreferenceUseCase,
            monitorPasscodeTypeUseCase = monitorPasscodeTypeUseCase,
            monitorPasscodeTimeOutUseCase = monitorPasscodeTimeOutUseCase,
            timeoutOptionMapper = timeoutOptionMapper,
            disablePasscodeUseCase = disablePasscodeUseCase,
            disableBiometricPasscodeUseCase = disableBiometricPasscodeUseCase,
            enableBiometricsUseCase = enableBiometricsUseCase,
            setPasscodeTimeoutUseCase = setPasscodeTimeoutUseCase
        )
    }

    @Test
    fun `test that enabled state is returned`() = runTest {
        stubFlows(timeOut = PasscodeTimeout.Immediate)

        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().isEnabled).isTrue()
        }
    }

    @Test
    fun `test that biometric state is returned as true if type is biometric`() = runTest {
        stubFlows(
            passcodeType = PasscodeType.Biometric(PasscodeType.Password),
            timeOut = PasscodeTimeout.Immediate
        )

        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().isBiometricsEnabled).isTrue()
        }
    }

    @Test
    fun `test that biometric state is returned as false if type is not biometric`() = runTest {
        stubFlows(
            timeOut = PasscodeTimeout.Immediate
        )

        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().isBiometricsEnabled).isFalse()
        }
    }

    @Test
    fun `test that passcode timeout option is returned`() = runTest {
        val input = PasscodeTimeout.TimeSpan(120000)
        val expected = timeoutOptionMapper(input)
        stubFlows(timeOut = input)

        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().timeout).isEqualTo(expected)
        }
    }

    @Test
    fun `test that disable passcode calls the correct use case`() = runTest {
        initUnderTest()

        underTest.disablePasscode()

        verify(disablePasscodeUseCase).invoke()
    }

    @Test
    fun `test that disable biometrics calls the correct use case`() =
        runTest {
            stubFlows(
                passcodeType = PasscodeType.Biometric(PasscodeType.Pin(4)),
                timeOut = PasscodeTimeout.Immediate
            )
            initUnderTest()

            underTest.disableBiometrics()

            verify(disableBiometricPasscodeUseCase).invoke()
        }

    @Test
    fun `test that enable biometrics calls the correct use case`() = runTest {
        stubFlows(
            passcodeType = PasscodeType.Pin(4),
            timeOut = PasscodeTimeout.Immediate
        )
        initUnderTest()

        underTest.enableBiometrics()

        verify(enableBiometricsUseCase).invoke()
    }

    @Test
    fun `test that default timeout is set if not found`() = runTest {
        stubFlows(timeOut = null)

        initUnderTest()

        underTest.onPasscodeEnabled()

        verify(setPasscodeTimeoutUseCase).invoke(PasscodeTimeout.DEFAULT)
    }

    @Test
    fun `test that timeout is cleared when passcode is disabled`() = runTest {
        val lockFlow = MutableStateFlow(true)
        monitorPasscodeLockPreferenceUseCase.stub {
            on { invoke() } doReturn lockFlow
        }
        monitorPasscodeTypeUseCase.stub {
            on { invoke() } doReturn PasscodeType.Password.asHotFlow()
        }
        monitorPasscodeTimeOutUseCase.stub {
            on { invoke() } doReturn PasscodeTimeout.Immediate.asHotFlow()
        }

        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().timeout).isNotNull()
            lockFlow.emit(false)
            assertThat(awaitItem().timeout).isNull()
        }


    }

    private fun stubFlows(
        isEnabled: Boolean = true,
        passcodeType: PasscodeType = PasscodeType.Password,
        timeOut: PasscodeTimeout? = PasscodeTimeout.Immediate,
    ) {
        monitorPasscodeLockPreferenceUseCase.stub {
            on { invoke() } doReturn isEnabled.asHotFlow()
        }
        monitorPasscodeTypeUseCase.stub {
            on { invoke() } doReturn passcodeType.asHotFlow()
        }
        monitorPasscodeTimeOutUseCase.stub {
            on { invoke() } doReturn timeOut.asHotFlow()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(UnconfinedTestDispatcher())
    }

}