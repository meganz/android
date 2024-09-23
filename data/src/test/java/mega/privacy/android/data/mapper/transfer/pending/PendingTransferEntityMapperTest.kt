package mega.privacy.android.data.mapper.transfer.pending

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
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
internal class PendingTransferEntityMapperTest {
    private lateinit var underTest: PendingTransferEntityMapper

    private val appDataMapper = mock<TransferAppDataStringMapper>()

    private val pendingTransfer = PendingTransfer(
        pendingTransferId = 123456L,
        transferTag = 789012,
        transferType = TransferType.DOWNLOAD,
        nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(11111L)),
        path = "file/path",
        appData = TransferAppData.ChatUpload(454L),
        isHighPriority = true,
        scanningFoldersData = PendingTransfer.ScanningFoldersData(
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
        underTest = PendingTransferEntityMapper(
            appDataMapper,
        )
    }

    fun resetMocks() = reset(appDataMapper)

    @Test
    fun `test that a pending transfer is mapped into a pending transfer entity`() = runTest {
        val appDataString = "appData"
        whenever(appDataMapper(listOfNotNull(pendingTransfer.appData))) doReturn appDataString
        val pendingTransferEntity = underTest(pendingTransfer)

        assertAll(
            "Grouped Assertions of ${PendingTransferEntity::class.simpleName}",
            { assertThat(pendingTransferEntity.pendingTransferId).isEqualTo(pendingTransfer.pendingTransferId) },
            { assertThat(pendingTransferEntity.transferTag).isEqualTo(pendingTransfer.transferTag) },
            { assertThat(pendingTransferEntity.transferType).isEqualTo(pendingTransfer.transferType) },
            { assertThat(pendingTransferEntity.nodeIdentifier).isEqualTo(pendingTransfer.nodeIdentifier) },
            { assertThat(pendingTransferEntity.path).isEqualTo(pendingTransfer.path) },
            { assertThat(pendingTransferEntity.appData).isEqualTo(appDataString) },
            { assertThat(pendingTransferEntity.isHighPriority).isEqualTo(pendingTransfer.isHighPriority) },
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
            { assertThat(pendingTransferEntity.startedFiles).isEqualTo(pendingTransfer.startedFiles) },
            { assertThat(pendingTransferEntity.alreadyTransferred).isEqualTo(pendingTransfer.alreadyTransferred) },
            { assertThat(pendingTransferEntity.state).isEqualTo(pendingTransfer.state) },
        )
    }
}