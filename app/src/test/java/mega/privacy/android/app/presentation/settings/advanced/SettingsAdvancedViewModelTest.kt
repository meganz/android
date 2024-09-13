package mega.privacy.android.app.presentation.settings.advanced

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.IsUseHttpsEnabledUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.SetUseHttpsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsAdvancedViewModelTest {
    private lateinit var underTest: SettingsAdvancedViewModel

    private val isUseHttpsEnabled = mock<IsUseHttpsEnabledUseCase>()

    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()

    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase> {
        on { invoke() }.thenReturn(
            MutableStateFlow(true)
        )
    }

    private val setUseHttps = mock<SetUseHttpsUseCase>()

    @BeforeEach
    fun setUp() {
        initViewModel()
    }

    private fun initViewModel(
        isUseHttpsEnabledValue: Boolean = false,
        monitorConnectivityUseCaseValue: Boolean = false,
        rootNodeExistsUseCaseValue: Boolean = false,
    ) {
        isUseHttpsEnabled.stub {
            onBlocking { invoke() }.thenReturn(isUseHttpsEnabledValue)
        }
        monitorConnectivityUseCase.stub {
            on { invoke() }.thenReturn(monitorConnectivityUseCaseValue.asHotFlow())
        }
        rootNodeExistsUseCase.stub {
            onBlocking { invoke() }.thenReturn(rootNodeExistsUseCaseValue)
        }
        underTest = SettingsAdvancedViewModel(
            isUseHttpsEnabled = isUseHttpsEnabled,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            setUseHttps = setUseHttps,
            ioDispatcher = StandardTestDispatcher()
        )
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
        initViewModel(isUseHttpsEnabledValue = true)
        underTest.state.map { it.useHttpsChecked }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that checkbox is enabled if network connected and root node exists`() = runTest {
        initViewModel(
            isUseHttpsEnabledValue = true,
            monitorConnectivityUseCaseValue = true,
            rootNodeExistsUseCaseValue = true,
        )

        underTest.state.map { it.useHttpsEnabled }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that checkbox becomes not enabled if network is lost`() = runTest {

        initViewModel(
            isUseHttpsEnabledValue = true,
            rootNodeExistsUseCaseValue = true,
        )

        val monitorConnectivityStateFlow = MutableStateFlow(true)
        whenever(monitorConnectivityUseCase()).thenReturn(monitorConnectivityStateFlow)

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
        initViewModel()
        underTest.useHttpsPreferenceChanged(true)

        scheduler.advanceUntilIdle()

        verify(setUseHttps).invoke(true)
    }

    companion object {
        private val scheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(scheduler))
    }
}