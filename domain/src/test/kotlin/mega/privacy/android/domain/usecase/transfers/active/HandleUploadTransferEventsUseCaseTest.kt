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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

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

    @Test
    fun `test that set node attributes use case is invoked when a finish transfer event is received`() =
        runTest {
            val nodeId = 343L
            val localPath = "path"
            val appData = listOf(mock<TransferAppData.Geolocation>())

            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
                on { this.nodeHandle } doReturn nodeId
                on { this.appData } doReturn appData
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

            underTest(finishEvent)

            verify(setNodeAttributesAfterUploadUseCase)(
                nodeHandle = nodeId,
                uriPath = UriPath(localPath),
                appData = appData
            )
        }

    @Test
    fun `test that not upload transfer events are filtered out`() =
        runTest {
            val nodeId = 343L
            val localPath = "path"
            val appData = listOf(mock<TransferAppData.Geolocation>())

            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.nodeHandle } doReturn nodeId
                on { this.appData } doReturn appData
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

            underTest(finishEvent)

            verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
        }

    @Test
    fun `test that not transfer finish events are filtered out`() =
        runTest {
            val nodeId = 343L
            val localPath = "path"
            val appData = listOf(mock<TransferAppData.Geolocation>())

            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
                on { this.nodeHandle } doReturn nodeId
                on { this.appData } doReturn appData
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferUpdateEvent(transfer)

            underTest(finishEvent)

            verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
        }

    @Test
    fun `test that transfer events with errors are filtered out`() =
        runTest {
            val nodeId = 343L
            val localPath = "path"
            val appData = listOf(mock<TransferAppData.Geolocation>())

            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
                on { this.nodeHandle } doReturn nodeId
                on { this.appData } doReturn appData
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, mock())

            underTest(finishEvent)

            verifyNoInteractions(setNodeAttributesAfterUploadUseCase)
        }

}