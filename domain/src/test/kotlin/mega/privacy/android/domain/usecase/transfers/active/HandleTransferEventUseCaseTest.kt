package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleTransferEventUseCaseTest {

    private lateinit var underTest: HandleTransferEventUseCase

    private val transferRepository = mock<TransferRepository>()
    private val broadcastBusinessAccountExpiredUseCase =
        mock<BroadcastBusinessAccountExpiredUseCase>()
    private val crashReporter = mock<CrashReporter>()
    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = HandleTransferEventUseCase(
            transferRepository = transferRepository,
            broadcastBusinessAccountExpiredUseCase = broadcastBusinessAccountExpiredUseCase,
            crashReporter = crashReporter,
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository,
            broadcastBusinessAccountExpiredUseCase,
            crashReporter,
            cameraUploadsRepository
        )
    }

    @ParameterizedTest
    @MethodSource("provideStartPauseFinishEvents")
    fun `test that invoke call insertOrUpdateActiveTransfers with the related transfer when the event is a start, pause, or finish event`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(transferRepository).insertOrUpdateActiveTransfers(eq(listOf(transferEvent.transfer)))
    }

    @Test
    fun `test that invoke call clearRecursiveTransferAppDataFromCache after insertOrUpdateActiveTransfers with the related transfer when the event is a folder finish event`() =
        runTest {
            val transferEvent = mockTransferEvent<TransferEvent.TransferFinishEvent>(
                TransferType.DOWNLOAD,
                1,
                isFolderTransfer = true
            )
            underTest.invoke(transferEvent)
            inOrder(transferRepository) {
                verify(transferRepository).insertOrUpdateActiveTransfers(eq(listOf(transferEvent.transfer)))
                verify(transferRepository).clearRecursiveTransferAppDataFromCache(transferEvent.transfer.tag)
            }
        }

    @ParameterizedTest
    @MethodSource("provideRecursiveTransferAppData")
    fun `test that parent group app data is added when child transfer start event is received`(
        appData: TransferAppData.RecursiveTransferAppData,
    ) =
        runTest {
            val parentTag = 2
            val transferEvent = mockTransferEvent<TransferEvent.TransferStartEvent>(
                TransferType.DOWNLOAD,
                1,
                folderTransferTag = parentTag
            )
            whenever(
                transferRepository.getRecursiveTransferAppDataFromParent(
                    eq(parentTag),
                    any()
                )
            ) doReturn listOf(appData)
            val expected = transferEvent.transfer.copy(appData = listOf(appData))
            underTest.invoke(transferEvent)
            verify(transferRepository).insertOrUpdateActiveTransfers(eq(listOf(expected)))
        }

    @ParameterizedTest
    @MethodSource("provideRecursiveTransferAppData")
    fun `test that parent group app data is added when child transfer paused event is received`(
        appData: TransferAppData.RecursiveTransferAppData,
    ) =
        runTest {
            val parentTag = 2
            val transferEvent = mockTransferEvent<TransferEvent.TransferPaused>(
                TransferType.DOWNLOAD,
                1,
                folderTransferTag = parentTag
            )
            whenever(
                transferRepository.getRecursiveTransferAppDataFromParent(
                    eq(parentTag),
                    any()
                )
            ) doReturn listOf(appData)
            val expected = transferEvent.transfer.copy(appData = listOf(appData))
            underTest.invoke(transferEvent)
            verify(transferRepository).insertOrUpdateActiveTransfers(eq(listOf(expected)))
        }

    @ParameterizedTest
    @MethodSource("provideRecursiveTransferAppData")
    fun `test that parent group app data is added when child transfer finish event is received`(
        appData: TransferAppData.RecursiveTransferAppData,
    ) =
        runTest {
            val parentTag = 2
            val transferEvent = mockTransferEvent<TransferEvent.TransferFinishEvent>(
                TransferType.DOWNLOAD,
                1,
                folderTransferTag = parentTag
            )
            whenever(
                transferRepository.getRecursiveTransferAppDataFromParent(
                    eq(parentTag),
                    any()
                )
            ) doReturn listOf(appData)
            val expected = transferEvent.transfer.copy(appData = listOf(appData))
            underTest.invoke(transferEvent)
            verify(transferRepository).insertOrUpdateActiveTransfers(eq(listOf(expected)))
        }

    @Test
    fun `test that invoke call insertOrUpdateActiveTransfers with the last event of each transfer when multiple events are send`() =
        runTest {
            val events1 = listOf(
                mockTransferEvent<TransferEvent.TransferStartEvent>(TransferType.DOWNLOAD, 1),
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 1),
                mockTransferEvent<TransferEvent.TransferFinishEvent>(TransferType.DOWNLOAD, 1),
            )
            val events2 = listOf(
                mockTransferEvent<TransferEvent.TransferStartEvent>(TransferType.DOWNLOAD, 2),
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 2) {
                    on { paused } doReturn true
                },
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 2) {
                    on { paused } doReturn false
                },
            )
            val events3 = listOf(
                mockTransferEvent<TransferEvent.TransferFinishEvent>(TransferType.DOWNLOAD, 3),
            )
            underTest.invoke(events = (events1 + events2 + events3).toTypedArray())
            verify(transferRepository).insertOrUpdateActiveTransfers(
                eq(
                    listOf(
                        events1.last().transfer,
                        events2.last().transfer,
                        events3.last().transfer,
                    )
                )
            )
        }

    @ParameterizedTest
    @MethodSource("provideUpdateFinishEvents")
    fun `test that invoke call updateTransferredBytes with the related transfer when the event is a update or finish event`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(transferRepository).updateTransferredBytes(listOf(transferEvent.transfer))
    }

    @ParameterizedTest
    @MethodSource("provideFinishEventsWithError")
    fun `test that invoke call broadcastBusinessAccountExpiredUseCase when the event is a finish event with BusinessAccountExpiredMegaException`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(broadcastBusinessAccountExpiredUseCase).invoke()
    }

    @ParameterizedTest
    @MethodSource("provideFinishEvents")
    fun `test that invoke call addCompletedTransfers when the event is a finish event`(
        transferEvent: TransferEvent.TransferFinishEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(transferRepository).addCompletedTransfers(eq(listOf(transferEvent)))
    }

    @Test
    fun `test that invoke call addCompletedTransfers with all events when multiple events are send`() =
        runTest {
            val events = listOf(
                mockTransferEvent<TransferEvent.TransferFinishEvent>(TransferType.DOWNLOAD, 1),
                mockTransferEvent<TransferEvent.TransferFinishEvent>(TransferType.DOWNLOAD, 2),
                mockTransferEvent<TransferEvent.TransferFinishEvent>(TransferType.DOWNLOAD, 3),
            )
            underTest.invoke(events = events.toTypedArray())
            verify(transferRepository).addCompletedTransfers(eq(events))
        }

    @Test
    fun `test that does not invoke call addCompletedTransfers when the event is a finish event and transfer is a preview`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.tag } doReturn 1
                on { this.appData }.thenReturn(listOf(TransferAppData.PreviewDownload))
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }

            underTest.invoke(transferEvent)

            verify(transferRepository, never()).addCompletedTransferFromFailedPendingTransfer(
                any(),
                any(),
                any()
            )
        }

    @Test
    fun `test that does not invoke call addCompletedTransfers when the event is a finish event and transfer is a folder`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.tag } doReturn 1
                on { isFolderTransfer } doReturn true
                on { this.appData }.thenReturn(emptyList())
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }

            underTest.invoke(transferEvent)

            verify(transferRepository, never()).addCompletedTransferFromFailedPendingTransfer(
                any(),
                any(),
                any()
            )
        }

    @ParameterizedTest
    @MethodSource("provideStartPauseUpdateEvents")
    fun `test that updateInProgressTransfers in repository is invoked when start, pause or update event is received`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest(transferEvent)
        verify(transferRepository).updateInProgressTransfers(eq(listOf(transferEvent.transfer)))
    }

    @ParameterizedTest
    @MethodSource("provideStartPauseUpdateEvents")
    fun `test that updateCameraUploadsInProgressTransfers in repository is invoked when start, pause or update CU transfer event is received`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest(transferEvent)
        if (transferEvent.transfer.transferType == TransferType.CU_UPLOAD) {
            verify(cameraUploadsRepository).updateCameraUploadsInProgressTransfers(
                eq(listOf(transferEvent.transfer))
            )
        } else {
            verify(cameraUploadsRepository, never()).updateCameraUploadsInProgressTransfers(any())
        }
    }

    @Test
    fun `test that invoke calls updateInProgressTransfers with the last event of each transfer when multiple events are send`() =
        runTest {
            val events1 = listOf(
                mockTransferEvent<TransferEvent.TransferStartEvent>(TransferType.DOWNLOAD, 1),
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 1),
                mockTransferEvent<TransferEvent.TransferUpdateEvent>(TransferType.DOWNLOAD, 1),
            )
            val events2 = listOf(
                mockTransferEvent<TransferEvent.TransferStartEvent>(TransferType.DOWNLOAD, 2),
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 2) {
                    on { paused } doReturn true
                },
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 2) {
                    on { paused } doReturn false
                },
            )
            val events3 = listOf(
                mockTransferEvent<TransferEvent.TransferUpdateEvent>(TransferType.DOWNLOAD, 3),
            )
            underTest.invoke(events = (events1 + events2 + events3).toTypedArray())
            verify(transferRepository).updateInProgressTransfers(
                eq(
                    listOf(
                        events1.last().transfer,
                        events2.last().transfer,
                        events3.last().transfer,
                    )
                )
            )
        }

    @Test
    fun `test that invoke calls updateCameraUploadsInProgressTransfers with the last event of each transfer when multiple CU transfer events are send`() =
        runTest {
            val events1 = listOf(
                mockTransferEvent<TransferEvent.TransferStartEvent>(TransferType.CU_UPLOAD, 1),
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.CU_UPLOAD, 1),
                mockTransferEvent<TransferEvent.TransferUpdateEvent>(TransferType.CU_UPLOAD, 1),
            )
            val events2 = listOf(
                mockTransferEvent<TransferEvent.TransferStartEvent>(TransferType.DOWNLOAD, 2),
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 2) {
                    on { paused } doReturn true
                },
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 2) {
                    on { paused } doReturn false
                },
            )
            val events3 = listOf(
                mockTransferEvent<TransferEvent.TransferUpdateEvent>(TransferType.CU_UPLOAD, 3),
            )
            underTest.invoke(events = (events1 + events2 + events3).toTypedArray())
            verify(cameraUploadsRepository).updateCameraUploadsInProgressTransfers(
                eq(
                    listOf(
                        events1.last().transfer,
                        events3.last().transfer,
                    )
                )
            )
        }

    @ParameterizedTest(name = ". Event: {0}")
    @MethodSource("provideTransferEventsForPerformanceIssues")
    fun `test that checkPossiblePerformanceIssues invokes crashReporter if required`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest.invoke(transferEvent)

        with(transferEvent) {
            if (this is TransferEvent.FolderTransferUpdateEvent
                && stage == TransferStage.STAGE_TRANSFERRING_FILES
                && fileCount > POSSIBLE_PERFORMANCE_ISSUE_NUMBER
            ) {
                val mainText =
                    if (transfer.transferType == TransferType.DOWNLOAD) DOWNLOADING_FOLDER_STRING
                    else UPLOADING_FOLDER_STRING

                verify(crashReporter).log(
                    mainText + FOLDER_COUNT_STRING + folderCount + FILE_COUNT_STRING + fileCount
                )
            } else {
                verifyNoInteractions(crashReporter)
            }
        }
    }

    private fun provideTransferEventsForPerformanceIssues() =
        provideTransferEvents<TransferEvent.TransferStartEvent>() +
                provideTransferEvents<TransferEvent.TransferFinishEvent>() +
                provideTransferEvents<TransferEvent.TransferUpdateEvent>() +
                provideTransferEvents<TransferEvent.TransferPaused>() +
                provideTransferEvents<TransferEvent.TransferTemporaryErrorEvent>() +
                provideTransferEvents<TransferEvent.FolderTransferUpdateEvent> {
                    on { stage } doReturn TransferStage.STAGE_TRANSFERRING_FILES
                    on { fileCount } doReturn 10001
                    on { folderCount } doReturn 45
                } +
                provideTransferEvents<TransferEvent.FolderTransferUpdateEvent> {
                    on { stage } doReturn TransferStage.STAGE_TRANSFERRING_FILES
                    on { fileCount } doReturn 9999
                    on { folderCount } doReturn 67
                } +
                provideTransferEvents<TransferEvent.FolderTransferUpdateEvent> {
                    on { stage } doReturn TransferStage.STAGE_NONE
                } +
                provideTransferEvents<TransferEvent.FolderTransferUpdateEvent> {
                    on { stage } doReturn TransferStage.STAGE_SCANNING
                } +
                provideTransferEvents<TransferEvent.FolderTransferUpdateEvent> {
                    on { stage } doReturn TransferStage.STAGE_CREATING_TREE
                }

    private fun provideStartPauseFinishEvents() =
        provideTransferEvents<TransferEvent.TransferStartEvent>() +
                provideTransferEvents<TransferEvent.TransferPaused>() +
                provideTransferEvents<TransferEvent.TransferFinishEvent>()

    private fun provideUpdateFinishEvents() =
        provideTransferEvents<TransferEvent.TransferUpdateEvent>() +
                provideTransferEvents<TransferEvent.TransferFinishEvent>()

    private fun provideFinishEvents(): List<TransferEvent.TransferFinishEvent> =
        provideFinishEventsWithError() +
                provideTransferEvents()

    private fun provideFinishEventsWithError() =
        provideTransferEvents<TransferEvent.TransferFinishEvent> {
            on { this.error }.thenReturn(BusinessAccountExpiredMegaException(1))
        }

    private fun provideStartFinishEvents() =
        provideTransferEvents<TransferEvent.TransferStartEvent>() +
                provideTransferEvents<TransferEvent.TransferFinishEvent>()

    private fun provideStartPauseUpdateEvents() =
        provideTransferEvents<TransferEvent.TransferStartEvent>() +
                provideTransferEvents<TransferEvent.TransferPaused>() +
                provideTransferEvents<TransferEvent.TransferUpdateEvent>()

    private fun provideRecursiveTransferAppData() = listOf(
        TransferAppData.TransferGroup(245),
        TransferAppData.OfflineDownload
    )


    private inline fun <reified T : TransferEvent> provideTransferEvents(
        transferUniqueId: Long = 0,
        stubbing: KStubbing<T>.(T) -> Unit = {},
    ) =
        TransferType.entries.map { transferType ->
            mockTransferEvent(transferType, transferUniqueId, stubbing = stubbing)
        }

    private inline fun <reified T : TransferEvent> mockTransferEvent(
        transferType: TransferType,
        transferUniqueId: Long = 0,
        folderTransferTag: Int? = null,
        isFolderTransfer: Boolean = false,
        stubbing: KStubbing<T>.(T) -> Unit = {},
    ): T {
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(transferType)
            on { this.uniqueId }.thenReturn(transferUniqueId)
            on { this.folderTransferTag }.thenReturn(folderTransferTag)
            on { this.isFolderTransfer }.thenReturn(isFolderTransfer)
            on { this.appData }.thenReturn(emptyList())
        }
        return mock<T> {
            on { this.transfer }.thenReturn(transfer)
            stubbing(it)
        }
    }
}