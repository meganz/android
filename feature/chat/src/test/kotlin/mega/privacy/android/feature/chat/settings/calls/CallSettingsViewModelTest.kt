package mega.privacy.android.feature.chat.settings.calls

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.domain.usecase.call.MonitorCallSoundEnabledUseCase
import mega.privacy.android.domain.usecase.call.SetCallsSoundEnabledStateUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallSettingsViewModelTest {
    private lateinit var underTest: CallSettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val monitorCallSoundEnabledUseCase = mock<MonitorCallSoundEnabledUseCase>()
    private val setCallsSoundEnabledStateUseCase = mock<SetCallsSoundEnabledStateUseCase>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }


    private fun initialiseUnderTest() {
        underTest = CallSettingsViewModel(
            monitorCallSoundEnabledUseCase = monitorCallSoundEnabledUseCase,
            setCallSoundEnabledUseCase = setCallsSoundEnabledStateUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            monitorCallSoundEnabledUseCase,
            setCallsSoundEnabledStateUseCase,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that ui state is updated on start`() = runTest {
        monitorCallSoundEnabledUseCase.stub {
            on { invoke() } doReturn flow {
                emit(CallsSoundEnabledState.Enabled)
                awaitCancellation()
            }
        }
        initialiseUnderTest()
        underTest.uiState.test {
            assertThat(awaitItem().isSoundNotificationActive).isTrue()
        }
    }

    @Test
    fun `test that set call sounds use case is called with enabled if set sound notification is called with true`() =
        runTest {
            initialiseUnderTest()
            underTest.setSoundNotification(true)

            verify(setCallsSoundEnabledStateUseCase).invoke(CallsSoundEnabledState.Enabled)
        }

    @Test
    fun `test that set call sounds use case is called with disabled if set sound notification is called with false`() =
        runTest {
            initialiseUnderTest()
            underTest.setSoundNotification(false)

            verify(setCallsSoundEnabledStateUseCase).invoke(CallsSoundEnabledState.Disabled)
        }
}