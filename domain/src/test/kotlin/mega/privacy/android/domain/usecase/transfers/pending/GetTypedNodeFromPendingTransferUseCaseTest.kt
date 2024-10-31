package mega.privacy.android.domain.usecase.transfers.pending

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetTypedNodeFromPendingTransferUseCaseTest {
    private lateinit var underTest: GetTypedNodeFromPendingTransferUseCase

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val getPublicNodeFromSerializedDataUseCase =
        mock<GetPublicNodeFromSerializedDataUseCase>()
    private val nodeRepository = mock<NodeRepository>()
    private val mapNodeToPublicLinkUseCase = mock<MapNodeToPublicLinkUseCase>()

    @BeforeEach
    fun setup() {
        underTest = GetTypedNodeFromPendingTransferUseCase(
            getNodeByIdUseCase,
            getChatFileUseCase,
            getPublicNodeFromSerializedDataUseCase,
            nodeRepository,
            mapNodeToPublicLinkUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        getNodeByIdUseCase,
        getChatFileUseCase,
        getPublicNodeFromSerializedDataUseCase,
        nodeRepository,
        mapNodeToPublicLinkUseCase,
    )

    @Test
    fun `test that the correct cloud node from use case is returned when invoked with a CloudDriveNode`() =
        runTest {
            val nodeId = NodeId(53L)
            val nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(nodeId)
            val pendingTransfer = mock<PendingTransfer> {
                on { this.nodeIdentifier } doReturn nodeIdentifier
            }
            val expected = mock<DefaultTypedFileNode>()
            whenever(getNodeByIdUseCase(nodeId)) doReturn expected

            val actual = underTest(pendingTransfer)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that the correct chat file node from get chat file use case is returned when invoked with a ChatAttachment`() =
        runTest {
            val chatId = 53L
            val messageId = 94L
            val messageIndex = 0
            val nodeId = NodeId(589L)
            val nodeIdentifier =
                PendingTransferNodeIdentifier.ChatAttachment(
                    chatId,
                    messageId,
                    messageIndex,
                    nodeId
                )
            val pendingTransfer = mock<PendingTransfer> {
                on { this.nodeIdentifier } doReturn nodeIdentifier
            }
            val expected = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId, messageIndex)) doReturn expected

            val actual = underTest(pendingTransfer)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that the correct link node from get public node from serialized data use case is returned when invoked with a PublicLinkFile with not null serialized data`() =
        runTest {
            val nodeId = NodeId(53L)
            val serializedData = "serializedFoo"
            val nodeIdentifier =
                PendingTransferNodeIdentifier.PublicLinkFile(serializedData, nodeId)
            val pendingTransfer = mock<PendingTransfer> {
                on { this.nodeIdentifier } doReturn nodeIdentifier
            }
            val expected = mock<PublicLinkFile>()
            whenever(getPublicNodeFromSerializedDataUseCase(serializedData)) doReturn expected

            val actual = underTest(pendingTransfer)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that the correct typed file link node from node repository is returned when invoked with a PublicLinkFile with null serialized data`() =
        runTest {
            val nodeId = NodeId(53L)
            val serializedData = null
            val nodeIdentifier =
                PendingTransferNodeIdentifier.PublicLinkFile(serializedData, nodeId)
            val pendingTransfer = mock<PendingTransfer> {
                on { this.nodeIdentifier } doReturn nodeIdentifier
            }
            val expected = mock<PublicLinkFile>()
            val fileNode = mock<FileNode>()
            whenever(nodeRepository.getNodeByHandle(nodeId.longValue, true)) doReturn fileNode
            whenever(mapNodeToPublicLinkUseCase(fileNode, null)) doReturn expected

            val actual = underTest(pendingTransfer)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that the correct typed folder link node from node repository is returned when invoked with a PublicLinkFolder`() =
        runTest {
            val nodeId = NodeId(53L)
            val nodeIdentifier = PendingTransferNodeIdentifier.PublicLinkFolder(nodeId)
            val pendingTransfer = mock<PendingTransfer> {
                on { this.nodeIdentifier } doReturn nodeIdentifier
            }
            val expected = mock<PublicLinkFile>()
            val folderNode = mock<FolderNode>()
            whenever(nodeRepository.getNodeByHandle(nodeId.longValue, true)) doReturn folderNode
            whenever(mapNodeToPublicLinkUseCase(folderNode, null)) doReturn expected

            val actual = underTest(pendingTransfer)

            assertThat(actual).isEqualTo(expected)
        }
}