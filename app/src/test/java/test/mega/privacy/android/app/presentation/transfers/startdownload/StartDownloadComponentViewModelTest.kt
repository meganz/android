package test.mega.privacy.android.app.presentation.transfers.startdownload

import android.net.Uri
import com.google.common.truth.Truth
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.TransfersConstants
import mega.privacy.android.app.presentation.transfers.startdownload.StartDownloadComponentViewModel
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferEvent
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.file.TotalFileSizeOfNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.setting.IsAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.setting.SetAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorActiveTransferFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.SaveDoNotPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldAskDownloadDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWithWorkerUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartDownloadComponentViewModelTest {

    private lateinit var underTest: StartDownloadComponentViewModel

    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase = mock()
    private val startDownloadsWithWorkerUseCase: StartDownloadsWithWorkerUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val clearActiveTransfersIfFinishedUseCase =
        mock<ClearActiveTransfersIfFinishedUseCase>()
    private val totalFileSizeOfNodesUseCase = mock<TotalFileSizeOfNodesUseCase>()
    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val isAskBeforeLargeDownloadsSettingUseCase =
        mock<IsAskBeforeLargeDownloadsSettingUseCase>()
    private val setAskBeforeLargeDownloadsSettingUseCase =
        mock<SetAskBeforeLargeDownloadsSettingUseCase>()
    private val getOrCreateStorageDownloadLocationUseCase =
        mock<GetOrCreateStorageDownloadLocationUseCase>()
    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()
    private val getCurrentDownloadSpeedUseCase = mock<GetCurrentDownloadSpeedUseCase>()
    private val getFilePreviewDownloadPathUseCase = mock<GetFilePreviewDownloadPathUseCase>()
    private val shouldAskDownloadDestinationUseCase = mock<ShouldAskDownloadDestinationUseCase>()
    private val shouldPromptToSaveDestinationUseCase = mock<ShouldPromptToSaveDestinationUseCase>()
    private val saveDoNotPromptToSaveDestinationUseCase =
        mock<SaveDoNotPromptToSaveDestinationUseCase>()
    private val setStorageDownloadAskAlwaysUseCase = mock<SetStorageDownloadAskAlwaysUseCase>()
    private val setStorageDownloadLocationUseCase = mock<SetStorageDownloadLocationUseCase>()
    private val monitorActiveTransferFinishedUseCase = mock<MonitorActiveTransferFinishedUseCase>()


    private val node: TypedFileNode = mock()
    private val nodes = listOf(node)
    private val parentNode: TypedFolderNode = mock()

    @BeforeAll
    fun setup() {
        initialStub()
        underTest = StartDownloadComponentViewModel(
            getOfflinePathForNodeUseCase,
            getOrCreateStorageDownloadLocationUseCase,
            getFilePreviewDownloadPathUseCase,
            startDownloadsWithWorkerUseCase,
            clearActiveTransfersIfFinishedUseCase,
            isConnectedToInternetUseCase,
            totalFileSizeOfNodesUseCase,
            fileSizeStringMapper,
            isAskBeforeLargeDownloadsSettingUseCase,
            setAskBeforeLargeDownloadsSettingUseCase,
            monitorOngoingActiveTransfersUseCase,
            getCurrentDownloadSpeedUseCase,
            shouldAskDownloadDestinationUseCase,
            shouldPromptToSaveDestinationUseCase,
            saveDoNotPromptToSaveDestinationUseCase,
            setStorageDownloadAskAlwaysUseCase,
            setStorageDownloadLocationUseCase,
            monitorActiveTransferFinishedUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getOrCreateStorageDownloadLocationUseCase,
            startDownloadsWithWorkerUseCase,
            getOfflinePathForNodeUseCase,
            isConnectedToInternetUseCase,
            node,
            parentNode,
            clearActiveTransfersIfFinishedUseCase,
            totalFileSizeOfNodesUseCase,
            fileSizeStringMapper,
            isAskBeforeLargeDownloadsSettingUseCase,
            setAskBeforeLargeDownloadsSettingUseCase,
            monitorOngoingActiveTransfersUseCase,
            getCurrentDownloadSpeedUseCase,
            shouldAskDownloadDestinationUseCase,
            shouldPromptToSaveDestinationUseCase,
            saveDoNotPromptToSaveDestinationUseCase,
            setStorageDownloadAskAlwaysUseCase,
            setStorageDownloadLocationUseCase,
        )
        initialStub()
    }

    private fun initialStub() {
        whenever(monitorOngoingActiveTransfersUseCase(any())).thenReturn(emptyFlow())
        whenever(monitorActiveTransferFinishedUseCase(any())).thenReturn(emptyFlow())
    }

    @ParameterizedTest
    @MethodSource("provideStartEvents")
    fun `test that clearActiveTransfersIfFinishedUseCase is invoked when startDownloadNode is invoked`(
        startEvent: TransferTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.startDownload(startEvent)
        verify(clearActiveTransfersIfFinishedUseCase).invoke(TransferType.DOWNLOAD)
    }

    @ParameterizedTest
    @MethodSource("provideStartEvents")
    fun `test that start download use case is invoked with correct parameters when startDownloadNode is invoked`(
        startEvent: TransferTriggerEvent,
    ) = runTest {
        commonStub()
        if (startEvent is TransferTriggerEvent.StartDownloadForOffline) {
            whenever(getOfflinePathForNodeUseCase(any())).thenReturn(destination)
        } else if (startEvent is TransferTriggerEvent.StartDownloadForPreview) {
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn(destination)
        }
        underTest.startDownload(startEvent)
        verify(startDownloadsWithWorkerUseCase).invoke(
            nodes,
            destination,
            startEvent.isHighPriority
        )
    }

    @Test
    fun `test that no connection event is emitted when monitorConnectivityUseCase is false`() =
        runTest {
            commonStub()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            underTest.startDownload(TransferTriggerEvent.StartDownloadNode(nodes))
            assertCurrentEventIsEqualTo(StartDownloadTransferEvent.NotConnected)
        }

    @Test
    fun `test that cancel event is emitted when start download nodes is invoked with no sibling nodes`() =
        runTest {
            commonStub()
            whenever(parentNode.parentId).thenReturn(NodeId(55L))
            underTest.startDownload(
                TransferTriggerEvent.StartDownloadNode(listOf(node, parentNode))
            )
            assertCurrentEventIsEqualTo(StartDownloadTransferEvent.Message.TransferCancelled)
        }

    @Test
    fun `test that cancel event is emitted when start download nodes is invoked with empty list`() =
        runTest {
            commonStub()
            underTest.startDownload(
                TransferTriggerEvent.StartDownloadNode(listOf())
            )
            assertCurrentEventIsEqualTo(StartDownloadTransferEvent.Message.TransferCancelled)
        }

    @Test
    fun `test that cancel event is emitted when start download nodes is invoked with null node`() =
        runTest {
            commonStub()
            underTest.startDownload(
                TransferTriggerEvent.StartDownloadForOffline(null)
            )
            assertCurrentEventIsEqualTo(StartDownloadTransferEvent.Message.TransferCancelled)
        }

    @Test
    fun `test that job in progress is set to ProcessingFiles when start download use case starts`() =
        runTest {
            commonStub()
            stubStartDownload(flow {
                delay(500)
                emit(mock<MultiTransferEvent.SingleTransferEvent> {
                    on { scanningFinished } doReturn true
                })
            })
            underTest.startDownload(TransferTriggerEvent.StartDownloadNode(nodes))
            Truth.assertThat(underTest.uiState.value.jobInProgressState)
                .isEqualTo(StartDownloadTransferJobInProgress.ProcessingFiles)
        }

    @Test
    fun `test that FinishProcessing event is emitted if start download use case finishes correctly`() =
        runTest {
            commonStub()
            underTest.startDownload(TransferTriggerEvent.StartDownloadNode(nodes))
            assertCurrentEventIsEqualTo(StartDownloadTransferEvent.FinishProcessing(null, 1, 0, 0))
        }

    @Test
    fun `test that NotSufficientSpace event is emitted if start download use case returns NotSufficientSpace`() =
        runTest {
            commonStub()
            stubStartDownload(flowOf(MultiTransferEvent.InsufficientSpace))
            underTest.startDownload(TransferTriggerEvent.StartDownloadNode(nodes))
            assertCurrentEventIsEqualTo(StartDownloadTransferEvent.Message.NotSufficientSpace)
        }

    @ParameterizedTest(name = "when StartDownloadUseCase finishes with {0}, then {1} is emitted")
    @MethodSource("provideDownloadNodeParameters")
    fun `test that a specific StartDownloadTransferEvent is emitted`(
        multiTransferEvent: MultiTransferEvent,
        startDownloadTransferEvent: StartDownloadTransferEvent,
    ) = runTest {
        commonStub()
        stubStartDownload(flowOf(multiTransferEvent))
        underTest.startDownload(TransferTriggerEvent.StartDownloadNode(nodes))
        assertCurrentEventIsEqualTo(startDownloadTransferEvent)
    }

    @ParameterizedTest
    @MethodSource("provideStartEvents")
    fun `test that ConfirmLargeDownload is emitted when a large download is started`(
        startEvent: TransferTriggerEvent,
    ) = runTest {
        commonStub()
        whenever(isAskBeforeLargeDownloadsSettingUseCase()).thenReturn(true)
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(TransfersConstants.CONFIRM_SIZE_MIN_BYTES + 1)
        val size = "x MB"
        whenever(fileSizeStringMapper(any())).thenReturn(size)
        underTest.startDownload(startEvent)
        assertCurrentEventIsEqualTo(
            StartDownloadTransferEvent.ConfirmLargeDownload(size, startEvent)
        )
    }

    @ParameterizedTest
    @MethodSource("provideStartEvents")
    fun `test that setAskBeforeLargeDownloadsSettingUseCase is invoked when specified in start download`(
        startEvent: TransferTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.startDownloadWithoutConfirmation(startEvent, true)
        verify(setAskBeforeLargeDownloadsSettingUseCase).invoke(false)
    }

    @ParameterizedTest
    @MethodSource("provideStartEvents")
    fun `test that setAskBeforeLargeDownloadsSettingUseCase is not invoked when not specified in start download`(
        startEvent: TransferTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.startDownloadWithoutConfirmation(startEvent, false)
        verifyNoInteractions(setAskBeforeLargeDownloadsSettingUseCase)
    }

    @Test
    fun `test that AskDestination event is triggered when a download starts and shouldAskDownloadDestinationUseCase is true`() =
        runTest {
            commonStub()
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            val event = TransferTriggerEvent.StartDownloadNode(nodes)
            underTest.startDownloadWithoutConfirmation(event)
            assertCurrentEventIsEqualTo(StartDownloadTransferEvent.AskDestination(event))
        }

    @Test
    fun `test that promptSaveDestination state is updated when startDownloadWithDestination is invoked and shouldPromptToSaveDestinationUseCase is true`() =
        runTest {
            commonStub()
            val uriString = "content:/destination"
            val destinationUri = mock<Uri> {
                on { toString() } doReturn uriString
            }
            val startDownloadNode = TransferTriggerEvent.StartDownloadNode(nodes)
            whenever(shouldPromptToSaveDestinationUseCase()).thenReturn(true)

            underTest.startDownloadWithDestination(startDownloadNode, destinationUri)

            Truth.assertThat(underTest.uiState.value.promptSaveDestination)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            Truth.assertThat((underTest.uiState.value.promptSaveDestination as StateEventWithContentTriggered).content)
                .isEqualTo(uriString)

        }

    @Test
    fun `test that saveDoNotPromptToSaveDestinationUseCase is invoked when doNotPromptToSaveDestinationAgain is invoked`() =
        runTest {
            underTest.doNotPromptToSaveDestinationAgain()
            verify(saveDoNotPromptToSaveDestinationUseCase).invoke()
        }

    @Test
    fun `test that setStorageDownloadLocationUseCase is invoked when saveDestination is invoked`() =
        runTest {
            val destination = "destination"
            underTest.saveDestination(destination)
            verify(setStorageDownloadLocationUseCase).invoke(destination)
        }

    @Test
    fun `test that setStorageDownloadAskAlwaysUseCase is set to false when saveDestination is invoked`() =
        runTest {
            underTest.saveDestination("destination")
            verify(setStorageDownloadAskAlwaysUseCase).invoke(false)
        }

    private fun provideDownloadNodeParameters() = listOf(
        Arguments.of(
            mock<MultiTransferEvent.SingleTransferEvent> {
                on { scanningFinished } doReturn true
            },
            StartDownloadTransferEvent.FinishProcessing(null, 1, 0, 0),
        ),
        Arguments.of(
            MultiTransferEvent.InsufficientSpace,
            StartDownloadTransferEvent.Message.NotSufficientSpace,
        ),
    )

    private fun provideStartEvents() = listOf(
        TransferTriggerEvent.StartDownloadNode(nodes),
        TransferTriggerEvent.StartDownloadForOffline(node),
        TransferTriggerEvent.StartDownloadForPreview(node),
    )

    private fun assertCurrentEventIsEqualTo(event: StartDownloadTransferEvent) {
        Truth.assertThat(underTest.uiState.value.oneOffViewEvent)
            .isInstanceOf(StateEventWithContentTriggered::class.java)
        Truth.assertThat((underTest.uiState.value.oneOffViewEvent as StateEventWithContentTriggered).content)
            .isEqualTo(event)
    }

    private suspend fun commonStub() {
        whenever(isAskBeforeLargeDownloadsSettingUseCase()).thenReturn(false)
        whenever(node.id).thenReturn(nodeId)
        whenever(node.parentId).thenReturn(parentId)
        whenever(parentNode.id).thenReturn(parentId)

        whenever(getOrCreateStorageDownloadLocationUseCase()).thenReturn(destination)

        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(1)
        whenever(shouldAskDownloadDestinationUseCase()).thenReturn(false)
        stubStartDownload(
            flowOf(
                mock<MultiTransferEvent.SingleTransferEvent> {
                    on { scanningFinished } doReturn true
                })
        )
    }

    private fun stubStartDownload(flow: Flow<MultiTransferEvent>) {
        whenever(
            startDownloadsWithWorkerUseCase(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(flow)
    }

    companion object {
        private const val NODE_HANDLE = 10L
        private const val PARENT_NODE_HANDLE = 12L
        private val nodeId = NodeId(NODE_HANDLE)
        private val parentId = NodeId(PARENT_NODE_HANDLE)
        private const val destination = "/destination/"
    }
}
