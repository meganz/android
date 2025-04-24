package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroupImpl
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InsertPendingUploadsForFilesUseCaseTest {
    private lateinit var underTest: InsertPendingUploadsForFilesUseCase

    private val transferRepository = mock<TransferRepository>()
    private val timeSystemRepository = mock<TimeSystemRepository>()
    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = InsertPendingUploadsForFilesUseCase(
            transferRepository,
            timeSystemRepository,
            nodeRepository,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            transferRepository,
            timeSystemRepository,
            nodeRepository,
        )
    }

    @Test
    fun `test that pending transfers are inserted with correct parameters`() = runTest {
        val pathsAndNames = (0..10).associate { "content://file$it" to "newName$it" }
        val parentFolderId = NodeId(242L)
        val currentTime = 398457L
        val destination = "/folder/sub-folder"
        val pendingTransferNodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(parentFolderId)
        whenever(timeSystemRepository.getCurrentTimeInMillis()) doReturn currentTime
        whenever(nodeRepository.getNodePathById(parentFolderId)) doReturn destination
        val transferGroupId = 2437865L
        whenever(
            transferRepository.insertActiveTransferGroup(
                ActiveTransferActionGroupImpl(
                    transferType = TransferType.GENERAL_UPLOAD,
                    destination = destination,
                    startTime = currentTime,
                    pendingTransferNodeId = pendingTransferNodeIdentifier,
                )
            )
        ) doReturn transferGroupId
        val appData = listOfNotNull(
            TransferAppData.TransferGroup(transferGroupId),
        )
        val isHighPriority = false
        val expected = pathsAndNames.map { (path, name) ->
            InsertPendingTransferRequest(
                transferType = TransferType.GENERAL_UPLOAD,
                nodeIdentifier = pendingTransferNodeIdentifier,
                uriPath = UriPath(path),
                appData = appData,
                isHighPriority = isHighPriority,
                fileName = name
            )
        }

        underTest(
            pathsAndNames,
            parentFolderId,
            isHighPriority,
        )

        verify(transferRepository).insertPendingTransfers(expected)
    }

}