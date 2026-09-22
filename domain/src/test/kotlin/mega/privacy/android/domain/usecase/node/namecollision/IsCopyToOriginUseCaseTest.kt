package mega.privacy.android.domain.usecase.node.namecollision

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.node.GetNodeByFingerprintAndParentNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsCopyToOriginUseCaseTest {
    private lateinit var underTest: IsCopyToOriginUseCase

    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val getNodeByFingerprintAndParentNodeUseCase =
        mock<GetNodeByFingerprintAndParentNodeUseCase>()
    private val nodeRepository = mock<NodeRepository>()

    private val nodeHandle = 1L
    private val parentHandle = 2L
    private val serializedData = "serialized"
    private val fingerprint = "fingerprint"

    @BeforeAll
    fun setUp() {
        underTest = IsCopyToOriginUseCase(
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            getNodeByFingerprintAndParentNodeUseCase = getNodeByFingerprintAndParentNodeUseCase,
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getNodeByHandleUseCase, getNodeByFingerprintAndParentNodeUseCase, nodeRepository)
    }

    @Test
    fun `test that invoke returns false when the default collision is not a copy`() = runTest {
        val collision = defaultCollision(type = NodeNameCollisionType.MOVE)

        assertThat(underTest(collision)).isFalse()
        verifyNoInteractions(getNodeByHandleUseCase)
    }

    @Test
    fun `test that invoke returns false when the collision is a chat collision`() = runTest {
        val collision = NodeNameCollision.Chat(
            collisionHandle = 3L,
            nodeHandle = nodeHandle,
            name = "name",
            size = 0L,
            childFolderCount = 0,
            childFileCount = 0,
            lastModified = 0L,
            parentHandle = parentHandle,
            isFile = true,
            chatId = 4L,
            messageId = 5L,
        )

        assertThat(underTest(collision)).isFalse()
        verifyNoInteractions(getNodeByHandleUseCase, getNodeByFingerprintAndParentNodeUseCase)
    }

    @Test
    fun `test that invoke returns true when the default node is found in the target parent`() =
        runTest {
            val node = nodeIn(parentHandle)
            whenever(getNodeByHandleUseCase(nodeHandle, true)).thenReturn(node)

            assertThat(underTest(defaultCollision())).isTrue()
        }

    @Test
    fun `test that invoke returns false when the default node is found in another parent`() =
        runTest {
            val node = nodeIn(99L)
            whenever(getNodeByHandleUseCase(nodeHandle, true)).thenReturn(node)

            assertThat(underTest(defaultCollision())).isFalse()
        }

    @Test
    fun `test that invoke returns false when the default node is not found by handle`() = runTest {
        whenever(getNodeByHandleUseCase(nodeHandle, true)).thenReturn(null)

        assertThat(underTest(defaultCollision())).isFalse()
    }

    @Test
    fun `test that invoke returns false when the default node lookup fails`() = runTest {
        whenever(getNodeByHandleUseCase(nodeHandle, true)).thenThrow(RuntimeException("error"))

        assertThat(underTest(defaultCollision())).isFalse()
    }

    @Test
    fun `test that invoke returns false when the file link node cannot be deserialized`() =
        runTest {
            whenever(nodeRepository.getNodeFromSerializedData(serializedData)).thenReturn(null)

            assertThat(underTest(fileLinkCollision())).isFalse()
            verifyNoInteractions(getNodeByFingerprintAndParentNodeUseCase)
        }

    @Test
    fun `test that invoke returns false when the file link node is a folder`() = runTest {
        val folderNode = mock<FolderNode>()
        whenever(nodeRepository.getNodeFromSerializedData(serializedData)).thenReturn(folderNode)

        assertThat(underTest(fileLinkCollision())).isFalse()
        verifyNoInteractions(getNodeByFingerprintAndParentNodeUseCase)
    }

    @Test
    fun `test that invoke returns false when the file link node has no fingerprint`() = runTest {
        val fileNode = fileNodeWithFingerprint(null)
        whenever(nodeRepository.getNodeFromSerializedData(serializedData)).thenReturn(fileNode)

        assertThat(underTest(fileLinkCollision())).isFalse()
        verifyNoInteractions(getNodeByFingerprintAndParentNodeUseCase)
    }

    @Test
    fun `test that invoke returns true when the file link fingerprint matches a node in the target parent`() =
        runTest {
            val fileNode = fileNodeWithFingerprint(fingerprint)
            val existingNode = nodeIn(parentHandle)
            whenever(nodeRepository.getNodeFromSerializedData(serializedData)).thenReturn(fileNode)
            whenever(getNodeByFingerprintAndParentNodeUseCase(fingerprint, NodeId(parentHandle)))
                .thenReturn(existingNode)

            assertThat(underTest(fileLinkCollision())).isTrue()
        }

    @Test
    fun `test that invoke returns false when the file link fingerprint matches a node in another parent`() =
        runTest {
            val fileNode = fileNodeWithFingerprint(fingerprint)
            val existingNode = nodeIn(99L)
            whenever(nodeRepository.getNodeFromSerializedData(serializedData)).thenReturn(fileNode)
            whenever(getNodeByFingerprintAndParentNodeUseCase(fingerprint, NodeId(parentHandle)))
                .thenReturn(existingNode)

            assertThat(underTest(fileLinkCollision())).isFalse()
        }

    @Test
    fun `test that invoke returns false when the file link fingerprint matches no node`() =
        runTest {
            val fileNode = fileNodeWithFingerprint(fingerprint)
            whenever(nodeRepository.getNodeFromSerializedData(serializedData)).thenReturn(fileNode)
            whenever(getNodeByFingerprintAndParentNodeUseCase(fingerprint, NodeId(parentHandle)))
                .thenReturn(null)

            assertThat(underTest(fileLinkCollision())).isFalse()
        }

    @Test
    fun `test that invoke throws when the file link lookup fails`() = runTest {
        whenever(nodeRepository.getNodeFromSerializedData(any()))
            .thenThrow(RuntimeException("error"))

        assertThrows<RuntimeException> { underTest(fileLinkCollision()) }
    }

    private fun defaultCollision(
        type: NodeNameCollisionType = NodeNameCollisionType.COPY,
    ) = NodeNameCollision.Default(
        collisionHandle = 3L,
        nodeHandle = nodeHandle,
        name = "name",
        size = 0L,
        childFolderCount = 0,
        childFileCount = 0,
        lastModified = 0L,
        parentHandle = parentHandle,
        isFile = true,
        type = type,
    )

    private fun fileLinkCollision() = NodeNameCollision.FileLink(
        collisionHandle = 3L,
        nodeHandle = nodeHandle,
        name = "name",
        size = 0L,
        childFolderCount = 0,
        childFileCount = 0,
        lastModified = 0L,
        parentHandle = parentHandle,
        isFile = true,
        serializedData = serializedData,
    )

    private fun nodeIn(parent: Long) = mock<FileNode> {
        on { parentId } doReturn NodeId(parent)
    }

    private fun fileNodeWithFingerprint(value: String?) = mock<FileNode> {
        on { fingerprint } doReturn value
    }
}
