package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.chat.GetChatFilesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CopyCollidedNodesUseCaseTest {
    private lateinit var underTest: CopyCollidedNodesUseCase

    private val getChatFilesUseCase: GetChatFilesUseCase = mock()
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase = mock()
    private val nodeRepository: NodeRepository = mock()
    private val accountRepository: AccountRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = CopyCollidedNodesUseCase(
            getChatFilesUseCase = getChatFilesUseCase,
            copyTypedNodeUseCase = copyTypedNodeUseCase,
            nodeRepository = nodeRepository,
            accountRepository = accountRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getChatFilesUseCase, copyTypedNodeUseCase, nodeRepository, accountRepository)
    }

    private fun defaultCollision(nodeHandle: Long = 1L) = mock<NodeNameCollision.Default> {
        on { this.nodeHandle } doReturn nodeHandle
        on { parentHandle } doReturn 2L
        on { renameName } doReturn "new name"
    }

    private fun chatCollision() = mock<NodeNameCollision.Chat> {
        on { nodeHandle } doReturn 1L
        on { parentHandle } doReturn 2L
        on { renameName } doReturn "new name"
        on { chatId } doReturn 1L
        on { messageId } doReturn 1L
    }

    private fun fileLinkCollision() = mock<NodeNameCollision.FileLink> {
        on { nodeHandle } doReturn 1L
        on { parentHandle } doReturn 2L
        on { renameName } doReturn "new name"
        on { serializedData } doReturn "data"
    }

    @Test
    fun `test that invoke copies a node in the account tree by handle`() = runTest {
        val collision = defaultCollision()
        whenever(nodeRepository.copyNode(NodeId(1L), NodeId(2L), "new name")) doReturn NodeId(3L)

        val result = underTest(listOf(collision), rename = true)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(0)
    }

    @Test
    fun `test that invoke counts an error when copying a node fails`() = runTest {
        val collision = defaultCollision()
        whenever(nodeRepository.copyNode(any<NodeId>(), any(), anyOrNull()))
            .thenThrow(RuntimeException::class.java)

        val result = underTest(listOf(collision), rename = true)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(1)
    }

    @Test
    fun `test that invoke copies a chat attachment through the chat file`() = runTest {
        val chatFile = mock<ChatDefaultFile> { on { id } doReturn NodeId(1L) }
        val collision = chatCollision()
        whenever(getChatFilesUseCase(1L, 1L)) doReturn listOf(chatFile)
        whenever(copyTypedNodeUseCase(chatFile, NodeId(2L), null)) doReturn NodeId(2L)

        val result = underTest(listOf(collision), rename = false)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(0)
    }

    @Test
    fun `test that invoke counts an error when the chat file is not found`() = runTest {
        val chatFile = mock<ChatDefaultFile> { on { id } doReturn NodeId(9L) }
        val collision = chatCollision()
        whenever(getChatFilesUseCase(1L, 1L)) doReturn listOf(chatFile)

        val result = underTest(listOf(collision), rename = true)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(1)
        verify(copyTypedNodeUseCase, never()).invoke(any(), any(), anyOrNull())
    }

    @Test
    fun `test that invoke counts an error when the chat files cannot be fetched`() = runTest {
        val collision = chatCollision()
        whenever(getChatFilesUseCase(any(), any())).thenThrow(RuntimeException::class.java)

        val result = underTest(listOf(collision), rename = false)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(1)
    }

    @Test
    fun `test that invoke copies a file link node from its serialized data`() = runTest {
        val publicNode = mock<FileNode>()
        val collision = fileLinkCollision()
        whenever(nodeRepository.getNodeFromSerializedData("data")) doReturn publicNode
        whenever(nodeRepository.copyPublicNode(publicNode, NodeId(2L), "new name")) doReturn NodeId(3L)

        val result = underTest(listOf(collision), rename = true)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(0)
        verify(nodeRepository, never()).copyNode(any<NodeId>(), any(), anyOrNull())
    }

    @Test
    fun `test that invoke counts an error when the file link node cannot be deserialized`() =
        runTest {
            val collision = fileLinkCollision()
            whenever(nodeRepository.getNodeFromSerializedData("data")) doReturn null

            val result = underTest(listOf(collision), rename = false)

            assertThat(result.count).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
            verify(nodeRepository, never()).copyPublicNode(any(), any(), anyOrNull())
        }

    @Test
    fun `test that invoke throws when copying a node hits a foreign node error`() = runTest {
        whenever(nodeRepository.copyNode(any<NodeId>(), any(), anyOrNull()))
            .thenThrow(ForeignNodeException::class.java)

        assertThrows<ForeignNodeException> { underTest(listOf(defaultCollision()), false) }
    }

    @Test
    fun `test that invoke throws when copying a node runs out of quota`() = runTest {
        whenever(nodeRepository.copyNode(any<NodeId>(), any(), anyOrNull()))
            .thenAnswer { throw NotEnoughQuotaMegaException(1, "") }

        assertThrows<NotEnoughQuotaMegaException> { underTest(listOf(defaultCollision()), false) }
    }

    @Test
    fun `test that invoke throws when copying a node exceeds the quota`() = runTest {
        whenever(nodeRepository.copyNode(any<NodeId>(), any(), anyOrNull()))
            .thenAnswer { throw QuotaExceededMegaException(1, "") }

        assertThrows<QuotaExceededMegaException> { underTest(listOf(defaultCollision()), false) }
    }

    @Test
    fun `test that invoke throws when copying a file link node exceeds the quota`() = runTest {
        val publicNode = mock<FileNode>()
        whenever(nodeRepository.getNodeFromSerializedData("data")) doReturn publicNode
        whenever(nodeRepository.copyPublicNode(publicNode, NodeId(2L), null))
            .thenAnswer { throw QuotaExceededMegaException(1, "") }

        assertThrows<QuotaExceededMegaException> { underTest(listOf(fileLinkCollision()), false) }
    }

    @Test
    fun `test that invoke counts each node when some of them fail to copy`() = runTest {
        val copied = defaultCollision(nodeHandle = 1L)
        val failed = defaultCollision(nodeHandle = 2L)
        whenever(nodeRepository.copyNode(NodeId(1L), NodeId(2L), null)) doReturn NodeId(3L)
        whenever(nodeRepository.copyNode(NodeId(2L), NodeId(2L), null))
            .thenThrow(SecurityException::class.java)

        val result = underTest(listOf(copied, failed), rename = false)

        assertThat(result.count).isEqualTo(2)
        assertThat(result.successCount).isEqualTo(1)
        assertThat(result.errorCount).isEqualTo(1)
    }

    @Test
    fun `test that invoke resets the account details timestamp when at least one node is copied`() =
        runTest {
            whenever(nodeRepository.copyNode(NodeId(1L), NodeId(2L), "new name")) doReturn NodeId(3L)

            underTest(listOf(defaultCollision()), rename = true)

            verify(accountRepository).resetAccountDetailsTimeStamp()
        }

    @Test
    fun `test that invoke does not reset the account details timestamp when no node is copied`() =
        runTest {
            whenever(nodeRepository.copyNode(any<NodeId>(), any(), anyOrNull()))
                .thenThrow(RuntimeException::class.java)

            val result = underTest(listOf(defaultCollision(1L), defaultCollision(2L)), rename = true)

            assertThat(result.errorCount).isEqualTo(2)
            verify(accountRepository, never()).resetAccountDetailsTimeStamp()
        }
}
