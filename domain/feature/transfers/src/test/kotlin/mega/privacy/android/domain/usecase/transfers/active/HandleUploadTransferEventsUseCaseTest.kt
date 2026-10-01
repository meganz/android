package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleUploadTransferEventsUseCaseTest {
    private lateinit var underTest: HandleUploadTransferEventsUseCase

    private val setNodeAttributesAfterUploadUseCase = mock<SetNodeAttributesAfterUploadUseCase>()
    private val broadcastStorageOverQuotaUseCase = mock<BroadcastStorageOverQuotaUseCase>()
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private var lastNodeId = 343L

    @BeforeAll
    fun setUp() {
        underTest = HandleUploadTransferEventsUseCase(
            setNodeAttributesAfterUploadUseCase = setNodeAttributesAfterUploadUseCase,
            broadcastStorageOverQuotaUseCase = broadcastStorageOverQuotaUseCase,
            applicationScope = testScope,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            setNodeAttributesAfterUploadUseCase,
            broadcastStorageOverQuotaUseCase,
        )
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that set node attributes use case is invoked when a finish transfer event is received`(
        transferType: TransferType,
    ) = runTest(testDispatcher) {
        val transfer = createTransferMock(transferType)
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

        underTest(finishEvent)

        advanceUntilIdle()

        verify(setNodeAttributesAfterUploadUseCase)(
            nodeHandle = transfer.nodeHandle,
            uriPath = UriPath(transfer.localPath),
            appData = transfer.appData
        )
    }

    @Test
    fun `test that set node attributes use case is invoked for each correct transfer only when multiple finish transfer events are received`() =
        runTest(testDispatcher) {
            val uploadTransfer = createTransferMock(TransferType.GENERAL_UPLOAD)
            val chatTransfer = createTransferMock(TransferType.CHAT_UPLOAD)
            val uploadTransfer2 = createTransferMock(TransferType.GENERAL_UPLOAD)

            val finishUploadEvent = TransferEvent.TransferFinishEvent(uploadTransfer, null)
            val finishChatEvent = TransferEvent.TransferFinishEvent(chatTransfer, null)
            val finishUploadEventError = TransferEvent.TransferFinishEvent(uploadTransfer2, mock())
            val finishEventsOk = listOf(
                finishUploadEvent,
                finishChatEvent,
            )

            underTest(*finishEventsOk.toTypedArray() + finishUploadEventError)

            // Advance the dispatcher to allow the background processing to complete
            advanceUntilIdle()

            finishEventsOk.forEach {
                verify(setNodeAttributesAfterUploadUseCase)(
                    nodeHandle = it.transfer.nodeHandle,
                    uriPath = UriPath(it.transfer.localPath),
                    appData = it.transfer.appData
                )
            }
            verifyNoMoreInteractions(setNodeAttributesAfterUploadUseCase)
        }

    @ParameterizedTest
    @EnumSource(
        value = TransferType::class,
        names = ["GENERAL_UPLOAD", "CHAT_UPLOAD"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `test that not upload transfer events are filtered out`(
        transferType: TransferType,
    ) = runTest(testDispatcher) {
        val transfer = createTransferMock(transferType)
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

        underTest(finishEvent)

        advanceUntilIdle()

        verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that not transfer finish events are filtered out`(
        transferType: TransferType,
    ) = runTest(testDispatcher) {
        val transfer = createTransferMock(transferType)
        val updateEvent = TransferEvent.TransferUpdateEvent(transfer)

        underTest(updateEvent)

        advanceUntilIdle()

        verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that transfer events with errors are filtered out`(
        transferType: TransferType,
    ) = runTest(testDispatcher) {
        val transfer = createTransferMock(transferType)
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, mock())

        underTest(finishEvent)

        advanceUntilIdle()

        verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
    }

    @Test
    fun `test that use case returns immediately without blocking`() = runTest(testDispatcher) {
        val transfer = createTransferMock(TransferType.GENERAL_UPLOAD)
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

        // The use case should return immediately without waiting for the background processing
        underTest(finishEvent)

        // At this point, the use case has returned but the background processing hasn't started yet
        verifyNoInteractions(setNodeAttributesAfterUploadUseCase)

        // Now advance the dispatcher to allow the background processing to complete
        advanceUntilIdle()

        // Now the background processing should have completed
        verify(setNodeAttributesAfterUploadUseCase)(
            nodeHandle = transfer.nodeHandle,
            uriPath = UriPath(transfer.localPath),
            appData = transfer.appData
        )
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD", "CU_UPLOAD"])
    fun `test that broadcastStorageOverQuotaUseCase is invoked when a QuotaExceededMegaException is received as a temporal error for upload Event and the transfer isForeignOverQuota value is false`(
        type: TransferType,
    ) = runTest {
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
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD", "CU_UPLOAD"])
    fun `test that broadcastStorageOverQuotaUseCase is not invoked when a QuotaExceededMegaException is received as a temporal error for upload Event and the transfer isForeignOverQuota value is true`(
        type: TransferType,
    ) = runTest {
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(type)
            on { this.isForeignOverQuota }.thenReturn(true)
        }
        val transferEvent = mock<TransferEvent.TransferTemporaryErrorEvent> {
            on { this.transfer }.thenReturn(transfer)
            on { this.error }.thenReturn(QuotaExceededMegaException(1, value = 1))
        }
        underTest.invoke(transferEvent)
        verifyNoInteractions(broadcastStorageOverQuotaUseCase)
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD", "CU_UPLOAD"])
    fun `test that broadcastStorageOverQuotaUseCase is invoked with parameter equals to false when a Start event is received for upload Event`(
        type: TransferType,
    ) = runTest {
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(type)
        }
        val transferEvent = mock<TransferEvent.TransferStartEvent> {
            on { this.transfer }.thenReturn(transfer)
        }
        underTest.invoke(transferEvent)
        verify(broadcastStorageOverQuotaUseCase).invoke(false)
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
    }

    private fun createTransferMock(
        transferType: TransferType,
        nodeId: Long = lastNodeId + 1,
    ): Transfer {
        lastNodeId = nodeId
        val localPath = "path$nodeId"
        val appData = listOf(mock<TransferAppData.Geolocation>())

        return mock<Transfer> {
            on { this.transferType } doReturn transferType
            on { this.nodeHandle } doReturn nodeId
            on { this.appData } doReturn appData
            on { this.localPath } doReturn localPath
        }
    }

    private fun provideFinishEvents(): List<TransferEvent.TransferFinishEvent> = buildList {
        TransferType.entries.forEach { transferType ->
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(transferType)
                on { this.appData }.thenReturn(emptyList())
            }
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer } doReturn transfer
            }
            val eventError = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer } doReturn transfer
                on { this.error }.thenReturn(BusinessAccountExpiredMegaException(1))
            }
            add(event)
            add(eventError)
        }
    }
}