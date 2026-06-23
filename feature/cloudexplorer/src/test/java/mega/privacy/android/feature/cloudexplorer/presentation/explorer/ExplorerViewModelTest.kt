package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExplorerViewModelTest {

    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    @AfterEach
    fun resetMocks() {
        reset(monitorConnectivityUseCase)
    }

    private fun buildViewModel(connectivity: Flow<Boolean> = flowOf(true)): ExplorerViewModel {
        whenever(monitorConnectivityUseCase()).thenReturn(connectivity)
        return ExplorerViewModel(monitorConnectivityUseCase)
    }

    @Test
    fun `test that uiState starts as the default loading state`() = runTest {
        val underTest = buildViewModel()
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(ExplorerUiState())
        }
    }

    @Test
    fun `test that uiState reflects the active tab signal`() = runTest {
        val underTest = buildViewModel()
        val folder = LocalizedText.Literal("Documents")
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(ExplorerUiState())

            underTest.onTabSignal(
                CLOUD_TAB_INDEX,
                TabSignal(
                    isLoading = false,
                    hasContent = true,
                    folderName = folder,
                ),
            )

            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.hasContent).isTrue()
            assertThat(state.folderName).isEqualTo(folder)
        }
    }

    @Test
    fun `test that uiState surfaces the selected tab when the tab changes`() = runTest {
        val underTest = buildViewModel()
        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(ExplorerUiState())

            underTest.onTabSignal(
                CLOUD_TAB_INDEX,
                TabSignal(isLoading = false, hasContent = true),
            )
            assertThat(awaitItem().hasContent).isTrue()

            underTest.onTabSignal(
                INCOMING_TAB_INDEX,
                TabSignal(isLoading = false, hasContent = false),
            )
            underTest.onTabSelected(INCOMING_TAB_INDEX)
            assertThat(awaitItem().hasContent).isFalse()
        }
    }

    @Test
    fun `test that uiState surfaces disconnected when connectivity is offline`() = runTest {
        val underTest = buildViewModel(flowOf(false))
        underTest.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem().isConnected).isFalse()
        }
    }

    @Test
    fun `test that noConnectionEvent is triggered when the screen opens offline`() = runTest {
        val underTest = buildViewModel(flowOf(false))
        underTest.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem().noConnectionEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that noConnectionEvent stays consumed when the screen opens online`() = runTest {
        val underTest = buildViewModel(flowOf(true))
        underTest.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem().noConnectionEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that noConnectionEvent is not triggered when the connection drops after the screen opens`() =
        runTest {
            val connectivity = MutableStateFlow(true)
            val underTest = buildViewModel(connectivity)
            underTest.uiState.test {
                advanceUntilIdle()
                assertThat(expectMostRecentItem().noConnectionEvent).isEqualTo(consumed)

                connectivity.value = false
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.isConnected).isFalse()
                assertThat(state.noConnectionEvent).isEqualTo(consumed)
            }
        }

    @Test
    fun `test that onNoConnectionEventConsumed consumes the event`() = runTest {
        val underTest = buildViewModel(flowOf(false))
        underTest.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem().noConnectionEvent).isEqualTo(triggered)

            underTest.onNoConnectionEventConsumed()
            advanceUntilIdle()
            assertThat(expectMostRecentItem().noConnectionEvent).isEqualTo(consumed)
        }
    }
}
