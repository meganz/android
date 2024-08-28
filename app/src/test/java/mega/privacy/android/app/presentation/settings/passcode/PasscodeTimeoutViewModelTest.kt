package mega.privacy.android.app.presentation.settings.passcode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.app.presentation.settings.passcode.mapper.PasscodeTimeoutMapper
import mega.privacy.android.app.presentation.settings.passcode.mapper.TimeoutOptionMapper
import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTimeOutUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeTimeoutUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class PasscodeTimeoutViewModelTest {
    private lateinit var underTest: PasscodeTimeoutViewModel

    private val monitorPasscodeTimeOutUseCase = mock<MonitorPasscodeTimeOutUseCase>()
    private val timeoutOptionMapper = mock<TimeoutOptionMapper>()
    private val setPasscodeTimeoutUseCase = mock<SetPasscodeTimeoutUseCase>()
    private val passcodeTimeoutMapper = mock<PasscodeTimeoutMapper>()

    @BeforeEach
    internal fun setUp() {
        reset(
            monitorPasscodeTimeOutUseCase,
            timeoutOptionMapper,
            setPasscodeTimeoutUseCase,
            passcodeTimeoutMapper,
        )
    }


    private fun initUnderTest() {
        underTest = PasscodeTimeoutViewModel(
            monitorPasscodeTimeOutUseCase = monitorPasscodeTimeOutUseCase,
            timeoutOptionMapper = timeoutOptionMapper,
            setPasscodeTimeoutUseCase = setPasscodeTimeoutUseCase,
            passcodeTimeoutMapper = passcodeTimeoutMapper,
        )
    }

    @Test
    fun `test that initial state has no selected value`() = runTest {
        monitorPasscodeTimeOutUseCase.stub {
            on { invoke() } doReturn null.asHotFlow()
        }
        initUnderTest()
        underTest.state.test {
            assertThat(awaitItem().currentOption).isNull()
        }
    }

    @Test
    fun `test that correct values are listed`() = runTest {
        monitorPasscodeTimeOutUseCase.stub {
            on { invoke() } doReturn null.asHotFlow()
        }
        initUnderTest()
        underTest.state.test {
            assertThat(awaitItem().options).containsExactly(
                TimeoutOption.Immediate,
                TimeoutOption.SecondsTimeSpan(5),
                TimeoutOption.SecondsTimeSpan(10),
                TimeoutOption.SecondsTimeSpan(30),
                TimeoutOption.MinutesTimeSpan(1),
                TimeoutOption.MinutesTimeSpan(2),
                TimeoutOption.MinutesTimeSpan(5),
            )
        }
    }

    @Test
    fun `test that selected value is set if returned`() = runTest {
        monitorPasscodeTimeOutUseCase.stub {
            on { invoke() } doReturn PasscodeTimeout.Immediate.asHotFlow()
        }
        val expectedOption = TimeoutOption.Immediate
        timeoutOptionMapper.stub {
            on { invoke(any()) } doReturn expectedOption
        }

        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().currentOption).isEqualTo(expectedOption)
        }
    }

    @Test
    fun `test that correct use case is called when timeout is selected`() = runTest {
        monitorPasscodeTimeOutUseCase.stub {
            on { invoke() } doReturn null.asHotFlow()
        }
        val expectedTimeout = PasscodeTimeout.Immediate
        passcodeTimeoutMapper.stub {
            on { invoke(any()) } doReturn expectedTimeout
        }

        initUnderTest()

        val option = TimeoutOption.Immediate
        underTest.onTimeoutSelected(option)

        verify(passcodeTimeoutMapper).invoke(option)
        verify(setPasscodeTimeoutUseCase).invoke(expectedTimeout)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(UnconfinedTestDispatcher())
    }

}