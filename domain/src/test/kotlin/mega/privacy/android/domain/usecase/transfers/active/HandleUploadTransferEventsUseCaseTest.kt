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
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeAll
    fun setUp() {
        underTest = HandleUploadTransferEventsUseCase(
            setNodeAttributesAfterUploadUseCase,
            testScope
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(setNodeAttributesAfterUploadUseCase)
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

    @Test
    fun `test that multiple calls to use case do not block each other`() = runTest(testDispatcher) {
        val transfer1 = createTransferMock(TransferType.GENERAL_UPLOAD)
        val transfer2 = createTransferMock(TransferType.CHAT_UPLOAD)
        val finishEvent1 = TransferEvent.TransferFinishEvent(transfer1, null)
        val finishEvent2 = TransferEvent.TransferFinishEvent(transfer2, null)

        // Both calls should return immediately
        underTest(finishEvent1)
        underTest(finishEvent2)

        // At this point, neither background processing should have started yet
        verifyNoInteractions(setNodeAttributesAfterUploadUseCase)

        // Advance the dispatcher to allow both background processes to complete
        advanceUntilIdle()

        // Now both background processes should have completed
        verify(setNodeAttributesAfterUploadUseCase)(
            nodeHandle = transfer1.nodeHandle,
            uriPath = UriPath(transfer1.localPath),
            appData = transfer1.appData
        )
        verify(setNodeAttributesAfterUploadUseCase)(
            nodeHandle = transfer2.nodeHandle,
            uriPath = UriPath(transfer2.localPath),
            appData = transfer2.appData
        )
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

    private var lastNodeId = 343L
}