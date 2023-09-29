package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddOrUpdateActiveTransferUseCaseTest {

    private lateinit var underTest: AddOrUpdateActiveTransferUseCase

    private val transferRepository = mock<TransferRepository>()
    private val broadcastBusinessAccountExpiredUseCase =
        mock<BroadcastBusinessAccountExpiredUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = AddOrUpdateActiveTransferUseCase(
            transferRepository = transferRepository,
            broadcastBusinessAccountExpiredUseCase = broadcastBusinessAccountExpiredUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository, broadcastBusinessAccountExpiredUseCase)
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
    @MethodSource("provideFinishWithErrorEvents")
    fun `test that invoke call broadcastBusinessAccountExpiredUseCase when the event is a finish event with BusinessAccountExpiredMegaException`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest.invoke(transferEvent)
        verify(broadcastBusinessAccountExpiredUseCase).invoke()
    }

    private fun provideStartPauseFinishEvents() = TransferType.values().flatMap { transferType ->
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(transferType)
        }
        listOf(
            mock<TransferEvent.TransferStartEvent> {
                on { this.transfer }.thenReturn(transfer)
            },
            mock<TransferEvent.TransferPaused> {
                on { this.transfer }.thenReturn(transfer)
            },
            mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }
        )
    }

    private fun provideUpdateFinishEvents() = TransferType.values().flatMap { transferType ->
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(transferType)
        }
        listOf(
            mock<TransferEvent.TransferUpdateEvent> {
                on { this.transfer }.thenReturn(transfer)
            },
            mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer }.thenReturn(transfer)
            }
        )
    }

    private fun provideFinishWithErrorEvents() = TransferType.values().map { transferType ->
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(transferType)
        }
        mock<TransferEvent.TransferFinishEvent> {
            on { this.transfer }.thenReturn(transfer)
            on { this.error }.thenReturn(mock<BusinessAccountExpiredMegaException>())
        }
    }
}