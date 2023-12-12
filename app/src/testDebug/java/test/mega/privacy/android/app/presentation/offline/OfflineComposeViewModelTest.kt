package test.mega.privacy.android.app.presentation.offline

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.offline.offlinecompose.OfflineComposeViewModel
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.TransferFinishType
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.usecase.LoadOfflineNodesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersFinishedUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfflineComposeViewModelTest {
    private lateinit var underTest: OfflineComposeViewModel
    private val loadOfflineNodesUseCase = mock<LoadOfflineNodesUseCase>()
    private val setOfflineWarningMessageVisibilityUseCase =
        mock<SetOfflineWarningMessageVisibilityUseCase>()
    private val monitorOfflineWarningMessageVisibilityUseCase =
        mock<MonitorOfflineWarningMessageVisibilityUseCase>()
    private val monitorTransfersFinishedUseCase = mock<MonitorTransfersFinishedUseCase>()


    @BeforeAll
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            loadOfflineNodesUseCase,
            monitorTransfersFinishedUseCase,
            setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineWarningMessageVisibilityUseCase
        )
    }

    private fun initTestClass() {
        underTest = OfflineComposeViewModel(
            loadOfflineNodesUseCase,
            monitorTransfersFinishedUseCase,
            setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineWarningMessageVisibilityUseCase
        )
    }

    @Test
    fun `test that loadOfflineNodesUseCase will be is invoked when MonitorTransfersFinishedUseCase flow is collected`() =
        runTest {
            val transfer = TransfersFinishedState(type = TransferFinishType.DOWNLOAD_OFFLINE)
            val offlineNodes = listOf<OtherOfflineNodeInformation>(
                mock(),
                mock(),
            )
            val mutableFlow = MutableSharedFlow<TransfersFinishedState>()
            whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(flowOf(false))
            whenever(monitorTransfersFinishedUseCase()).thenReturn(mutableFlow)
            whenever(loadOfflineNodesUseCase(any(), any())).thenReturn(offlineNodes)
            initTestClass()
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.offlineNodes.map { it.offlineNode }).isEqualTo(offlineNodes)
            }
            whenever(loadOfflineNodesUseCase(any(), any())).thenReturn(emptyList())
            mutableFlow.emit(transfer)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.offlineNodes).isEmpty()
            }
        }

    @Test
    fun `test that dismissOfflineWarning will invoke setOfflineWarningMessageVisibilityUseCase`() {
        runTest {
            val expectedResult = false
            whenever(monitorTransfersFinishedUseCase()).thenReturn(flowOf(mock()))
            whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(flowOf(true))
            whenever(loadOfflineNodesUseCase("/", "")).thenReturn(emptyList())
            initTestClass()
            underTest.dismissOfflineWarning()
            testScheduler.advanceUntilIdle()
            verify(setOfflineWarningMessageVisibilityUseCase).invoke(expectedResult)
        }
    }

    @Test
    fun `test that showOfflineWarning equals to the same value that monitorOfflineWarningMessageVisibilityUseCase returns`() =
        runTest {
            val expected = false
            whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(flowOf(expected))
            whenever(monitorTransfersFinishedUseCase()).thenReturn(flowOf(mock()))
            whenever(loadOfflineNodesUseCase("/", "")).thenReturn(emptyList())
            initTestClass()
            underTest.monitorOfflineWarningMessage()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.showOfflineWarning).isEqualTo(expected)
            }
        }

    @Test
    fun `test that isLoading should be false when get fetching the offline nodes is successful`() =
        runTest {
            val path = "/"
            val searchQuery = ""
            whenever(monitorTransfersFinishedUseCase()).thenReturn(flowOf(mock()))
            whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(flowOf(true))
            whenever(loadOfflineNodesUseCase(path, searchQuery)).thenReturn(emptyList())
            initTestClass()
            underTest.loadOfflineNodes()
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.isLoading).isEqualTo(false)
            }
        }

    @Test
    fun `test that offlineNodes List should be null when get fetching the offline nodes fails`() =
        runTest {
            val path = "/"
            val searchQuery = ""
            whenever(loadOfflineNodesUseCase(path, searchQuery)).thenAnswer {
                throw Throwable()
            }
            whenever(monitorTransfersFinishedUseCase()).thenReturn(flowOf(mock()))
            whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(flowOf(true))
            initTestClass()
            underTest.loadOfflineNodes()
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.isLoading).isFalse()
                Truth.assertThat(state.offlineNodes).isEmpty()
            }
        }

    @Test
    fun `test that offlineNodes List should NOT be null when get fetching the offline nodes is successful`() =
        runTest {
            val path = "/"
            val searchQuery = ""
            whenever(loadOfflineNodesUseCase(path, searchQuery)).thenReturn(emptyList())
            whenever(monitorTransfersFinishedUseCase()).thenReturn(flowOf(mock()))
            whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(flowOf(true))
            initTestClass()
            underTest.loadOfflineNodes()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.offlineNodes).isNotNull()
            }
        }
}