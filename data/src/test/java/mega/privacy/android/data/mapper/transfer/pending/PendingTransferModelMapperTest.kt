package mega.privacy.android.data.mapper.transfer.pending

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PendingTransferModelMapperTest {
    private lateinit var underTest: PendingTransferModelMapper

    private val appDataMapper = mock<TransferAppDataMapper>()

    private val pendingTransferEntity = PendingTransferEntity(
        pendingTransferId = 123456L,
        transferTag = 789012,
        transferType = TransferType.DOWNLOAD,
        nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(11111L)),
        path = "file/path",
        appData = "appData",
        isHighPriority = true,
        scanningFoldersData = PendingTransferEntity.ScanningFoldersDataEntity(
            stage = TransferStage.STAGE_SCANNING,
            fileCount = 6543,
            folderCount = 903456,
            createdFolderCount = 9,
        ),
        startedFiles = 0,
        alreadyTransferred = 0,
        state = PendingTransferState.NotSentToSdk,
    )

    @BeforeAll
    fun setUp() {
        underTest = PendingTransferModelMapper(
            appDataMapper,
        )
    }

    fun resetMocks() = reset(appDataMapper)

    @Test
    fun `test that a pending transfer is mapped into a pending transfer entity`() = runTest {
        val appData = TransferAppData.ChatUpload(454L)
        whenever(appDataMapper(pendingTransferEntity.appData ?: "")) doReturn listOf(appData)
        val pendingTransfer = underTest(pendingTransferEntity)

        assertAll(
            "Grouped Assertions of ${PendingTransferEntity::class.simpleName}",
            { assertThat(pendingTransfer.pendingTransferId).isEqualTo(pendingTransferEntity.pendingTransferId) },
            { assertThat(pendingTransfer.transferTag).isEqualTo(pendingTransferEntity.transferTag) },
            { assertThat(pendingTransfer.transferType).isEqualTo(pendingTransferEntity.transferType) },
            { assertThat(pendingTransfer.nodeIdentifier).isEqualTo(pendingTransferEntity.nodeIdentifier) },
            { assertThat(pendingTransfer.path).isEqualTo(pendingTransferEntity.path) },
            { assertThat(pendingTransfer.appData).isEqualTo(appData) },
            { assertThat(pendingTransfer.isHighPriority).isEqualTo(pendingTransferEntity.isHighPriority) },
            { assertThat(pendingTransfer.scanningFoldersData.stage).isEqualTo(pendingTransferEntity.scanningFoldersData.stage) },
            {
                assertThat(pendingTransfer.scanningFoldersData.fileCount)
                    .isEqualTo(pendingTransferEntity.scanningFoldersData.fileCount)
            },
            {
                assertThat(pendingTransfer.scanningFoldersData.folderCount)
                    .isEqualTo(pendingTransferEntity.scanningFoldersData.folderCount)
            },
            {
                assertThat(pendingTransfer.scanningFoldersData.createdFolderCount)
                    .isEqualTo(pendingTransferEntity.scanningFoldersData.createdFolderCount)
            },
            { assertThat(pendingTransfer.startedFiles).isEqualTo(pendingTransferEntity.startedFiles) },
            { assertThat(pendingTransfer.alreadyTransferred).isEqualTo(pendingTransferEntity.alreadyTransferred) },
            { assertThat(pendingTransfer.state).isEqualTo(pendingTransferEntity.state) },
        )
    }
}