package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorPendingMessageTransferEventsUseCaseTest {

    private lateinit var underTest: MonitorPendingMessageTransferEventsUseCase

    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase = mock()
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase = mock()

    private val transferEventFlow = MutableSharedFlow<TransferEvent>()
    private val pendingMessageId1 = 123L
    private val pendingMessageId2 = 234L
    private val transfer1 = mock<Transfer> {
        on { appData } doReturn listOf(TransferAppData.ChatUpload(pendingMessageId1))
    }
    private val transfer2 = mock<Transfer> {
        on { appData } doReturn listOf(TransferAppData.ChatUpload(pendingMessageId2))
    }

    @BeforeAll
    fun setUp() {
        underTest = MonitorPendingMessageTransferEventsUseCase(
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            getInProgressTransfersUseCase = getInProgressTransfersUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getInProgressTransfersUseCase)
        wheneverBlocking { monitorTransferEventsUseCase() } doReturn transferEventFlow
    }

    @Test
    fun `test that no event is emitted if no transfer event update is received and there is no in progress transfers`() =
        runTest {
            whenever(getInProgressTransfersUseCase()).thenReturn(emptyList())

            underTest().test {
                expectNoEvents()
            }
        }

    @Test
    fun `test that two events are emitted if no transfer event update is received and there are two in progress chat upload transfers`() =
        runTest {
            whenever(getInProgressTransfersUseCase()).thenReturn(listOf(transfer1, transfer2))

            underTest().test {
                assertThat(awaitItem()).isEqualTo(Pair(listOf(pendingMessageId1), transfer1))
                assertThat(awaitItem()).isEqualTo(Pair(listOf(pendingMessageId2), transfer2))
            }
        }

    @Test
    fun `test that two events are emitted if a transfer event update is received and there are one in progress chat upload transfers`() =
        runTest {
            whenever(getInProgressTransfersUseCase()).thenReturn(listOf(transfer1))

            underTest().test {
                assertThat(awaitItem()).isEqualTo(Pair(listOf(pendingMessageId1), transfer1))
                transferEventFlow.emit(TransferEvent.TransferUpdateEvent(transfer2))
                assertThat(awaitItem()).isEqualTo(Pair(listOf(pendingMessageId2), transfer2))
            }
        }

    @Test
    fun `test that two events are emitted if two transfer event updates are received and there is no in progress chat upload transfers`() =
        runTest {
            whenever(getInProgressTransfersUseCase()).thenReturn(emptyList())

            underTest().test {
                transferEventFlow.emit(TransferEvent.TransferUpdateEvent(transfer1))
                assertThat(awaitItem()).isEqualTo(Pair(listOf(pendingMessageId1), transfer1))
                transferEventFlow.emit(TransferEvent.TransferUpdateEvent(transfer2))
                assertThat(awaitItem()).isEqualTo(Pair(listOf(pendingMessageId2), transfer2))
            }
        }
}