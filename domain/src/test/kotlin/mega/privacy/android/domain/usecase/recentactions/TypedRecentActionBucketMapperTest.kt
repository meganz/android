package mega.privacy.android.domain.usecase.recentactions

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.recentactions.NodeInfoForRecentActions
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypedRecentActionBucketMapperTest {

    private lateinit var underTest: TypedRecentActionBucketMapper

    private val recentActionsRepository = mock<RecentActionsRepository>()
    private val addNodeType = mock<AddNodeType>()
    private val areCredentialsVerifiedUseCase = mock<AreCredentialsVerifiedUseCase>()
    private val nodeRepository = mock<NodeRepository>()

    @BeforeEach
    fun setUp() {
        underTest = TypedRecentActionBucketMapper(
            recentActionsRepository = recentActionsRepository,
            addNodeType = addNodeType,
            areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
            nodeRepository = nodeRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            recentActionsRepository,
            addNodeType,
            areCredentialsVerifiedUseCase,
            nodeRepository,
        )
    }

    private fun createFileNode(id: Long = 1L, isNodeKeyDecrypted: Boolean = true): FileNode = mock {
        on { this.id } doReturn NodeId(id)
        on { this.isNodeKeyDecrypted } doReturn isNodeKeyDecrypted
    }

    private fun createFolderNode(): FolderNode = mock()

    private suspend fun setupDefaultMocks(
        fileNodes: List<FileNode>,
        nodeAccessPermission: AccessPermission = AccessPermission.OWNER,
        parentNodeInfo: NodeInfoForRecentActions? = null,
    ) {
        fileNodes.forEach { node ->
            val typedNode = DefaultTypedFileNode(node)
            whenever(addNodeType(node)).thenReturn(typedNode)
        }
        whenever(recentActionsRepository.getNodeInfo(any())).thenReturn(parentNodeInfo)
        whenever(nodeRepository.getNodeAccessPermission(any())).thenReturn(nodeAccessPermission)
    }

    private fun createBucket(
        identifier: String = "test-identifier",
        timestamp: Long = 1234567890L,
        dateTimestamp: Long = 1234567890L,
        userEmail: String = "test@example.com",
        parentNodeId: NodeId = NodeId(100L),
        isUpdate: Boolean = false,
        isMedia: Boolean = false,
        nodes: List<UnTypedNode> = listOf(createFileNode()),
    ): RecentActionBucketUnTyped = RecentActionBucketUnTyped(
        identifier = identifier,
        timestamp = timestamp,
        dateTimestamp = dateTimestamp,
        userEmail = userEmail,
        parentNodeId = parentNodeId,
        isUpdate = isUpdate,
        isMedia = isMedia,
        nodes = nodes,
    )

    private fun createParentNodeInfo(
        id: NodeId = NodeId(100L),
        name: String = "Parent Folder",
        isFolder: Boolean = true,
        isIncomingShare: Boolean = false,
        isOutgoingShare: Boolean = false,
        isPendingShare: Boolean = false,
        parentId: NodeId = NodeId(1L),
    ): NodeInfoForRecentActions = mock {
        on { this.id } doReturn id
        on { this.name } doReturn name
        on { this.isFolder } doReturn isFolder
        on { this.isIncomingShare } doReturn isIncomingShare
        on { this.isOutgoingShare } doReturn isOutgoingShare
        on { this.isPendingShare } doReturn isPendingShare
        on { this.parentId } doReturn parentId
    }

    @Test
    fun `test that mapper converts untyped buckets to typed buckets`() = runTest {
        val fileNode1 = createFileNode(id = 1L)
        val fileNode2 = createFileNode(id = 2L)
        val bucket = createBucket(nodes = listOf(fileNode1, fileNode2))
        val visibleContacts = mapOf("test@example.com" to "Test User")

        setupDefaultMocks(listOf(fileNode1, fileNode2))

        val result = underTest(
            buckets = listOf(bucket),
            visibleContacts = visibleContacts,
            currentUserEmail = "test@example.com",
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].identifier).isEqualTo("test-identifier")
        assertThat(result[0].timestamp).isEqualTo(1234567890L)
        assertThat(result[0].userEmail).isEqualTo("test@example.com")
        assertThat(result[0].userName).isEqualTo("Test User")
        assertThat(result[0].nodes).hasSize(2)
        assertThat(result[0].currentUserIsOwner).isTrue()
        assertThat(result[0].parentFolderSharesType).isEqualTo(RecentActionsSharesType.NONE)
    }

    @Test
    fun `test that mapper filters out buckets with no valid typed nodes`() = runTest {
        val folderNode = createFolderNode()
        val bucket = createBucket(nodes = listOf(folderNode))

        whenever(addNodeType(folderNode)).thenReturn(null) // Folder nodes are filtered out

        val result = underTest(
            buckets = listOf(bucket),
            visibleContacts = emptyMap(),
            currentUserEmail = null,
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that mapper uses email as userName when not in contacts`() = runTest {
        val fileNode = createFileNode()
        val bucket = createBucket(nodes = listOf(fileNode))

        setupDefaultMocks(listOf(fileNode))

        val result = underTest(
            buckets = listOf(bucket),
            visibleContacts = emptyMap(),
            currentUserEmail = null,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].userName).isEqualTo("test@example.com")
    }

    @Test
    fun `test that mapper determines currentUserIsOwner correctly`() = runTest {
        val fileNode = createFileNode()
        val bucket = createBucket(userEmail = "owner@example.com", nodes = listOf(fileNode))

        setupDefaultMocks(listOf(fileNode))

        val resultOwner = underTest(
            buckets = listOf(bucket),
            visibleContacts = emptyMap(),
            currentUserEmail = "owner@example.com",
        )

        val resultNotOwner = underTest(
            buckets = listOf(bucket),
            visibleContacts = emptyMap(),
            currentUserEmail = "other@example.com",
        )

        assertThat(resultOwner[0].currentUserIsOwner).isTrue()
        assertThat(resultNotOwner[0].currentUserIsOwner).isFalse()
    }

    @Test
    fun `test that mapper determines shares type correctly for incoming shares`() = runTest {
        val fileNode = createFileNode()
        val parentNodeInfo = createParentNodeInfo(isIncomingShare = true)
        val bucket = createBucket(userEmail = "other@example.com", nodes = listOf(fileNode))

        setupDefaultMocks(listOf(fileNode), nodeAccessPermission = AccessPermission.READ, parentNodeInfo = parentNodeInfo)

        val result = underTest(
            buckets = listOf(bucket),
            visibleContacts = emptyMap(),
            currentUserEmail = "current@example.com",
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].parentFolderSharesType).isEqualTo(RecentActionsSharesType.INCOMING_SHARES)
        assertThat(result[0].parentFolderName).isEqualTo("Parent Folder")
    }

    @Test
    fun `test that mapper determines shares type correctly for outgoing shares`() = runTest {
        val fileNode = createFileNode()
        val parentNodeInfo = createParentNodeInfo(isOutgoingShare = true)
        val bucket = createBucket(userEmail = "other@example.com", nodes = listOf(fileNode))

        setupDefaultMocks(listOf(fileNode), parentNodeInfo = parentNodeInfo)

        val result = underTest(
            buckets = listOf(bucket),
            visibleContacts = emptyMap(),
            currentUserEmail = "current@example.com",
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].parentFolderSharesType).isEqualTo(RecentActionsSharesType.OUTGOING_SHARES)
    }

    @Test
    fun `test that mapper verifies credentials and sets isKeyVerified correctly`() = runTest {
        val fileNode = createFileNode(isNodeKeyDecrypted = false)
        val bucket = createBucket(userEmail = "other@example.com", nodes = listOf(fileNode))

        setupDefaultMocks(listOf(fileNode))
        whenever(areCredentialsVerifiedUseCase(any())).thenReturn(true)

        val result = underTest(
            buckets = listOf(bucket),
            visibleContacts = emptyMap(),
            currentUserEmail = "current@example.com",
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].isKeyVerified).isTrue()
        assertThat(result[0].isNodeKeyDecrypted).isFalse()
        verify(areCredentialsVerifiedUseCase).invoke("other@example.com")
    }

    @Test
    fun `test that mapper processes multiple buckets correctly`() = runTest {
        val fileNode1 = createFileNode(id = 1L)
        val fileNode2 = createFileNode(id = 2L)
        val bucket1 = createBucket(
            identifier = "identifier-1",
            userEmail = "user1@example.com",
            parentNodeId = NodeId(100L),
            nodes = listOf(fileNode1)
        )
        val bucket2 = createBucket(
            identifier = "identifier-2",
            timestamp = 1234567900L,
            userEmail = "user2@example.com",
            parentNodeId = NodeId(200L),
            isUpdate = true,
            isMedia = true,
            nodes = listOf(fileNode2)
        )

        setupDefaultMocks(listOf(fileNode1, fileNode2))

        val result = underTest(
            buckets = listOf(bucket1, bucket2),
            visibleContacts = emptyMap(),
            currentUserEmail = null,
        )

        assertThat(result).hasSize(2)
        assertThat(result[0].identifier).isEqualTo("identifier-1")
        assertThat(result[1].identifier).isEqualTo("identifier-2")
        assertThat(result[0].isUpdate).isFalse()
        assertThat(result[1].isUpdate).isTrue()
        assertThat(result[0].isMedia).isFalse()
        assertThat(result[1].isMedia).isTrue()
    }
}

