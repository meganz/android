package test.mega.privacy.android.app.presentation.settings.advanced

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.advanced.SettingsAdvancedViewModel
import mega.privacy.android.domain.usecase.IsUseHttpsEnabled
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.RootNodeExists
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

    private val rootNodeExists = mock<RootNodeExists>()

    private val monitorConnectivity = mock<MonitorConnectivity> {
        on { invoke() }.thenReturn(
            emptyFlow()
        )
    }

    private val setUseHttps = mock<SetUseHttps>()

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = SettingsAdvancedViewModel(
            isUseHttpsEnabled = isUseHttpsEnabled,
            rootNodeExists = rootNodeExists,
            monitorConnectivity = monitorConnectivity,
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
        whenever(monitorConnectivity()).thenReturn(flowOf(true))
        whenever(rootNodeExists()).thenReturn(true)

        underTest.state.map { it.useHttpsEnabled }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that checkbox becomes not enabled if network is lost`() = runTest {
        whenever(isUseHttpsEnabled()).thenReturn(true)
        whenever(monitorConnectivity()).thenReturn(flowOf(true, false))
        whenever(rootNodeExists()).thenReturn(true)

        underTest.state.map { it.useHttpsEnabled }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
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