package mega.privacy.android.app.presentation.transfers.startdownload

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.triggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.TransfersConstants
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.ConfirmLargeDownloadInfo
import mega.privacy.android.app.presentation.transfers.starttransfer.model.SaveDestinationInfo
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.usecase.SetAskForDownloadLocationUseCase
import mega.privacy.android.domain.usecase.SetDownloadLocationUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import mega.privacy.android.domain.usecase.file.TotalFileSizeOfNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.setting.IsAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.setting.SetAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.DeleteCacheFilesUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.SetAskedResumeTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.ShouldAskForResumeTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransfersByIdUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateDownloadLocationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.SaveDoNotPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldAskDownloadDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWorkerAndWaitUntilIsStartedUseCase
import mega.privacy.android.domain.usecase.transfers.filespermission.MonitorRequestFilesPermissionDeniedUseCase
import mega.privacy.android.domain.usecase.transfers.filespermission.SetRequestFilesPermissionDeniedUseCase
import mega.privacy.android.domain.usecase.transfers.offline.SaveOfflineNodesToDevice
import mega.privacy.android.domain.usecase.transfers.offline.SaveUriToDeviceUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import mega.privacy.android.domain.usecase.transfers.pending.DeleteAllPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingDownloadsForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingUploadsForFilesUseCase
import mega.privacy.android.domain.usecase.transfers.pending.MonitorPendingTransfersUntilResolvedUseCase
import mega.privacy.android.domain.usecase.transfers.previews.BroadcastTransferTagToCancelUseCase
import mega.privacy.android.domain.usecase.transfers.previews.MonitorTransferTagToCancelUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetCurrentUploadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadsWorkerAndWaitUntilIsStartedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartTransfersComponentViewModelTest {

    private lateinit var underTest: StartTransfersComponentViewModel

    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val clearActiveTransfersIfFinishedUseCase =
        mock<ClearActiveTransfersIfFinishedUseCase>()
    private val totalFileSizeOfNodesUseCase = mock<TotalFileSizeOfNodesUseCase>()
    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val isAskBeforeLargeDownloadsSettingUseCase =
        mock<IsAskBeforeLargeDownloadsSettingUseCase>()
    private val setAskBeforeLargeDownloadsSettingUseCase =
        mock<SetAskBeforeLargeDownloadsSettingUseCase>()
    private val getOrCreateDownloadLocationUseCase =
        mock<GetOrCreateDownloadLocationUseCase>()
    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()
    private val getCurrentDownloadSpeedUseCase = mock<GetCurrentDownloadSpeedUseCase>()
    private val getFilePreviewDownloadPathUseCase = mock<GetFilePreviewDownloadPathUseCase>()
    private val shouldAskDownloadDestinationUseCase = mock<ShouldAskDownloadDestinationUseCase>()
    private val shouldPromptToSaveDestinationUseCase = mock<ShouldPromptToSaveDestinationUseCase>()
    private val saveDoNotPromptToSaveDestinationUseCase =
        mock<SaveDoNotPromptToSaveDestinationUseCase>()
    private val setAskForDownloadLocationUseCase = mock<SetAskForDownloadLocationUseCase>()
    private val setDownloadLocationUseCase = mock<SetDownloadLocationUseCase>()
    private val sendChatAttachmentsUseCase = mock<SendChatAttachmentsUseCase>()
    private val shouldAskForResumeTransfersUseCase = mock<ShouldAskForResumeTransfersUseCase>()
    private val setAskedResumeTransfersUseCase = mock<SetAskedResumeTransfersUseCase>()
    private val pauseTransfersQueueUseCase = mock<PauseTransfersQueueUseCase>()
    private val saveOfflineNodesToDevice = mock<SaveOfflineNodesToDevice>()
    private val saveUriToDeviceUseCase = mock<SaveUriToDeviceUseCase>()
    private val getCurrentUploadSpeedUseCase = mock<GetCurrentUploadSpeedUseCase>()
    private val cancelCancelTokenUseCase = mock<CancelCancelTokenUseCase>()
    private val monitorRequestFilesPermissionDeniedUseCase =
        mock<MonitorRequestFilesPermissionDeniedUseCase> {
            on { invoke() } doReturn emptyFlow()
        }
    private val setRequestFilesPermissionDeniedUseCase =
        mock<SetRequestFilesPermissionDeniedUseCase>()
    private val startDownloadsWorkerAndWaitUntilIsStartedUseCase =
        mock<StartDownloadsWorkerAndWaitUntilIsStartedUseCase>()
    private val deleteAllPendingTransfersUseCase = mock<DeleteAllPendingTransfersUseCase>()
    private val monitorPendingTransfersUntilResolvedUseCase =
        mock<MonitorPendingTransfersUntilResolvedUseCase>()
    private val monitorStorageOverQuotaUseCase = mock<MonitorStorageOverQuotaUseCase> {
        on { invoke() } doReturn emptyFlow()
    }
    private val invalidateCancelTokenUseCase = mock<InvalidateCancelTokenUseCase>()
    private val insertPendingUploadsForFilesUseCase = mock<InsertPendingUploadsForFilesUseCase>()
    private val startUploadsWorkerAndWaitUntilIsStartedUseCase =
        mock<StartUploadsWorkerAndWaitUntilIsStartedUseCase>()
    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()
    private val insertPendingDownloadsForNodesUseCase =
        mock<InsertPendingDownloadsForNodesUseCase>()
    private val areTransfersPausedUseCase = mock<AreTransfersPausedUseCase>()
    private val getFileNameFromStringUriUseCase = mock<GetFileNameFromStringUriUseCase>()
    private val cancelTransferByTagUseCase = mock<CancelTransferByTagUseCase>()
    private val deleteCacheFilesUseCase = mock<DeleteCacheFilesUseCase>()
    private val getTransferByTagUseCase = mock<GetTransferByTagUseCase>()
    private val monitorTransferTagToCancelUseCase = mock<MonitorTransferTagToCancelUseCase> {
        on { invoke() } doReturn emptyFlow()
    }
    private val broadcastTransferTagToCancelUseCase = mock<BroadcastTransferTagToCancelUseCase>()
    private val deleteCompletedTransfersByIdUseCase = mock<DeleteCompletedTransfersByIdUseCase>()
    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase>()
    private val ratingHandlerImpl = mock<RatingHandlerImpl>()

    private val node: TypedFileNode = mock()
    private val nodes = listOf(node)
    private val parentNode: TypedFolderNode = mock()
    private val startDownloadEvent = TransferTriggerEvent.StartDownloadNode(
        nodes = nodes,
        withStartMessage = false,
    )
    private val startOfflineDownloadEvent = TransferTriggerEvent.StartDownloadForOffline(
        node = nodes.first(),
        withStartMessage = false,
    )
    private val startUploadFilesEvent =
        TransferTriggerEvent.StartUpload.Files(mapOf(DESTINATION to null), parentId)
    private val startUploadTextFileEvent = TransferTriggerEvent.StartUpload.TextFile(
        DESTINATION,
        parentId,
        isEditMode = false,
        fromHomePage = false
    )

    private val startUploadEvent =
        TransferTriggerEvent.StartUpload.Files(mapOf("foo" to null), NodeId(34678L))

    @BeforeAll
    fun setup() {
        initialStub()
        initTest()
    }

    private fun initTest() {
        underTest = StartTransfersComponentViewModel(
            getOfflinePathForNodeUseCase = getOfflinePathForNodeUseCase,
            getOrCreateDownloadLocationUseCase = getOrCreateDownloadLocationUseCase,
            getFilePreviewDownloadPathUseCase = getFilePreviewDownloadPathUseCase,
            clearActiveTransfersIfFinishedUseCase = clearActiveTransfersIfFinishedUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            totalFileSizeOfNodesUseCase = totalFileSizeOfNodesUseCase,
            fileSizeStringMapper = fileSizeStringMapper,
            isAskBeforeLargeDownloadsSettingUseCase = isAskBeforeLargeDownloadsSettingUseCase,
            setAskBeforeLargeDownloadsSettingUseCase = setAskBeforeLargeDownloadsSettingUseCase,
            monitorOngoingActiveTransfersUseCase = monitorOngoingActiveTransfersUseCase,
            getCurrentDownloadSpeedUseCase = getCurrentDownloadSpeedUseCase,
            shouldAskDownloadDestinationUseCase = shouldAskDownloadDestinationUseCase,
            shouldPromptToSaveDestinationUseCase = shouldPromptToSaveDestinationUseCase,
            saveDoNotPromptToSaveDestinationUseCase = saveDoNotPromptToSaveDestinationUseCase,
            setAskForDownloadLocationUseCase = setAskForDownloadLocationUseCase,
            setDownloadLocationUseCase = setDownloadLocationUseCase,
            sendChatAttachmentsUseCase = sendChatAttachmentsUseCase,
            shouldAskForResumeTransfersUseCase = shouldAskForResumeTransfersUseCase,
            setAskedResumeTransfersUseCase = setAskedResumeTransfersUseCase,
            pauseTransfersQueueUseCase = pauseTransfersQueueUseCase,
            saveOfflineNodesToDevice = saveOfflineNodesToDevice,
            saveUriToDeviceUseCase = saveUriToDeviceUseCase,
            getCurrentUploadSpeedUseCase = getCurrentUploadSpeedUseCase,
            cancelCancelTokenUseCase = cancelCancelTokenUseCase,
            monitorRequestFilesPermissionDeniedUseCase = monitorRequestFilesPermissionDeniedUseCase,
            setRequestFilesPermissionDeniedUseCase = setRequestFilesPermissionDeniedUseCase,
            startDownloadsWorkerAndWaitUntilIsStartedUseCase = startDownloadsWorkerAndWaitUntilIsStartedUseCase,
            startUploadsWorkerAndWaitUntilIsStartedUseCase = startUploadsWorkerAndWaitUntilIsStartedUseCase,
            deleteAllPendingTransfersUseCase = deleteAllPendingTransfersUseCase,
            monitorPendingTransfersUntilResolvedUseCase = monitorPendingTransfersUntilResolvedUseCase,
            insertPendingDownloadsForNodesUseCase = insertPendingDownloadsForNodesUseCase,
            insertPendingUploadsForFilesUseCase = insertPendingUploadsForFilesUseCase,
            monitorStorageOverQuotaUseCase = monitorStorageOverQuotaUseCase,
            invalidateCancelTokenUseCase = invalidateCancelTokenUseCase,
            getCurrentTimeInMillisUseCase = getCurrentTimeInMillisUseCase,
            areTransfersPausedUseCase = areTransfersPausedUseCase,
            getFileNameFromStringUriUseCase = getFileNameFromStringUriUseCase,
            cancelTransferByTagUseCase = cancelTransferByTagUseCase,
            deleteCacheFilesUseCase = deleteCacheFilesUseCase,
            getTransferByTagUseCase = getTransferByTagUseCase,
            monitorTransferTagToCancelUseCase = monitorTransferTagToCancelUseCase,
            broadcastTransferTagToCancelUseCase = broadcastTransferTagToCancelUseCase,
            deleteCompletedTransfersByIdUseCase = deleteCompletedTransfersByIdUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            ratingHandler = ratingHandlerImpl,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getOfflinePathForNodeUseCase,
            getOrCreateDownloadLocationUseCase,
            getFilePreviewDownloadPathUseCase,
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
            setAskForDownloadLocationUseCase,
            setDownloadLocationUseCase,
            sendChatAttachmentsUseCase,
            shouldAskForResumeTransfersUseCase,
            setAskedResumeTransfersUseCase,
            pauseTransfersQueueUseCase,
            node,
            parentNode,
            saveOfflineNodesToDevice,
            saveUriToDeviceUseCase,
            getCurrentUploadSpeedUseCase,
            cancelCancelTokenUseCase,
            setRequestFilesPermissionDeniedUseCase,
            startDownloadsWorkerAndWaitUntilIsStartedUseCase,
            deleteAllPendingTransfersUseCase,
            monitorPendingTransfersUntilResolvedUseCase,
            insertPendingDownloadsForNodesUseCase,
            invalidateCancelTokenUseCase,
            insertPendingUploadsForFilesUseCase,
            startUploadsWorkerAndWaitUntilIsStartedUseCase,
            getCurrentTimeInMillisUseCase,
            areTransfersPausedUseCase,
            getFileNameFromStringUriUseCase,
            cancelTransferByTagUseCase,
            deleteCacheFilesUseCase,
            getTransferByTagUseCase,
            broadcastTransferTagToCancelUseCase,
            deleteCompletedTransfersByIdUseCase,
            monitorStorageStateEventUseCase
        )
        initialStub()
    }

    private fun initialStub() = runTest {
        whenever(monitorOngoingActiveTransfersUseCase(any())).thenReturn(emptyFlow())
        whenever(monitorRequestFilesPermissionDeniedUseCase()).thenReturn(emptyFlow())
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(emptyFlow())
        val storageStateEvent = mock<StorageStateEvent> {
            on { storageState } doReturn StorageState.Unknown
        }
        whenever(monitorStorageStateEventUseCase()).thenReturn(MutableStateFlow(storageStateEvent))
    }

    @ParameterizedTest
    @MethodSource("provideStartEvents")
    fun `test that clearActiveTransfersIfFinishedUseCase is invoked when startTransfer is invoked`(
        startEvent: TransferTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.startTransfer(startEvent)
        verify(clearActiveTransfersIfFinishedUseCase).invoke()
    }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that start download worker is started  when download is started`(
        startEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        if (startEvent is TransferTriggerEvent.StartDownloadForOffline) {
            whenever(getOfflinePathForNodeUseCase(any())).thenReturn(DESTINATION)
        } else if (startEvent is TransferTriggerEvent.StartDownloadForPreview) {
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn(DESTINATION)
        }
        underTest.startTransfer(startEvent)
        verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase).invoke()
    }

    @Test
    fun `test that preview file is deleted before starting to download it`() = runTest {
        commonStub()
        val previewCachePath = "/cache/preview"
        whenever(getFilePreviewDownloadPathUseCase()) doReturn previewCachePath
        val startEvent = TransferTriggerEvent.StartDownloadForPreview(node, false)
        underTest.startTransfer(startEvent)
        verify(deleteCacheFilesUseCase).invoke(listOf(UriPath(previewCachePath + node.name)))
    }

    @ParameterizedTest
    @MethodSource("provideStartChatUploadEvents")
    fun `test that send chat attachments use case is invoked with correct parameters when chat upload is started`(
        startEvent: TransferTriggerEvent.StartChatUpload,
    ) = runTest {
        commonStub()
        underTest.startTransfer(startEvent)
        verify(sendChatAttachmentsUseCase).invoke(
            listOf(uploadUri).associateWith { null },
            false,
            CHAT_ID,
        )
    }

    @Test
    fun `test that no connection event is emitted when monitorConnectivityUseCase is false and start a download`() =
        runTest {
            commonStub()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            underTest.startTransfer(
                TransferTriggerEvent.StartDownloadNode(
                    nodes = nodes,
                    withStartMessage = false,
                )
            )
            assertCurrentEventIsEqualTo(StartTransferEvent.NotConnected)
        }

    @Test
    fun `test that cancel event is emitted when start download nodes is invoked with empty list`() =
        runTest {
            commonStub()
            underTest.startTransfer(
                TransferTriggerEvent.StartDownloadNode(
                    nodes = listOf(),
                    withStartMessage = false,
                )
            )
            assertCurrentEventIsEqualTo(StartTransferEvent.Message.TransferCancelled)
        }

    @Test
    fun `test that cancel event is emitted when start download nodes is invoked with null node`() =
        runTest {
            commonStub()
            underTest.startTransfer(
                TransferTriggerEvent.StartDownloadForOffline(
                    node = null,
                    withStartMessage = false,
                )
            )
            assertCurrentEventIsEqualTo(StartTransferEvent.Message.TransferCancelled)
        }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that ConfirmLargeDownload is emitted when a large download is started`(
        startEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        whenever(isAskBeforeLargeDownloadsSettingUseCase()).thenReturn(true)
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(TransfersConstants.CONFIRM_SIZE_MIN_BYTES + 1)
        val size = "x MB"
        whenever(fileSizeStringMapper(any())).thenReturn(size)
        underTest.startTransfer(startEvent)
        assertThat(underTest.uiState.value.confirmLargeDownload).isEqualTo(
            ConfirmLargeDownloadInfo(size, startEvent)
        )
    }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that setAskBeforeLargeDownloadsSettingUseCase is invoked when specified in largeDownloadAnswered`(
        startEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.largeDownloadAnswered(startEvent, true)
        verify(setAskBeforeLargeDownloadsSettingUseCase).invoke(false)
    }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that setAskBeforeLargeDownloadsSettingUseCase is not invoked when not specified in largeDownloadAnswered`(
        startEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        underTest.largeDownloadAnswered(startEvent, false)
        verifyNoInteractions(setAskBeforeLargeDownloadsSettingUseCase)
    }

    @Test
    fun `test that AskDestination event is triggered when a download starts and shouldAskDownloadDestinationUseCase is true`() =
        runTest {
            commonStub()
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            val event = TransferTriggerEvent.StartDownloadNode(
                nodes = nodes,
                withStartMessage = false,
            )
            underTest.startDownloadWithoutConfirmation(event)
            assertThat(underTest.uiState.value.askDestinationForDownload).isEqualTo(
                event
            )
        }

    @Test
    fun `test that promptSaveDestination state is updated when startDownloadWithDestination is invoked and shouldPromptToSaveDestinationUseCase is true`() =
        runTest {
            commonStub()
            val uriString = "content:/destination"
            val destinationName = "destinationName"
            val destinationUri = mock<Uri> {
                on { toString() } doReturn uriString
            }
            val startDownloadNode = TransferTriggerEvent.StartDownloadNode(
                nodes = nodes,
                withStartMessage = false,
            )
            val expected = SaveDestinationInfo(
                destination = uriString,
                destinationName = destinationName
            )

            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            whenever(shouldPromptToSaveDestinationUseCase()).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(uriString)).thenReturn(destinationName)

            underTest.startDownloadWithoutConfirmation(startDownloadNode)
            underTest.startDownloadWithDestination(destinationUri)

            assertThat(underTest.uiState.value.promptSaveDestination)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((underTest.uiState.value.promptSaveDestination as StateEventWithContentTriggered).content)
                .isEqualTo(expected)

        }

    @Test
    fun `test that consumePromptSaveDestination updates state`() =
        runTest {
            commonStub()
            val uriString = "content:/destination"
            val destinationName = "destinationName"
            val destinationUri = mock<Uri> {
                on { toString() } doReturn uriString
            }
            val startDownloadNode = TransferTriggerEvent.StartDownloadNode(
                nodes = nodes,
                withStartMessage = false,
            )

            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            whenever(shouldPromptToSaveDestinationUseCase()).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(uriString)).thenReturn(destinationName)

            underTest.startDownloadWithoutConfirmation(startDownloadNode)
            underTest.startDownloadWithDestination(destinationUri)
            underTest.consumePromptSaveDestination()

            assertThat(underTest.uiState.value.promptSaveDestination)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }


    @Test
    fun `test that setStorageDownloadAskAlwaysUseCase is set to true when alwaysAskForDestination is invoked`() =
        runTest {
            underTest.alwaysAskForDestination()
            verify(setAskForDownloadLocationUseCase).invoke(true)
        }

    @Test
    fun `test that saveDoNotPromptToSaveDestinationUseCase is invoked when alwaysAskForDestination is invoked`() =
        runTest {
            underTest.alwaysAskForDestination()
            verify(saveDoNotPromptToSaveDestinationUseCase).invoke()
        }

    @Test
    fun `test that saveDoNotPromptToSaveDestinationUseCase is set to false when saveDestination is invoked`() =
        runTest {
            underTest.saveDestination("destination")
            verify(saveDoNotPromptToSaveDestinationUseCase).invoke()
        }

    @Test
    fun `test that setDownloadLocationUseCase is invoked when saveDestination is invoked`() =
        runTest {
            val destination = "destination"
            underTest.saveDestination(destination)
            verify(setDownloadLocationUseCase).invoke(destination)
        }

    @Test
    fun `test that setStorageDownloadAskAlwaysUseCase is set to false when saveDestination is invoked`() =
        runTest {
            underTest.saveDestination("destination")
            verify(setAskForDownloadLocationUseCase).invoke(false)
        }

    @Test
    fun `test that paused transfers event is emitted when shouldAskForResumeTransfersUseCase is true and start a chat upload`() =
        runTest {
            commonStub()
            whenever(shouldAskForResumeTransfersUseCase()).thenReturn(true)
            val triggerEvent =
                TransferTriggerEvent.StartChatUpload.Files(CHAT_ID, listOf(uploadUri))
            underTest.startTransfer(triggerEvent)
            assertCurrentEventIsEqualTo(StartTransferEvent.PausedTransfers(triggerEvent))
        }

    @Test
    fun `test that paused transfers event is emitted when transfers are paused and start a preview download`() =
        runTest {
            commonStub()
            stubMonitorPendingTransfers(
                TransferType.DOWNLOAD,
                flow { awaitCancellation() } //no events when it's paused
            )
            whenever(areTransfersPausedUseCase()).thenReturn(true)
            whenever(getFilePreviewDownloadPathUseCase()).thenReturn("/path")
            val triggerEvent =
                TransferTriggerEvent.StartDownloadForPreview(mock<ChatDefaultFile>(), false)
            underTest.startTransfer(triggerEvent)
            assertCurrentEventIsEqualTo(StartTransferEvent.PausedTransfers(triggerEvent))
        }

    @Test
    fun `test that no connection event is emitted when monitorConnectivityUseCase is false and start an upload`() =
        runTest {
            commonStub()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)

            underTest.startTransfer(startUploadFilesEvent)

            assertCurrentEventIsEqualTo(StartTransferEvent.NotConnected)
        }

    @Test
    fun `test that cancel event is emitted when start upload files is invoked with empty map`() =
        runTest {
            commonStub()

            underTest.startTransfer(
                TransferTriggerEvent.StartUpload.Files(mapOf(), parentId)
            )

            assertCurrentEventIsEqualTo(StartTransferEvent.Message.TransferCancelled)
        }

    @Test
    fun `test that job in progress is set to ProcessingFiles when start upload use case starts`() =
        runTest {
            commonStub()
            stubMonitorPendingTransfers(TransferType.GENERAL_UPLOAD, flow {
                emit(mockScanningPendingTransfers())
                awaitCancellation()
            })

            underTest.startTransfer(startUploadFilesEvent)

            assertThat(underTest.uiState.value.jobInProgressState)
                .isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)
        }

    @ParameterizedTest
    @MethodSource("provideStartUploadEvents")
    fun `test that start upload use case is invoked with correct parameters when upload is started`(
        startEvent: TransferTriggerEvent.StartUpload,
    ) = runTest {
        commonStub()

        underTest.startTransfer(startEvent)

        verify(insertPendingUploadsForFilesUseCase).invoke(
            mapOf(DESTINATION to null),
            parentId,
            startEvent.isHighPriority,
        )
    }

    @Test
    fun `test that FinishUploadProcessing event is emitted if start upload use case emits an event with scanning finished true`() =
        runTest {
            commonStub()

            underTest.startTransfer(startUploadFilesEvent)

            assertThat(underTest.uiState.value.jobInProgressState).isNull()
            assertCurrentEventIsEqualTo(
                StartTransferEvent.FinishUploadProcessing(1, startUploadFilesEvent)
            )
        }

    @Test
    fun `test that FinishUploadProcessing event is emitted if start upload use case finishes correctly`() =
        runTest {
            commonStub()

            underTest.startTransfer(startUploadFilesEvent)

            assertCurrentEventIsEqualTo(
                StartTransferEvent.FinishUploadProcessing(1, startUploadFilesEvent)
            )
        }

    @Test
    fun `test that start download with destination trigger save offline nodes to device when event is CopyOfflineNode`() =
        runTest {
            commonStub()
            val uri = mock<Uri> {
                on { toString() } doReturn DESTINATION
            }
            val nodeId = NodeId(1)
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(true)
            whenever(saveOfflineNodesToDevice(listOf(nodeId), UriPath(DESTINATION))).thenReturn(1)
            underTest.startDownloadWithoutConfirmation(
                TransferTriggerEvent.CopyOfflineNode(listOf(nodeId))
            )
            underTest.startDownloadWithDestination(
                uri
            )
            assertCurrentEventIsEqualTo(
                StartTransferEvent.FinishCopyOffline(1)
            )
            verifyNoInteractions(startDownloadsWorkerAndWaitUntilIsStartedUseCase)
        }

    @Test
    fun `test that start download without confirmation trigger save offline nodes to device when event is CopyOfflineNode`() =
        runTest {
            commonStub()
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(false)
            whenever(getOrCreateDownloadLocationUseCase()).thenReturn(DESTINATION)
            val nodeId = NodeId(1)
            underTest.startDownloadWithoutConfirmation(
                TransferTriggerEvent.CopyOfflineNode(listOf(nodeId)),
            )
            verify(saveOfflineNodesToDevice).invoke(listOf(nodeId), UriPath(DESTINATION))
            verifyNoInteractions(startDownloadsWorkerAndWaitUntilIsStartedUseCase)
        }

    @Test
    fun `test that start download without confirmation trigger save uri to device when event is CopyUri`() =
        runTest {
            commonStub()
            val sourceUri = "Source"
            val uri = mock<Uri> {
                on { toString() } doReturn sourceUri
            }
            whenever(shouldAskDownloadDestinationUseCase()).thenReturn(false)
            whenever(getOrCreateDownloadLocationUseCase()).thenReturn(DESTINATION)
            underTest.startDownloadWithoutConfirmation(
                TransferTriggerEvent.CopyUri(
                    "name",
                    uri
                )
            )
            verify(saveUriToDeviceUseCase).invoke("name", UriPath(sourceUri), UriPath(DESTINATION))
            verifyNoInteractions(startDownloadsWorkerAndWaitUntilIsStartedUseCase)
        }

    @Test
    fun `test that cancel current transfers job invokes cancel cancelToken use case`() = runTest {
        commonStub()
        underTest.cancelCurrentTransfersJob()

        verify(cancelCancelTokenUseCase).invoke()
    }

    @Test
    fun `test that cancel current transfers job sets state to cancelling when previous state was scanning`() =
        runTest {
            commonStub()
            stubMonitorPendingTransfers(TransferType.GENERAL_UPLOAD, flow {
                emit(mockScanningPendingTransfers())
                awaitCancellation()
            })
            underTest.uiState.test {
                awaitItem() //don't care about initial value
                underTest.startTransfer(startUploadFilesEvent)
                assertThat(awaitItem().jobInProgressState).isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)

                underTest.cancelCurrentTransfersJob()
                assertThat(awaitItem().jobInProgressState).isEqualTo(StartTransferJobInProgress.CancellingTransfers)
            }
        }

    @Test
    fun `test that cancel current transfers job does not set state to cancelling when scanning has already finished`() =
        runTest {
            commonStub()
            val pendingTransfersFlow = MutableSharedFlow<List<PendingTransfer>>()
            stubMonitorPendingTransfers(
                TransferType.GENERAL_UPLOAD,
                pendingTransfersFlow.takeWhile { it.isNotEmpty() }
            )
            underTest.uiState.test {
                awaitItem() //don't care about initial value
                underTest.startTransfer(startUploadFilesEvent)
                pendingTransfersFlow.emit(mockScanningPendingTransfers())
                assertThat(awaitItem().jobInProgressState).isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)
                pendingTransfersFlow.emit(emptyList())
                assertThat(awaitItem().jobInProgressState).isEqualTo(null)
                underTest.cancelCurrentTransfersJob()
                expectNoEvents()
            }
        }

    @Test
    fun `test that cancel current transfers job does not set state to cancelling when previous state was not scanning`() =
        runTest {
            commonStub()
            underTest.uiState.test {
                underTest.cancelCurrentTransfersJob()
                assertThat(awaitItem().jobInProgressState).isNotInstanceOf(
                    StartTransferJobInProgress.CancellingTransfers::class.java
                )
            }
        }

    @Test
    fun `test that cancel current transfers job does not set state to cancelling when the cancellation fails`() =
        runTest {
            commonStub()
            stubMonitorPendingTransfers(TransferType.GENERAL_UPLOAD, flow {
                emit(mockScanningPendingTransfers())
                awaitCancellation()
            })
            whenever(cancelCancelTokenUseCase()).thenThrow(RuntimeException())
            underTest.uiState.test {

                awaitItem() //don't care about initial value
                underTest.startTransfer(startUploadFilesEvent)
                assertThat(awaitItem().jobInProgressState).isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)

                underTest.cancelCurrentTransfersJob()
                expectNoEvents()
            }
        }

    private fun mockScanningPendingTransfers(): List<PendingTransfer> = listOf(mock {
        on { scanningFoldersData } doReturn PendingTransfer.ScanningFoldersData(
            stage = TransferStage.STAGE_SCANNING
        )
    })

    @Test
    fun `test that monitorRequestFilesPermissionDeniedUseCase updates state`() =
        runTest {
            whenever(monitorRequestFilesPermissionDeniedUseCase())
                .thenReturn(flowOf(true))

            initTest()

            underTest.uiState.map { it.requestFilesPermissionDenied }.test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `test that setRequestFilesPermissionDenied invokes correct use case`() = runTest {
        whenever(setRequestFilesPermissionDeniedUseCase()).thenReturn(Unit)

        underTest.setRequestFilesPermissionDenied()
        verify(setRequestFilesPermissionDeniedUseCase).invoke()
    }

    @ParameterizedTest(name = " if use case returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that monitorStorageOverQuota updates state`(
        isStorageOverQuota: Boolean,
    ) = runTest {
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(flowOf(isStorageOverQuota))

        initTest()

        underTest.uiState.map { it.isStorageOverQuota }.test {
            assertThat(awaitItem()).isEqualTo(isStorageOverQuota)
        }
    }

    @Test
    fun `test that when storage state is paywall transfers are not started`() = runTest {
        val storageStateEvent = mock<StorageStateEvent> {
            on { storageState } doReturn StorageState.PayWall
        }
        whenever(monitorStorageStateEventUseCase()).thenReturn(MutableStateFlow(storageStateEvent))
        initTest()
        commonStub()
        underTest.startTransfer(
            TransferTriggerEvent.StartDownloadNode(
                nodes = nodes,
                withStartMessage = false,
            )
        )
        assertThat(underTest.uiState.value.oneOffViewEvent).isEqualTo(triggered(StartTransferEvent.PayWall))
    }

    @Nested
    inner class StartDownload {
        @Test
        fun `test that startDownloadsWorkerAndWaitUntilIsStartedUseCase is invoked when a download starts`() =
            runTest {
                commonStub()

                underTest.startTransfer(startDownloadEvent)

                verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase)()
            }

        @Test
        fun `test that ui state is updated with pending transfers from monitorNotResolvedPendingTransfersUseCase when a download starts`() =
            runTest {
                commonStub()
                val expectedList = listOf(
                    TransferStage.STAGE_NONE,
                    TransferStage.STAGE_SCANNING,
                    TransferStage.STAGE_CREATING_TREE,
                )

                val pendingTransfers = expectedList.map { stage ->
                    listOf(mock<PendingTransfer> {
                        on { this.scanningFoldersData } doReturn
                                PendingTransfer.ScanningFoldersData(stage)
                    })
                }

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn flow {
                    pendingTransfers.forEach {
                        emit(it)
                        yield()
                    }
                    awaitCancellation()
                }

                underTest.uiState.test {
                    println(awaitItem())//ignore initial
                    underTest.startTransfer(
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = nodes,
                            withStartMessage = false,
                        )
                    )
                    expectedList.forEach { expected ->
                        val actual =
                            (awaitItem().jobInProgressState as? StartTransferJobInProgress.ScanningTransfers)?.stage
                        println(actual)
                        assertThat(actual).isEqualTo(expected)
                    }
                }

                assertThat(underTest.uiState.value.jobInProgressState)
                    .isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)
            }

        @Test
        fun `test that FinishDownloadProcessing event is emitted when monitorNotResolvedPendingTransfersUseCase finishes`() =
            runTest {
                commonStub()
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                    on { this.startedFiles } doReturn 1
                }
                val triggerEvent = TransferTriggerEvent.StartDownloadNode(
                    nodes = nodes,
                    withStartMessage = false,
                )
                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(triggerEvent)

                assertThat(underTest.uiState.value.jobInProgressState).isNull()
                assertCurrentEventIsEqualTo(
                    StartTransferEvent.FinishDownloadProcessing(null, triggerEvent)
                )
            }

        @Test
        fun `test that deleteAllPendingTransfersUseCase is invoked when monitorNotResolvedPendingTransfersUseCase finishes`() =
            runTest {
                commonStub()
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                }
                val triggerEvent = TransferTriggerEvent.StartDownloadNode(
                    nodes = nodes,
                    withStartMessage = false,
                )

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(triggerEvent)

                verify(deleteAllPendingTransfersUseCase)()
            }
    }

    @Nested
    inner class StartUpload {
        @Test
        fun `test that startUploadsWorkerAndWaitUntilIsStartedUseCase is invoked when an upload starts`() =
            runTest {
                commonStub()

                underTest.startTransfer(startUploadFilesEvent)

                verify(startUploadsWorkerAndWaitUntilIsStartedUseCase)()
            }

        @Test
        fun `test that ui state is updated with pending transfers from monitorNotResolvedPendingTransfersUseCase when an upload starts`() =
            runTest {
                commonStub()
                val expectedList = listOf(
                    TransferStage.STAGE_NONE,
                    TransferStage.STAGE_SCANNING,
                    TransferStage.STAGE_CREATING_TREE,
                )

                val pendingTransfers = expectedList.map { stage ->
                    listOf(mock<PendingTransfer> {
                        on { this.scanningFoldersData } doReturn
                                PendingTransfer.ScanningFoldersData(stage)
                    })
                }

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)) doReturn flow {
                    pendingTransfers.forEach {
                        emit(it)
                        yield()
                    }
                    awaitCancellation()
                }

                underTest.uiState.test {
                    println(awaitItem())//ignore initial
                    underTest.startTransfer(startUploadEvent)
                    expectedList.forEach { expected ->
                        val actual =
                            (awaitItem().jobInProgressState as? StartTransferJobInProgress.ScanningTransfers)?.stage
                        println(actual)
                        assertThat(actual).isEqualTo(expected)
                    }
                }

                assertThat(underTest.uiState.value.jobInProgressState)
                    .isInstanceOf(StartTransferJobInProgress.ScanningTransfers::class.java)
            }

        @Test
        fun `test that FinishDownloadProcessing event is emitted when monitorNotResolvedPendingTransfersUseCase finishes`() =
            runTest {
                commonStub()
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                    on { this.startedFiles } doReturn 1
                }
                val triggerEvent = startUploadEvent
                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(triggerEvent)

                assertThat(underTest.uiState.value.jobInProgressState).isNull()
                assertCurrentEventIsEqualTo(
                    StartTransferEvent.FinishUploadProcessing(1, triggerEvent)
                )
            }

        @Test
        fun `test that deleteAllPendingTransfersUseCase is invoked when monitorNotResolvedPendingTransfersUseCase finishes`() =
            runTest {
                commonStub()
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                }

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(startUploadEvent)

                verify(deleteAllPendingTransfersUseCase)()
            }
    }

    @Nested
    inner class RetryTransfers {
        private val firstId = 1
        private val secondId = 2
        private val retriedTransferIds = listOf(firstId, secondId)
        private val retryDownloadEvent = TransferTriggerEvent.RetryDownloadNode(
            node = nodes.first(),
            downloadLocation = "downloadLocation",
        )
        private val retryUploadsEvent = TransferTriggerEvent.RetryTransfers(
            mapOf(firstId to startUploadFilesEvent, secondId to startUploadFilesEvent)
        )
        private val retryDownloadsEvent = TransferTriggerEvent.RetryTransfers(
            mapOf(firstId to retryDownloadEvent, secondId to startOfflineDownloadEvent)
        )
        private val retryUploadAndDownloadEvent = TransferTriggerEvent.RetryTransfers(
            mapOf(firstId to retryDownloadEvent, secondId to startUploadFilesEvent)
        )

        @Test
        fun `test that startUploadsWorkerAndWaitUntilIsStartedUseCase is invoked when different uploads start`() =
            runTest {
                commonStub()

                underTest.startTransfer(retryUploadsEvent)

                verify(startUploadsWorkerAndWaitUntilIsStartedUseCase)()
                verifyNoInteractions(startDownloadsWorkerAndWaitUntilIsStartedUseCase)
            }

        @Test
        fun `test that startDownloadsWorkerAndWaitUntilIsStartedUseCase is invoked when different downloads start`() =
            runTest {
                commonStub()

                underTest.startTransfer(retryDownloadsEvent)

                verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase)()
                verifyNoInteractions(startUploadsWorkerAndWaitUntilIsStartedUseCase)
            }

        @Test
        fun `test that startDownloadsWorkerAndWaitUntilIsStartedUseCase and startUploadsWorkerAndWaitUntilIsStartedUseCase are invoked when different transfers start`() =
            runTest {
                commonStub()

                underTest.startTransfer(retryUploadAndDownloadEvent)

                verify(startUploadsWorkerAndWaitUntilIsStartedUseCase)()
                verify(startDownloadsWorkerAndWaitUntilIsStartedUseCase)()
            }

        @Test
        fun `test that deleteAllPendingTransfersUseCase and invalidateCancelTokenUseCase are invoked when monitorPendingTransfersUntilResolvedUseCase finishes for uploads`() =
            runTest {
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                }

                commonStub()

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(retryUploadsEvent)

                verify(invalidateCancelTokenUseCase)()
                verify(deleteAllPendingTransfersUseCase)()
            }

        @Test
        fun `test that invalidateCancelTokenUseCase and invalidateCancelTokenUseCase are invoked when monitorPendingTransfersUntilResolvedUseCase finishes for downloads`() =
            runTest {
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                }

                commonStub()

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(retryDownloadsEvent)

                verify(invalidateCancelTokenUseCase)()
                verify(deleteAllPendingTransfersUseCase)()
            }

        @Test
        fun `test that deleteAllPendingTransfersUseCase and invalidateCancelTokenUseCase are invoked when monitorPendingTransfersUntilResolvedUseCase finishes for uploads and downloads`() =
            runTest {
                val pendingTransfer = mock<PendingTransfer> {
                    val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                        TransferStage.STAGE_TRANSFERRING_FILES,
                    )
                    on { this.scanningFoldersData } doReturn scanningFoldersData
                }

                commonStub()

                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))
                whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn
                        flowOf(listOf(pendingTransfer))

                underTest.startTransfer(retryUploadAndDownloadEvent)

                verify(invalidateCancelTokenUseCase)()
                verify(deleteAllPendingTransfersUseCase)()
            }

        @Test
        fun `test that insertPendingDownloadsForNodesUseCase and deleteCompletedTransfersByIdUseCase are invoked with correct parameters for downloads`() =
            runTest {
                commonStub()

                whenever(getOfflinePathForNodeUseCase(any())) doReturn DESTINATION
                whenever(shouldPromptToSaveDestinationUseCase()) doReturn true

                underTest.startTransfer(retryDownloadsEvent)

                verify(insertPendingDownloadsForNodesUseCase)(
                    retryDownloadEvent.nodes,
                    UriPath(retryDownloadEvent.downloadLocation),
                    retryDownloadEvent.isHighPriority,
                    retryDownloadEvent.appData,
                )

                verify(insertPendingDownloadsForNodesUseCase)(
                    startOfflineDownloadEvent.nodes,
                    UriPath(DESTINATION),
                    startOfflineDownloadEvent.isHighPriority,
                    startOfflineDownloadEvent.appData,
                )

                verify(deleteCompletedTransfersByIdUseCase)(retriedTransferIds)
            }

        @Test
        fun `test that insertPendingUploadsForFilesUseCase and deleteCompletedTransfersByIdUseCase are invoked with correct parameters for uploads`() =
            runTest {
                commonStub()

                underTest.startTransfer(retryUploadsEvent)

                verify(insertPendingUploadsForFilesUseCase, times(2)).invoke(
                    startUploadFilesEvent.pathsAndNames,
                    startUploadFilesEvent.destinationId,
                    startUploadFilesEvent.isHighPriority,
                )

                verify(deleteCompletedTransfersByIdUseCase)(retriedTransferIds)
            }

        @Test
        fun `test that insertPendingDownloadsForNodesUseCase, insertPendingUploadsForFilesUseCase and deleteCompletedTransfersByIdUseCase are invoked with correct parameters for uploads and downloads`() =
            runTest {
                commonStub()

                whenever(getOrCreateDownloadLocationUseCase()) doReturn DESTINATION

                underTest.startTransfer(retryUploadAndDownloadEvent)

                verify(insertPendingDownloadsForNodesUseCase)(
                    startDownloadEvent.nodes,
                    UriPath(DESTINATION),
                    startDownloadEvent.isHighPriority,
                    startDownloadEvent.appData,
                )

                verify(insertPendingUploadsForFilesUseCase)(
                    startUploadFilesEvent.pathsAndNames,
                    startUploadFilesEvent.destinationId,
                    startUploadFilesEvent.isHighPriority,
                )

                verify(deleteCompletedTransfersByIdUseCase)(retriedTransferIds)
            }

        @Test
        fun `test that state is updated with NotSufficientSpace event when insufficient space for downloads`() =
            runTest {
                commonStub()

                whenever(getOfflinePathForNodeUseCase(any())) doReturn DESTINATION
                whenever(shouldPromptToSaveDestinationUseCase()) doReturn true
                whenever(
                    insertPendingDownloadsForNodesUseCase(
                        retryDownloadEvent.nodes,
                        UriPath(retryDownloadEvent.downloadLocation),
                        retryDownloadEvent.isHighPriority,
                        retryDownloadEvent.appData,
                    )
                ).thenAnswer { throw NotEnoughStorageException() }

                underTest.startTransfer(retryDownloadsEvent)

                underTest.uiState.map { it.oneOffViewEvent }.test {
                    assertThat(awaitItem())
                        .isEqualTo(triggered(StartTransferEvent.Message.NotSufficientSpace))
                }
            }

        @Test
        fun `test that state is not updated with NotSufficientSpace event when sufficient space for downloads`() =
            runTest {
                commonStub()

                whenever(getOfflinePathForNodeUseCase(any())) doReturn DESTINATION
                whenever(shouldPromptToSaveDestinationUseCase()) doReturn true
                whenever(
                    insertPendingDownloadsForNodesUseCase(
                        retryDownloadEvent.nodes,
                        UriPath(retryDownloadEvent.downloadLocation),
                        retryDownloadEvent.isHighPriority,
                        retryDownloadEvent.appData,
                    )
                ) doReturn Unit

                underTest.startTransfer(retryDownloadsEvent)

                underTest.uiState.map { it.oneOffViewEvent }.test {
                    assertThat(awaitItem())
                        .isNotEqualTo(triggered(StartTransferEvent.Message.NotSufficientSpace))
                }
            }
    }

    @Test
    fun `test that invalidateCancelTokenUseCase is invoked when monitorNotResolvedPendingTransfersUseCase finishes`() =
        runTest {
            commonStub()
            val pendingTransfer = mock<PendingTransfer> {
                val scanningFoldersData = PendingTransfer.ScanningFoldersData(
                    TransferStage.STAGE_TRANSFERRING_FILES,
                )
                on { this.scanningFoldersData } doReturn scanningFoldersData
            }
            val triggerEvent = TransferTriggerEvent.StartDownloadNode(
                nodes = nodes,
                withStartMessage = false,
            )
            whenever(monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)) doReturn
                    flowOf(listOf(pendingTransfer))

            underTest.startTransfer(triggerEvent)

            verify(invalidateCancelTokenUseCase)()
        }

    @ParameterizedTest
    @MethodSource("provideStartDownloadEvents")
    fun `test that insertPendingDownloadsForNodesUseCase is invoked with correct parameters when a download starts`(
        startDownloadEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) = runTest {
        commonStub()
        whenever(getOfflinePathForNodeUseCase(any())) doReturn DESTINATION
        whenever(getFilePreviewDownloadPathUseCase()) doReturn DESTINATION

        underTest.startTransfer(startDownloadEvent)

        verify(insertPendingDownloadsForNodesUseCase)(
            startDownloadEvent.nodes,
            UriPath(DESTINATION),
            startDownloadEvent.isHighPriority,
            startDownloadEvent.appData,
        )
    }

    @Nested
    inner class TriggerEventWithoutPermission {

        @BeforeEach
        fun cleanUp() {
            underTest.consumeRequestPermission()
        }

        @Test
        fun `test that transferEventWaitingForPermissionRequest sets triggerEventWithoutPermission`() =
            runTest {
                val expected = mock<TransferTriggerEvent.StartUpload.Files>()
                underTest.uiState.test {
                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                    underTest.transferEventWaitingForPermissionRequest(expected)

                    val actual = awaitItem().triggerEventWithoutPermission
                    assertThat(actual).isEqualTo(expected)
                }
            }

        @Test
        fun `test that consumeRequestPermission clears triggerEventWithoutPermission`() =
            runTest {
                val expected = mock<TransferTriggerEvent.StartUpload.Files>()
                underTest.uiState.test {
                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                    underTest.transferEventWaitingForPermissionRequest(expected)
                    assertThat(awaitItem().triggerEventWithoutPermission).isNotNull()

                    underTest.consumeRequestPermission()

                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                }
            }


        @Test
        fun `test that startTransferAfterPermissionRequest starts transfer flow`() = runTest {
            commonStub()
            val event = mock<TransferTriggerEvent.StartUpload.Files>()
            underTest.transferEventWaitingForPermissionRequest(event)

            underTest.startTransferAfterPermissionRequest()

            verify(clearActiveTransfersIfFinishedUseCase).invoke()
        }

        @Test
        fun `test that startTransferAfterPermissionRequest clears triggerEventWithoutPermission`() =
            runTest {
                commonStub()
                val expected = mock<TransferTriggerEvent.StartUpload.Files>()
                underTest.uiState.test {
                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                    underTest.transferEventWaitingForPermissionRequest(expected)
                    assertThat(awaitItem().triggerEventWithoutPermission).isNotNull()

                    underTest.startTransferAfterPermissionRequest()
                    awaitItem() //new ui state for the started transfer
                    assertThat(awaitItem().triggerEventWithoutPermission).isNull()
                }
            }
    }

    @Test
    fun `test that monitorTransferTagToCancelUseCase updates state correctly`() = runTest {
        val transferTag = 123

        whenever(monitorTransferTagToCancelUseCase()) doReturn flowOf(transferTag)

        initTest()

        underTest.uiState.test {
            assertThat(awaitItem().transferTagToCancel).isEqualTo(transferTag)
        }
    }

    @Test
    fun `test that cancelTransferConfirmed invokes correctly`() =
        runTest {
            val transferTagToCancel = 1

            whenever(monitorTransferTagToCancelUseCase()) doReturn flowOf(transferTagToCancel)
            whenever(cancelTransferByTagUseCase(transferTagToCancel)).thenReturn(Unit)
            whenever(broadcastTransferTagToCancelUseCase(null)).thenReturn(Unit)

            initTest()

            underTest.cancelTransferConfirmed()

            verify(cancelTransferByTagUseCase).invoke(transferTagToCancel)
            verify(broadcastTransferTagToCancelUseCase).invoke(null)
        }

    @ParameterizedTest(name = " when use case finishes with success: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that cancelTransferConfirmed updates the state correctly`(success: Boolean) =
        runTest {
            val transferTagToCancel = 1

            whenever(cancelTransferByTagUseCase(transferTagToCancel)).also {
                if (success) {
                    it.thenReturn(Unit)
                } else {
                    it.thenThrow(RuntimeException())
                }
            }

            with(underTest) {
                cancelTransferConfirmed()

                uiState.test {
                    val result = awaitItem().cancelTransferFailure
                    assertThat(result).isEqualTo(
                        if (success) StateEvent.Consumed
                        else StateEvent.Triggered
                    )
                }
            }
        }

    @Test
    fun `test that onConsumeCancelTransferResult updates the state correctly`() =
        runTest {
            with(underTest) {
                onConsumeCancelTransferFailure()

                uiState.test {
                    val result = awaitItem().cancelTransferFailure
                    assertThat(result).isEqualTo(StateEvent.Consumed)
                }
            }
        }

    @Test
    fun `test that cancelTransferCancelled invokes correctly`() = runTest {
        underTest.cancelTransferCancelled()

        verify(broadcastTransferTagToCancelUseCase).invoke(null)
    }

    @Test
    fun `test that previewFile updates state correctly`() = runTest {
        val transferTagToCancel = 1
        val file = mock<File>()

        whenever(monitorTransferTagToCancelUseCase()) doReturn flowOf(transferTagToCancel)

        initTest()

        underTest.previewFile(file)

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.previewFileToOpen).isEqualTo(file)
            assertThat(actual.jobInProgressState).isNull()
        }
    }

    private fun provideStartDownloadEvents() = listOf(
        TransferTriggerEvent.StartDownloadNode(
            nodes = nodes,
            withStartMessage = false,
        ),
        TransferTriggerEvent.StartDownloadForOffline(
            node = node,
            withStartMessage = false,
        ),
        TransferTriggerEvent.StartDownloadForPreview(node, false),

        )

    private fun provideStartChatUploadEvents() = listOf(
        TransferTriggerEvent.StartChatUpload.Files(CHAT_ID, listOf(uploadUri)),
    )

    private fun provideStartUploadEvents() = listOf(
        startUploadFilesEvent,
        startUploadTextFileEvent,
    )

    private fun provideStartEvents() = provideStartDownloadEvents() +
            provideStartChatUploadEvents() + provideStartUploadEvents()

    private fun assertCurrentEventIsEqualTo(event: StartTransferEvent) {
        assertThat(underTest.uiState.value.oneOffViewEvent)
            .isInstanceOf(StateEventWithContentTriggered::class.java)
        assertThat((underTest.uiState.value.oneOffViewEvent as StateEventWithContentTriggered).content)
            .isEqualTo(event)
    }

    private suspend fun commonStub() {
        whenever(isAskBeforeLargeDownloadsSettingUseCase()).thenReturn(false)
        whenever(node.id).thenReturn(nodeId)
        whenever(node.name).thenReturn(NODE_NAME)
        whenever(node.parentId).thenReturn(parentId)
        whenever(parentNode.id).thenReturn(parentId)

        whenever(getOrCreateDownloadLocationUseCase()).thenReturn(DESTINATION)

        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(1)
        whenever(shouldAskDownloadDestinationUseCase()).thenReturn(false)
        whenever(monitorRequestFilesPermissionDeniedUseCase()).thenReturn(emptyFlow())
        whenever(monitorStorageOverQuotaUseCase()).thenReturn(emptyFlow())
    }

    private fun stubMonitorPendingTransfers(
        transferType: TransferType,
        flow: Flow<List<PendingTransfer>>,
    ) {
        whenever(
            monitorPendingTransfersUntilResolvedUseCase(transferType)
        ).thenReturn(flow)
    }

    companion object {
        private const val NODE_HANDLE = 10L
        private const val PARENT_NODE_HANDLE = 12L
        private const val CHAT_ID = 20L
        private val uploadUri = UriPath("foo")
        private val nodeId = NodeId(NODE_HANDLE)
        private val parentId = NodeId(PARENT_NODE_HANDLE)
        private const val DESTINATION = "/destination/"
        private const val NODE_NAME = "node.txt"
    }
}
