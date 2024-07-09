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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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

    @Test
    fun `test that voice clip transfer does not invoke repository nor GetTransferDestinationUriUseCase when event is TransferFinishEvent`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.CHAT_UPLOAD
                on { this.appData } doReturn listOf(TransferAppData.VoiceClip)
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }

            underTest.invoke(transferEvent)
            verifyNoInteractions(getTransferDestinationUriUseCase)
            verifyNoInteractions(transferRepository)
        }

    @Test
    fun `test that background transfer does not invoke repository nor GetTransferDestinationUriUseCase when event is TransferFinishEvent`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.appData } doReturn listOf(TransferAppData.BackgroundTransfer)
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }

            underTest.invoke(transferEvent)
            verifyNoInteractions(getTransferDestinationUriUseCase)
            verifyNoInteractions(transferRepository)
        }

    @Test
    fun `test that streaming transfer does not invoke repository nor GetTransferDestinationUriUseCase when event is TransferFinishEvent`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.isStreamingTransfer } doReturn true
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }

            underTest.invoke(transferEvent)
            verifyNoInteractions(getTransferDestinationUriUseCase)
            verifyNoInteractions(transferRepository)
        }

    @Test
    fun `test that upload sync transfer does not invoke repository nor GetTransferDestinationUriUseCase when event is TransferFinishEvent`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
                on { this.isSyncTransfer } doReturn true
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }

            underTest.invoke(transferEvent)
            verifyNoInteractions(getTransferDestinationUriUseCase)
            verifyNoInteractions(transferRepository)
        }

    @Test
    fun `test that download sync transfer does not invoke repository nor GetTransferDestinationUriUseCase when event is TransferFinishEvent`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.isSyncTransfer } doReturn true
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }

            underTest.invoke(transferEvent)
            verifyNoInteractions(getTransferDestinationUriUseCase)
            verifyNoInteractions(transferRepository)
        }

    @Test
    fun `test that backups transfer does not invoke repository nor GetTransferDestinationUriUseCase when event is TransferFinishEvent`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
                on { this.isBackupTransfer } doReturn true
            }
            val transferEvent = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }

            underTest.invoke(transferEvent)
            verifyNoInteractions(getTransferDestinationUriUseCase)
            verifyNoInteractions(transferRepository)
        }

    @ParameterizedTest
    @MethodSource("provideStartPauseFinishEvents")
    fun `test that invoke call insertOrUpdateActiveTransfer with the related transfer when the event is a start, pause, or finish event`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(transferRepository).insertOrUpdateActiveTransfer(transferEvent.transfer)
    }

    @ParameterizedTest
    @MethodSource("provideUpdateFinishEvents")
    fun `test that invoke call updateTransferredBytes with the related transfer when the event is a update or finish event`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(transferRepository).updateTransferredBytes(transferEvent.transfer)
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
    fun `test that broadcastStorageOverQuotaUseCase is invoked when a QuotaExceededMegaException is received as a temporal error for upload Event`(
        type: TransferType,
    ) = runTest {
        reset(broadcastStorageOverQuotaUseCase)
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(type)
        }
        val transferEvent = mock<TransferEvent.TransferTemporaryErrorEvent> {
            on { this.transfer }.thenReturn(transfer)
            on { this.error }.thenReturn(QuotaExceededMegaException(1, value = 1))
        }
        underTest.invoke(transferEvent)
        verify(broadcastStorageOverQuotaUseCase).invoke(true)
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
    fun `test that invoke call addCompletedTransfer when the event is a finish event`(
        transferEvent: TransferEvent.TransferFinishEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(transferRepository).addCompletedTransfer(transferEvent.transfer, transferEvent.error)
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


    private inline fun <reified T : TransferEvent> provideTransferEvents(stubbing: KStubbing<T>.(T) -> Unit = {}) =
        TransferType.entries.map { transferType ->
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(transferType)
            }
            mock<T> {
                on { this.transfer }.thenReturn(transfer)
                stubbing(it)
            }
        }
}