package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.BroadcastOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddOrUpdateActiveTransferUseCaseTest {

    private lateinit var underTest: AddOrUpdateActiveTransferUseCase

    private val transferRepository = mock<TransferRepository>()
    private val broadcastBusinessAccountExpiredUseCase =
        mock<BroadcastBusinessAccountExpiredUseCase>()
    private val broadcastTransferOverQuotaUseCase = mock<BroadcastTransferOverQuotaUseCase>()
    private val broadcastOfflineFileAvailabilityUseCase =
        mock<BroadcastOfflineFileAvailabilityUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = AddOrUpdateActiveTransferUseCase(
            transferRepository = transferRepository,
            broadcastBusinessAccountExpiredUseCase = broadcastBusinessAccountExpiredUseCase,
            broadcastTransferOverQuotaUseCase = broadcastTransferOverQuotaUseCase,
            broadcastOfflineFileAvailabilityUseCase = broadcastOfflineFileAvailabilityUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository,
            broadcastBusinessAccountExpiredUseCase,
            broadcastTransferOverQuotaUseCase,
            broadcastOfflineFileAvailabilityUseCase
        )
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

    @ParameterizedTest
    @MethodSource("provideQuotaExceededMegaExceptionTemporaryErrorEvents")
    fun `test that broadcastTransferOverQuotaUseCase is invoked when a QuotaExceededMegaException is received as a temporal error`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(broadcastTransferOverQuotaUseCase).invoke(true)
    }

    @ParameterizedTest
    @MethodSource("provideFinishEvents")
    fun `test that broadcastOfflineFileAvailabilityUseCase is invoked when the event is a download finish event`(
        transferEvent: TransferEvent.TransferFinishEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        if (transferEvent.transfer.transferType == TransferType.DOWNLOAD) {
            verify(broadcastOfflineFileAvailabilityUseCase).invoke(transferEvent.transfer.nodeHandle)
        } else {
            verifyNoInteractions(broadcastOfflineFileAvailabilityUseCase)
        }
    }

    @ParameterizedTest
    @MethodSource("provideFinishEvents")
    fun `test that invoke call addCompletedTransfer when the event is a finish event`(
        transferEvent: TransferEvent.TransferFinishEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(transferRepository).addCompletedTransfer(transferEvent.transfer, transferEvent.error)
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

    private fun provideQuotaExceededMegaExceptionTemporaryErrorEvents() =
        provideTransferEvents<TransferEvent.TransferTemporaryErrorEvent> {
            on { this.error }.thenReturn(QuotaExceededMegaException(1, value = 1))
        }


    private inline fun <reified T : TransferEvent> provideTransferEvents(stubbing: KStubbing<T>.(T) -> Unit = {}) =
        TransferType.values().map { transferType ->
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(transferType)
            }
            mock<T> {
                on { this.transfer }.thenReturn(transfer)
                stubbing(it)
            }
        }
}