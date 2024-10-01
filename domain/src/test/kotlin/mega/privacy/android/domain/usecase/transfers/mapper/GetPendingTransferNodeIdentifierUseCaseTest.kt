package mega.privacy.android.domain.usecase.transfers.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPendingTransferNodeIdentifierUseCaseTest {
    private lateinit var underTest: GetPendingTransferNodeIdentifierUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetPendingTransferNodeIdentifierUseCase()
    }

    @Test
    fun `test that a ChatFile node returns a ChatAttachment Identifier`() = runTest {
        val chatId = 65L
        val messageId = 95L
        val messageIndex = 0
        val typedNode = mock<ChatDefaultFile> {
            on { this.chatId } doReturn chatId
            on { this.messageId } doReturn messageId
            on { this.messageIndex } doReturn messageIndex
        }
        val expected = PendingTransferNodeIdentifier.ChatAttachment(
            chatId,
            messageId,
            messageIndex,
        )

        val actual = underTest(typedNode)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that a PublicLinkFile node returns a PublicLinkFile Identifier`() = runTest {
        val nodeId = NodeId(78596L)
        val serializedData = "serializedData"
        val fileNode = mock<TypedFileNode> {
            on { this.serializedData } doReturn serializedData
        }
        val typedNode = mock<PublicLinkFile> {
            on { this.node } doReturn fileNode
            on { this.id } doReturn nodeId
        }
        val expected = PendingTransferNodeIdentifier.PublicLinkFile(
            serializedData,
            nodeId,
        )

        val actual = underTest(typedNode)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that a PublicLinkFolder node returns a PublicLinkFolder Identifier`() = runTest {
        val nodeId = NodeId(78916L)
        val typedNode = mock<PublicLinkFolder> {
            on { this.id } doReturn nodeId
        }
        val expected = PendingTransferNodeIdentifier.PublicLinkFolder(nodeId)

        val actual = underTest(typedNode)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that a TypedFileNode node returns a CloudDriveNode Identifier`() = runTest {
        val nodeId = NodeId(78696L)
        val typedNode = mock<TypedFileNode> {
            on { this.id } doReturn nodeId
        }
        val expected = PendingTransferNodeIdentifier.CloudDriveNode(nodeId)

        val actual = underTest(typedNode)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that a TypedFolderNode node returns a CloudDriveNode Identifier`() = runTest {
        val nodeId = NodeId(78696L)
        val typedNode = mock<TypedFolderNode> {
            on { this.id } doReturn nodeId
        }
        val expected = PendingTransferNodeIdentifier.CloudDriveNode(nodeId)

        val actual = underTest(typedNode)

        assertThat(actual).isEqualTo(expected)
    }
}