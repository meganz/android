package test.mega.privacy.android.app.presentation.settings.advanced

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.advanced.SettingsAdvancedViewModel
import mega.privacy.android.domain.usecase.IsUseHttpsEnabled
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.SetUseHttps
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsAdvancedViewModelTest {
    private lateinit var underTest: SettingsAdvancedViewModel

    private val isUseHttpsEnabled = mock<IsUseHttpsEnabled>()

    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()

    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase> {
        on { invoke() }.thenReturn(
            MutableStateFlow(true)
        )
    }

    private val setUseHttps = mock<SetUseHttps>()

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = SettingsAdvancedViewModel(
            isUseHttpsEnabled = isUseHttpsEnabled,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            setUseHttps = setUseHttps,
            ioDispatcher = StandardTestDispatcher()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state has checkbox unchecked`() = runTest {
        underTest.state.test {
            assertThat(awaitItem().useHttpsChecked).isFalse()
        }
    }

    @Test
    fun `test that initial state has checkbox not enabled`() = runTest {
        underTest.state.test {
            assertThat(awaitItem().useHttpsEnabled).isFalse()
        }
    }

    @Test
    fun `test that checkbox is checked if preference is set to true`() = runTest {
        whenever(isUseHttpsEnabled()).thenReturn(true)

        underTest.state.map { it.useHttpsChecked }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that checkbox is enabled if network connected and root node exists`() = runTest {
        whenever(isUseHttpsEnabled()).thenReturn(true)
        whenever(monitorConnectivityUseCase()).thenReturn(MutableStateFlow(true))
        whenever(rootNodeExistsUseCase()).thenReturn(true)

        underTest.state.map { it.useHttpsEnabled }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that checkbox becomes not enabled if network is lost`() = runTest {
        val monitorConnectivityStateFlow = MutableStateFlow(true)
        whenever(isUseHttpsEnabled()).thenReturn(true)
        whenever(monitorConnectivityUseCase()).thenReturn(monitorConnectivityStateFlow)
        whenever(rootNodeExistsUseCase()).thenReturn(true)
        underTest.state.map { it.useHttpsEnabled }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
        monitorConnectivityStateFlow.update { false }
        runCurrent()
        underTest.state.map { it.useHttpsEnabled }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that preference is set to true when checkbox is checked`() = runTest {
        underTest.useHttpsPreferenceChanged(true)

        scheduler.advanceUntilIdle()

        verify(setUseHttps).invoke(true)
    }

}