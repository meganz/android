package mega.privacy.android.data.mapper.transfer.pending

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
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
internal class InsertPendingTransferRequestMapperTest {
    private lateinit var underTest: InsertPendingTransferRequestMapper

    private val appDataMapper = mock<TransferAppDataStringMapper>()

    private val insertRequest = InsertPendingTransferRequest(
        transferType = TransferType.DOWNLOAD,
        nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(11111L)),
        path = "file/path",
        appData = TransferAppData.ChatUpload(454L),
        isHighPriority = true,
    )

    @BeforeAll
    fun setUp() {
        underTest = InsertPendingTransferRequestMapper(
            appDataMapper,
        )
    }

    fun resetMocks() = reset(appDataMapper)

    @Test
    fun `test that a pending transfer is mapped into a pending transfer entity`() = runTest {
        val appDataString = "appData"
        whenever(appDataMapper(listOfNotNull(insertRequest.appData))) doReturn appDataString
        val pendingTransferEntity = underTest(insertRequest)

        assertAll(
            "Grouped Assertions of ${PendingTransferEntity::class.simpleName}",
            { assertThat(pendingTransferEntity.pendingTransferId).isNull() },
            { assertThat(pendingTransferEntity.transferTag).isNull() },
            { assertThat(pendingTransferEntity.transferType).isEqualTo(insertRequest.transferType) },
            { assertThat(pendingTransferEntity.nodeIdentifier).isEqualTo(insertRequest.nodeIdentifier) },
            { assertThat(pendingTransferEntity.path).isEqualTo(insertRequest.path) },
            { assertThat(pendingTransferEntity.appData).isEqualTo(appDataString) },
            { assertThat(pendingTransferEntity.isHighPriority).isEqualTo(insertRequest.isHighPriority) },
            { assertThat(pendingTransferEntity.scanningFoldersData).isEqualTo(PendingTransferEntity.ScanningFoldersDataEntity()) },
            { assertThat(pendingTransferEntity.startedFiles).isEqualTo(0) },
            { assertThat(pendingTransferEntity.alreadyTransferred).isEqualTo(0) },
            { assertThat(pendingTransferEntity.state).isEqualTo(PendingTransferState.NotSentToSdk) },
        )
    }
}