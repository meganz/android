package mega.privacy.android.feature.sync.presentation

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.usecase.IsOnboardingRequiredUseCase
import mega.privacy.android.feature.sync.ui.SyncViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncViewModelTest {

    private val isOnboardingRequiredUseCase: IsOnboardingRequiredUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()

    private lateinit var syncViewModel: SyncViewModel

    @AfterEach
    fun resetAndTearDown() {
        reset(
            isOnboardingRequiredUseCase,
            monitorConnectivityUseCase,
        )
    }

    @Test
    fun `test that when network is not connected state is changed to no network state`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))
        initViewModel()

        Truth.assertThat(syncViewModel.state.value.isNetworkConnected).isFalse()
    }

    @Test
    fun `test that when network is connected state is changed to content`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
        initViewModel()

        Truth.assertThat(syncViewModel.state.value.isNetworkConnected).isTrue()
    }

    private fun initViewModel() {
        syncViewModel = SyncViewModel(isOnboardingRequiredUseCase, monitorConnectivityUseCase)
    }
}