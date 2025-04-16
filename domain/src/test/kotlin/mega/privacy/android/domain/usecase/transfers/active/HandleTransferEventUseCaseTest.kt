package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.DestinationUriAndSubFolders
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.HandleAvailableOfflineEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.sd.GetTransferDestinationUriUseCase
import mega.privacy.android.domain.usecase.transfers.sd.HandleSDCardEventUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleTransferEventUseCaseTest {

    private lateinit var underTest: HandleTransferEventUseCase

    private val transferRepository = mock<TransferRepository>()
    private val broadcastBusinessAccountExpiredUseCase =
        mock<BroadcastBusinessAccountExpiredUseCase>()
    private val broadcastTransferOverQuotaUseCase = mock<BroadcastTransferOverQuotaUseCase>()
    private val handleAvailableOfflineEventUseCase = mock<HandleAvailableOfflineEventUseCase>()
    private val handleSDCardEventUseCase = mock<HandleSDCardEventUseCase>()
    private val getTransferDestinationUriUseCase = mock<GetTransferDestinationUriUseCase>()
    private val broadcastStorageOverQuotaUseCase = mock<BroadcastStorageOverQuotaUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = HandleTransferEventUseCase(
            transferRepository = transferRepository,
            broadcastBusinessAccountExpiredUseCase = broadcastBusinessAccountExpiredUseCase,
            broadcastTransferOverQuotaUseCase = broadcastTransferOverQuotaUseCase,
            broadcastStorageOverQuotaUseCase = broadcastStorageOverQuotaUseCase,
            handleAvailableOfflineEventUseCase = handleAvailableOfflineEventUseCase,
            handleSDCardEventUseCase = handleSDCardEventUseCase,
            getTransferDestinationUriUseCase = getTransferDestinationUriUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository,
            broadcastBusinessAccountExpiredUseCase,
            broadcastTransferOverQuotaUseCase,
            broadcastStorageOverQuotaUseCase,
            handleAvailableOfflineEventUseCase,
            handleSDCardEventUseCase,
            getTransferDestinationUriUseCase,
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
            whenever(transferRepository.getRecursiveTransferAppDataFromParent(parentTag)) doReturn
                    listOf(appData)
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
            whenever(transferRepository.getRecursiveTransferAppDataFromParent(parentTag)) doReturn
                    listOf(appData)
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
            whenever(transferRepository.getRecursiveTransferAppDataFromParent(parentTag)) doReturn
                    listOf(appData)
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

    @Test
    fun `test that broadcastTransferOverQuotaUseCase is invoked when a QuotaExceededMegaException is received as a temporal error for download Event`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(TransferType.DOWNLOAD)
            }
            val transferEvent = mock<TransferEvent.TransferTemporaryErrorEvent> {
                on { this.transfer }.thenReturn(transfer)
                on { this.error }.thenReturn(QuotaExceededMegaException(1, value = 1))
            }
            underTest.invoke(transferEvent)
            verify(broadcastTransferOverQuotaUseCase).invoke(true)
            verifyNoInteractions(broadcastStorageOverQuotaUseCase)
        }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD", "CU_UPLOAD"])
    fun `test that broadcastStorageOverQuotaUseCase is invoked when a QuotaExceededMegaException is received as a temporal error for upload Event and the transfer isForeignOverQuota value is false`(
        type: TransferType,
    ) = runTest {
        reset(broadcastStorageOverQuotaUseCase)
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(type)
            on { this.isForeignOverQuota }.thenReturn(false)
        }
        val transferEvent = mock<TransferEvent.TransferTemporaryErrorEvent> {
            on { this.transfer }.thenReturn(transfer)
            on { this.error }.thenReturn(QuotaExceededMegaException(1, value = 1))
        }
        underTest.invoke(transferEvent)
        verify(broadcastStorageOverQuotaUseCase).invoke(true)
        verifyNoMoreInteractions(broadcastTransferOverQuotaUseCase)
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD", "CU_UPLOAD"])
    fun `test that broadcastStorageOverQuotaUseCase is not invoked when a QuotaExceededMegaException is received as a temporal error for upload Event and the transfer isForeignOverQuota value is true`(
        type: TransferType,
    ) = runTest {
        reset(broadcastStorageOverQuotaUseCase)
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(type)
            on { this.isForeignOverQuota }.thenReturn(true)
        }
        val transferEvent = mock<TransferEvent.TransferTemporaryErrorEvent> {
            on { this.transfer }.thenReturn(transfer)
            on { this.error }.thenReturn(QuotaExceededMegaException(1, value = 1))
        }
        underTest.invoke(transferEvent)
        verifyNoInteractions(broadcastTransferOverQuotaUseCase)
    }

    @Test
    fun `test that broadcastTransferOverQuotaUseCase is invoked with parameter equals to false when a Start event is received for download Event`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(TransferType.DOWNLOAD)
            }
            val transferEvent = mock<TransferEvent.TransferStartEvent> {
                on { this.transfer }.thenReturn(transfer)
            }
            underTest.invoke(transferEvent)
            verify(broadcastTransferOverQuotaUseCase).invoke(false)
            verifyNoInteractions(broadcastStorageOverQuotaUseCase)
        }

    @Test
    fun `test that broadcastTransferOverQuotaUseCase is invoked with parameter equals to false when a Update event is received for download Event`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(TransferType.DOWNLOAD)
            }
            val transferEvent = mock<TransferEvent.TransferUpdateEvent> {
                on { this.transfer }.thenReturn(transfer)
            }
            underTest.invoke(transferEvent)
            verify(broadcastTransferOverQuotaUseCase).invoke(false)
            verifyNoInteractions(broadcastStorageOverQuotaUseCase)
        }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD", "CU_UPLOAD"])
    fun `test that broadcastStorageOverQuotaUseCase is invoked with parameter equals to false when a Start event is received for upload Event`(
        type: TransferType,
    ) = runTest {
        reset(broadcastStorageOverQuotaUseCase)
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(type)
        }
        val transferEvent = mock<TransferEvent.TransferStartEvent> {
            on { this.transfer }.thenReturn(transfer)
        }
        underTest.invoke(transferEvent)
        verify(broadcastStorageOverQuotaUseCase).invoke(false)
        verifyNoInteractions(broadcastTransferOverQuotaUseCase)
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD", "CU_UPLOAD"])
    fun `test that broadcastStorageOverQuotaUseCase is invoked with parameter equals to false when an Update event is received for upload Event`(
        type: TransferType,
    ) = runTest {
        reset(broadcastStorageOverQuotaUseCase)
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(type)
        }
        val transferEvent = mock<TransferEvent.TransferUpdateEvent> {
            on { this.transfer }.thenReturn(transfer)
        }
        underTest.invoke(transferEvent)
        verify(broadcastStorageOverQuotaUseCase).invoke(false)
        verifyNoInteractions(broadcastTransferOverQuotaUseCase)
    }

    @ParameterizedTest
    @MethodSource("provideFinishEvents")
    fun `test that invoke call addCompletedTransfers when the event is a finish event`(
        transferEvent: TransferEvent.TransferFinishEvent,
    ) = runTest {
        val destinationUriAndSubFolders = DestinationUriAndSubFolders("folder/file")
        whenever(getTransferDestinationUriUseCase(transferEvent.transfer)) doReturn destinationUriAndSubFolders

        underTest.invoke(transferEvent)
        verify(transferRepository).addCompletedTransfers(eq(mapOf(transferEvent to destinationUriAndSubFolders.toString())))
    }

    @Test
    fun `test that invoke call addCompletedTransfers with all events when multiple events are send`() =
        runTest {
            val destinationUriAndSubFolders = DestinationUriAndSubFolders("folder/file")
            whenever(getTransferDestinationUriUseCase(any())) doReturn destinationUriAndSubFolders
            val events = listOf(
                mockTransferEvent<TransferEvent.TransferFinishEvent>(TransferType.DOWNLOAD, 1),
                mockTransferEvent<TransferEvent.TransferFinishEvent>(TransferType.DOWNLOAD, 2),
                mockTransferEvent<TransferEvent.TransferFinishEvent>(TransferType.DOWNLOAD, 3),
            )
            val expected = events.associateWith { destinationUriAndSubFolders.toString() }
            underTest.invoke(events = events.toTypedArray())
            verify(transferRepository).addCompletedTransfers(eq(expected))
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
            val destinationUriAndSubFolders = DestinationUriAndSubFolders("folder/file")

            whenever(getTransferDestinationUriUseCase(transferEvent.transfer)) doReturn destinationUriAndSubFolders

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
            val destinationUriAndSubFolders = DestinationUriAndSubFolders("folder/file")

            whenever(getTransferDestinationUriUseCase(transferEvent.transfer)) doReturn destinationUriAndSubFolders

            underTest.invoke(transferEvent)

            verify(transferRepository, never()).addCompletedTransferFromFailedPendingTransfer(
                any(),
                any(),
                any()
            )
        }

    @ParameterizedTest
    @MethodSource("provideStartFinishEvents")
    fun `test that handleSDCardEventUseCase with correct destination is invoked when start and update event is received`(
        transferEvent: TransferEvent,
    ) = runTest {
        val destinationUriAndSubFolders = mock<DestinationUriAndSubFolders>()
        whenever(getTransferDestinationUriUseCase(transferEvent.transfer)) doReturn destinationUriAndSubFolders
        underTest.invoke(transferEvent)
        verify(
            handleSDCardEventUseCase,
        ).invoke(transferEvent, destinationUriAndSubFolders)
    }

    @ParameterizedTest
    @MethodSource("provideFinishEvents")
    fun `test that handleAvailableOfflineEventUseCase is invoked when finish event is received`(
        transferEvent: TransferEvent.TransferFinishEvent,
    ) = runTest {
        underTest(transferEvent)
        verify(handleAvailableOfflineEventUseCase).invoke(transferEvent)
    }

    @ParameterizedTest
    @MethodSource("provideStartPauseUpdateEvents")
    fun `test that updateInProgressTransfers in repository is invoked when start, pause or update event is received`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest(transferEvent)
        verify(transferRepository).updateInProgressTransfers(eq(listOf(transferEvent.transfer)))
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