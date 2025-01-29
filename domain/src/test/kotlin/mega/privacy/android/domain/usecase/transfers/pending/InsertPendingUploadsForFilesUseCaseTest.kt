package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InsertPendingUploadsForFilesUseCaseTest {
    private lateinit var underTest: InsertPendingUploadsForFilesUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = InsertPendingUploadsForFilesUseCase(transferRepository)
    }

    @BeforeEach
    fun cleanUp() {
        reset(transferRepository)
    }

    @Test
    fun `test that pending transfers are inserted with correct parameters`() = runTest {
        val pathsAndNames = (0..10).associate { "content://file$it" to "newName$it" }
        val parentFolderId = NodeId(242L)
        val appData = listOf(mock<TransferAppData.Geolocation>())
        val isHighPriority = false
        val expected = pathsAndNames.map { (path, name) ->
            InsertPendingTransferRequest(
                transferType = TransferType.GENERAL_UPLOAD,
                nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(parentFolderId),
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
            appData,
        )

        verify(transferRepository).insertPendingTransfers(expected)
    }

}