package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.BroadcastOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.offline.IsOfflineTransferUseCase
import mega.privacy.android.domain.usecase.offline.SaveOfflineNodeInformationUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleAvailableOfflineEventUseCaseTest {
    private lateinit var underTest: HandleAvailableOfflineEventUseCase

    private val isOfflineTransferUseCase = mock<IsOfflineTransferUseCase>()
    private val saveOfflineNodeInformationUseCase = mock<SaveOfflineNodeInformationUseCase>()
    private val broadcastOfflineFileAvailabilityUseCase =
        mock<BroadcastOfflineFileAvailabilityUseCase>()

    @BeforeAll
    fun setup() {

        underTest = HandleAvailableOfflineEventUseCase(
            isOfflineTransferUseCase,
            saveOfflineNodeInformationUseCase,
            broadcastOfflineFileAvailabilityUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            isOfflineTransferUseCase,
            saveOfflineNodeInformationUseCase,
            broadcastOfflineFileAvailabilityUseCase,
        )
    }

    @Test
    fun `test that offline node information is saved when a finish event for an offline download transfer is received`() =
        runTest {
            val transfer = mockTransfer()
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
            }
            whenever(isOfflineTransferUseCase(transfer)) doReturn true

            underTest(event)

            verify(saveOfflineNodeInformationUseCase).invoke(NodeId(nodeId))
        }

    @Test
    fun `test that offline node information is not invoked when a finish event for not offline download transfer is received`() =
        runTest {
            val transfer = mockTransfer()
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
            }
            whenever(isOfflineTransferUseCase(transfer)) doReturn false

            underTest(event)

            verifyNoInteractions(saveOfflineNodeInformationUseCase)
        }

    private fun mockTransfer() = mock<Transfer> {
        on { it.nodeHandle } doReturn nodeId
        on { it.transferType } doReturn TransferType.DOWNLOAD
    }

    @Test
    fun `test that offline node information is not invoked when a finish event with error is received`() =
        runTest {
            val transfer = mockTransfer()
            val error = mock<MegaException>()
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
                on { it.error } doReturn error
            }
            whenever(isOfflineTransferUseCase(transfer)) doReturn true

            underTest(event)

            verifyNoInteractions(saveOfflineNodeInformationUseCase)
        }

    @ParameterizedTest
    @MethodSource("provideNotFinishEvents")
    fun `test that offline node information is not invoked when is not a finish event`(
        event: TransferEvent,
    ) =
        runTest {

            whenever(isOfflineTransferUseCase(event.transfer)) doReturn true

            underTest(event)

            verifyNoInteractions(saveOfflineNodeInformationUseCase)
        }

    @Test
    fun `test that broadcastOfflineFileAvailabilityUseCase is invoked when a finish event for an offline download transfer is received`() =
        runTest {
            val transfer = mockTransfer()
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
            }
            whenever(isOfflineTransferUseCase(transfer)) doReturn true

            underTest(event)

            verify(broadcastOfflineFileAvailabilityUseCase).invoke(nodeId)
        }

    private fun provideNotFinishEvents(): List<TransferEvent> {

        val transfer = mockTransfer()
        return listOf(
            mock<TransferEvent.TransferStartEvent> {
                on { it.transfer } doReturn transfer
            },
            mock<TransferEvent.TransferUpdateEvent> {
                on { it.transfer } doReturn transfer
            },
            mock<TransferEvent.TransferDataEvent> {
                on { it.transfer } doReturn transfer
            },

            mock<TransferEvent.TransferTemporaryErrorEvent> {
                on { it.transfer } doReturn transfer
            },

            mock<TransferEvent.TransferPaused> {
                on { it.transfer } doReturn transfer
            },
        )
    }

    private val nodeId = 1432L

}