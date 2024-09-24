package mega.privacy.android.domain.usecase.recentactions

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.recentactions.NodeInfoForRecentActions
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
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
class GetRecentActionsUseCaseTest {

    private lateinit var underTest: GetRecentActionsUseCase

    private val recentActionsRepository = mock<RecentActionsRepository>()
    private val addNodeType = mock<AddNodeType>()
    private val contactsRepository = mock<ContactsRepository>()
    private val nodeRepository = mock<NodeRepository>()
    private val getCurrentUserEmail = mock<GetCurrentUserEmail>()
    private val areCredentialsVerifiedUseCase = mock<AreCredentialsVerifiedUseCase>()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val dummyNode1 = mock<FileNode> {
        on { id } doReturn NodeId(123L)
        on { isNodeKeyDecrypted }.thenReturn(true)
    }
    private val dummyRecentActionBucketUnTyped = RecentActionBucketUnTyped(
        timestamp = 0L,
        userEmail = "aaa@aaa.com",
        parentNodeId = NodeId(321L),
        isUpdate = false,
        isMedia = false,
        nodes = listOf(dummyNode1)
    )

    @BeforeEach
    fun setUp() {
        commonStub()
        underTest = GetRecentActionsUseCase(
            recentActionsRepository = recentActionsRepository,
            addNodeType = addNodeType,
            contactsRepository = contactsRepository,
            nodeRepository = nodeRepository,
            getCurrentUserEmail = getCurrentUserEmail,
            areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
            coroutineDispatcher = ioDispatcher
        )
    }

    private fun commonStub() = runTest {
        whenever(contactsRepository.getAllContactsName()).thenReturn(emptyMap())
        whenever(nodeRepository.getNodeAccessPermission(NodeId(any()))).thenReturn(AccessPermission.OWNER)
        whenever(addNodeType(dummyNode1)).thenReturn(DefaultTypedFileNode(dummyNode1))
        whenever(areCredentialsVerifiedUseCase(any())).thenReturn(true)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            recentActionsRepository,
            addNodeType,
            nodeRepository,
            getCurrentUserEmail,
            contactsRepository,
            areCredentialsVerifiedUseCase,
        )
    }

    @Test
    fun `test that recentActionsRepository getRecentActions is invoked`() = runBlockingTest {
        whenever(recentActionsRepository.getRecentActions(any())).thenReturn(emptyList())
        underTest(false)
        verify(recentActionsRepository).getRecentActions(false)
    }

    @Test
    fun `test that for each recent action retrieved, add node type to each node inside the recent action`() =
        runTest {
            val nodes = (0..3).map {
                val mockFile = mock<FileNode> {
                    on { id } doReturn NodeId(12)
                }
                whenever(addNodeType(mockFile)).thenReturn(DefaultTypedFileNode(mockFile))
                mockFile
            }
            val list = listOf(
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "aaa@aaa.com",
                    parentNodeId = NodeId(0L),
                    isUpdate = false,
                    isMedia = false,
                    nodes = listOf(nodes[0], nodes[1])
                ),
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "aaa@aaa.com",
                    parentNodeId = NodeId(0L),
                    isUpdate = false,
                    isMedia = false,
                    nodes = listOf(nodes[2], nodes[3])
                ),
            )
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(list)

            underTest(false)

            (0..3).forEach {
                verify(addNodeType).invoke(nodes[it])
            }
        }

    @Test
    fun `test that only instance of TypedFileNode is returned when node type is added`() =
        runTest {
            val node1 = mock<FileNode>()
            val node2 = mock<FolderNode>()

            val list = listOf(
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "aaa@aaa.com",
                    parentNodeId = NodeId(0L),
                    isUpdate = false,
                    isMedia = false,
                    nodes = listOf(node1, node2)
                ),
            )
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(list)
            whenever(addNodeType(node1)).thenReturn(mock<TypedFileNode>())
            whenever(addNodeType(node2)).thenReturn(mock<TypedFolderNode>())

            val result = underTest(false)

            assertThat(result[0].nodes.size).isEqualTo(1)
        }

    @Test
    fun `test that areCredentialsVerifiedUseCase is invoked when isNodeKeyDecrypted is false`() =
        runTest {
            val file1 = mock<FileNode> {
                on { isNodeKeyDecrypted }.thenReturn(false)
            }
            val file2 = mock<FileNode> {
                on { isNodeKeyDecrypted }.thenReturn(true)
            }
            val list = listOf(
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "aaa@aaa.com",
                    parentNodeId = NodeId(0L),
                    isUpdate = false,
                    isMedia = false,
                    nodes = listOf(file1)
                ),
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "bbb@bbb.com",
                    parentNodeId = NodeId(0L),
                    isUpdate = false,
                    isMedia = false,
                    nodes = listOf(file2)
                ),
            )
            whenever(addNodeType(file1)).thenReturn(DefaultTypedFileNode(file1))
            whenever(addNodeType(file2)).thenReturn(DefaultTypedFileNode(file2))
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(list)

            underTest(false)

            verify(areCredentialsVerifiedUseCase).invoke(any())
        }

    @Test
    fun `test that isCurrentUserOwner is invoked for each RecentActionBucket`() = runTest {
        whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
            listOf(dummyRecentActionBucketUnTyped)
        )

        underTest(false)

        verify(getCurrentUserEmail).invoke(false)
    }

    @Test
    fun `test that isCurrentUserOwner is false when email doesn't match`() = runTest {
        val userAccount = mock<UserAccount> {
            on { email }.thenReturn("ccc@gmail.com")
        }
        whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
        whenever(getCurrentUserEmail(false)).thenReturn("ccc@gmail.com")
        whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
            listOf(dummyRecentActionBucketUnTyped)
        )

        val result = underTest(false)

        assertThat(result[0].currentUserIsOwner).isFalse()
        verify(getCurrentUserEmail).invoke(false)
    }


    @Test
    fun `test that buckets are filtered when it has no nodes`() =
        runTest {
            val list = listOf(
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "aaa@aaa.com",
                    parentNodeId = NodeId(0L),
                    isUpdate = false,
                    isMedia = false,
                    nodes = emptyList()
                ),
                RecentActionBucketUnTyped(
                    timestamp = 0L,
                    userEmail = "bbb@bbb.com",
                    parentNodeId = NodeId(0L),
                    isUpdate = false,
                    isMedia = false,
                    nodes = emptyList()
                ),
            )
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(list)

            val result = underTest(false)

            assertThat(result).isEmpty()
        }

    @Test
    fun `test that the recent action user name item is populated with the fullName if retrieved from email`() =
        runTest {
            val expected = "FirstName LastName"
            val userEmail = "aaa@aaa.com"
            val contactsMap = mapOf(userEmail to expected)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getCurrentUserEmail(false)).thenReturn(userEmail)
            whenever(contactsRepository.getAllContactsName()).thenReturn(contactsMap)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].userName).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action user name item is populated with email if name could not be retrieved from email`() =
        runTest {
            val userEmail = "aaa@aaa.com"
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getCurrentUserEmail(false)).thenReturn(userEmail)
            whenever(contactsRepository.getAllContactsName()).thenReturn(emptyMap())
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].userName).isEqualTo(userEmail)
        }

    @Test
    fun `test that the recent action parent folder name item is set to empty string if not retrieved from the parent node`() =
        runTest {
            val parentNode = mock<NodeInfoForRecentActions> {
                on { parentId }.thenReturn(NodeId(1L))
            }
            val expected = ""
            whenever(recentActionsRepository.getNodeInfo(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(recentActionsRepository.getNodeInfo(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].parentFolderName).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action parent folder name item is set if retrieved from the parent node`() =
        runTest {
            val expected = "Cloud drive"
            val parentNode = mock<NodeInfoForRecentActions> {
                on { parentId }.thenReturn(NodeId(1L))
                on { name }.thenReturn(expected)
            }
            whenever(recentActionsRepository.getNodeInfo(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(recentActionsRepository.getNodeInfo(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].parentFolderName).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to INCOMING_SHARES if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.INCOMING_SHARES
            val parentNode = mock<NodeInfoForRecentActions> {
                on { id }.thenReturn(NodeId(321L))
                on { parentId }.thenReturn(NodeId(1L))
                on { isFolder }.thenReturn(true)
                on { isIncomingShare }.thenReturn(true)
            }
            whenever(getCurrentUserEmail(false)).thenReturn("aaa@aaa.com")
            whenever(recentActionsRepository.getNodeInfo(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(recentActionsRepository.getNodeInfo(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to OUTGOING_SHARES if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.OUTGOING_SHARES
            val parentNode = mock<NodeInfoForRecentActions> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isOutgoingShare }.thenReturn(true)
            }
            whenever(recentActionsRepository.getNodeInfo(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(recentActionsRepository.getNodeInfo(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to PENDING_OUTGOING_SHARES if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.PENDING_OUTGOING_SHARES
            val parentNode = mock<NodeInfoForRecentActions> {
                on { isFolder }.thenReturn(true)
                on { parentId }.thenReturn(NodeId(1L))
                on { isPendingShare }.thenReturn(true)
            }
            whenever(getCurrentUserEmail(false)).thenReturn("aaa@aaa.com")
            whenever(nodeRepository.getNodeAccessPermission(NodeId(123L))).thenReturn(
                AccessPermission.OWNER
            )
            whenever(recentActionsRepository.getNodeInfo(NodeId(321L))).thenReturn(parentNode)
            whenever(recentActionsRepository.getNodeInfo(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to NONE if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.NONE
            val parentNode = mock<NodeInfoForRecentActions> {
                on { isFolder }.thenReturn(true)
                on { parentId }.thenReturn(NodeId(1L))
                on { isIncomingShare }.thenReturn(false)
                on { isOutgoingShare }.thenReturn(false)
                on { isPendingShare }.thenReturn(false)
            }
            whenever(getCurrentUserEmail(false)).thenReturn("aaa@aaa.com")
            whenever(recentActionsRepository.getNodeInfo(NodeId(321L))).thenReturn(parentNode)
            whenever(recentActionsRepository.getNodeInfo(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to OUTGOING_SHARES when current user is not owner but has owner access`() =
        runTest {
            val expected = RecentActionsSharesType.OUTGOING_SHARES
            val parentNode = mock<NodeInfoForRecentActions> {
                on { isFolder }.thenReturn(true)
                on { parentId }.thenReturn(NodeId(1L))
                on { isIncomingShare }.thenReturn(false)
                on { isOutgoingShare }.thenReturn(false)
                on { isPendingShare }.thenReturn(false)
            }
            whenever(getCurrentUserEmail(false)).thenReturn("bbb@aaa.com")
            whenever(nodeRepository.getNodeAccessPermission(NodeId(123L))).thenReturn(
                AccessPermission.OWNER
            )
            whenever(recentActionsRepository.getNodeInfo(NodeId(321L))).thenReturn(parentNode)
            whenever(recentActionsRepository.getNodeInfo(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to INCOMING_SHARES when current user is not owner and doesn't have owner access`() =
        runTest {
            val expected = RecentActionsSharesType.INCOMING_SHARES
            val parentNode = mock<NodeInfoForRecentActions> {
                on { isFolder }.thenReturn(true)
                on { parentId }.thenReturn(NodeId(1L))
                on { isIncomingShare }.thenReturn(false)
                on { isOutgoingShare }.thenReturn(false)
                on { isPendingShare }.thenReturn(false)
            }
            whenever(getCurrentUserEmail(false)).thenReturn("bbb@aaa.com")
            whenever(nodeRepository.getNodeAccessPermission(NodeId(123L))).thenReturn(
                AccessPermission.READ
            )
            whenever(recentActionsRepository.getNodeInfo(NodeId(321L))).thenReturn(parentNode)
            whenever(recentActionsRepository.getNodeInfo(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions(any())).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest(false)

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }
}