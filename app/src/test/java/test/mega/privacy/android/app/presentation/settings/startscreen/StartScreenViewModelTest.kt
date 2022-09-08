package test.mega.privacy.android.app.presentation.settings.startscreen

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.startscreen.StartScreenViewModel
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOption
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOptionMapper
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.SetStartScreenPreference
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class StartScreenViewModelTest {
    private lateinit var underTest: StartScreenViewModel

    private val monitorStartScreenPreference = mock<MonitorStartScreenPreference> {
        on { invoke() }.thenReturn(emptyFlow())
    }

    private val setStartScreenPreference = mock<SetStartScreenPreference>()

    private val mapStartScreenOption = mock<StartScreenOptionMapper> {
        on { invoke(any()) }.thenAnswer {
            val startScreen = it.arguments[0] as StartScreen
            StartScreenOption(startScreen, 0, 0)
        }
    }

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = StartScreenViewModel(
            monitorStartScreenPreference = monitorStartScreenPreference,
            setStartScreenPreference = setStartScreenPreference,
            startScreenOptionMapper = mapStartScreenOption,
            eventBus = {},
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial value has no screen selected`() = runTest {
        underTest.state.test {
            val (_, selectedScreen) = awaitItem()
            assertThat(selectedScreen).isEqualTo(StartScreen.None)
        }
    }

    @Test
    fun `test that all values except None is included as an option`() = runTest {
        underTest.state.test {
            val (options, _) = awaitItem()
            assertThat(options.map { it.startScreen }).containsExactlyElementsIn(StartScreen.values()
                .filterNot { it == StartScreen.None })
        }
    }

    @Test
    fun `test that selected screen is set if returned`() = runTest {
        val expected = StartScreen.Chat
        whenever(monitorStartScreenPreference()).thenReturn(flowOf(expected))
        underTest.state
            .map { it.selectedScreen }
            .drop(1)
            .test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
    }

    @Test
    fun `test that set use case is called when new screen is set`() {
        val expected = StartScreen.Photos
        underTest.newScreenClicked(expected)
        scheduler.advanceUntilIdle()
        verifyBlocking(setStartScreenPreference) { invoke(expected) }
    }
}