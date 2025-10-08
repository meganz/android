package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ActiveTransferActionGroupEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroupImpl
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferGroupEntityMapperTest {
    private lateinit var underTest: ActiveTransferGroupEntityMapper


    @BeforeAll
    fun setUp() {
        underTest = ActiveTransferGroupEntityMapper()
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    internal fun `test that entity is mapped correctly`(
        transferType: TransferType,
    ) = runTest {
        val activeTransferGroup =
            ActiveTransferActionGroupImpl(
                groupId = GROUP_ID,
                transferType = transferType,
                destination = DESTINATION,
                startTime = START_TIME,
                pendingTransferNodeId = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(NODE_ID)),
                selectedNames = listOf(NAME_1, NAME_2),
            )
        val expected =
            ActiveTransferActionGroupEntity(
                groupId = GROUP_ID,
                transferType = transferType,
                destination = DESTINATION,
                startTime = START_TIME,
                pendingTransferNodeId = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(NODE_ID)),
                selectedNames = listOf(NAME_1, NAME_2),
            )
        val actual = underTest(activeTransferGroup)
        assertThat(expected).isEqualTo(actual)
    }

    private companion object {
        const val GROUP_ID = 34
        const val DESTINATION = "destination"
        const val START_TIME = 94837594L
        const val NODE_ID = 4895L
        const val NAME_1 = "name1.txt"
        const val NAME_2 = "name2.jpg"
    }
}