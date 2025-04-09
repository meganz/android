package mega.privacy.android.domain.usecase.transfers.active


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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleUploadTransferEventsUseCaseTest {
    private lateinit var underTest: HandleUploadTransferEventsUseCase

    private val setNodeAttributesAfterUploadUseCase = mock<SetNodeAttributesAfterUploadUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = HandleUploadTransferEventsUseCase(
            setNodeAttributesAfterUploadUseCase
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
    ) = runTest {

        val transfer = createTransferMock(transferType)
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

        underTest(finishEvent)

        verify(setNodeAttributesAfterUploadUseCase)(
            nodeHandle = transfer.nodeHandle,
            uriPath = UriPath(transfer.localPath),
            appData = transfer.appData
        )
    }

    @Test
    fun `test that set node attributes use case is invoked for each correct transfer only when multiple finish transfer events are received`() =
        runTest {
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
    ) = runTest {
        val transfer = createTransferMock(transferType)
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

        underTest(finishEvent)

        verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that not transfer finish events are filtered out`(
        transferType: TransferType,
    ) = runTest {
        val transfer = createTransferMock(transferType)
        val updateEvent = TransferEvent.TransferUpdateEvent(transfer)

        underTest(updateEvent)

        verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
    }

    @ParameterizedTest
    @EnumSource(value = TransferType::class, names = ["GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that transfer events with errors are filtered out`(
        transferType: TransferType,
    ) = runTest {
        val transfer = createTransferMock(transferType)
        val finishEvent = TransferEvent.TransferFinishEvent(transfer, mock())

        underTest(finishEvent)

        verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
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