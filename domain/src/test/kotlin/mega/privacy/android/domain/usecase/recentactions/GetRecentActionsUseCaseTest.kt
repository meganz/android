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
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
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
    private val getVisibleContactsUseCase = mock<GetVisibleContactsUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getAccountDetailsUseCase = mock<GetAccountDetailsUseCase>()
    private val areCredentialsVerifiedUseCase = mock<AreCredentialsVerifiedUseCase>()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val dummyNode1 = mock<FileNode> {
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
            getVisibleContactsUseCase = getVisibleContactsUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
            coroutineDispatcher = ioDispatcher
        )
    }

    private fun commonStub() = runTest {
        whenever(getVisibleContactsUseCase()).thenReturn(emptyList())
        whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(mock())
        whenever(getAccountDetailsUseCase(any())).thenReturn(mock())
        whenever(areCredentialsVerifiedUseCase(any())).thenReturn(true)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            recentActionsRepository,
            addNodeType,
            getVisibleContactsUseCase,
            getNodeByIdUseCase,
            getAccountDetailsUseCase,
            areCredentialsVerifiedUseCase,
        )
    }

    @Test
    fun `test that recentActionsRepository getRecentActions is invoked`() = runBlockingTest {
        whenever(recentActionsRepository.getRecentActions()).thenReturn(emptyList())
        underTest()
        verify(recentActionsRepository).getRecentActions()
    }

    @Test
    fun `test that for each recent action retrieved, add node type to each node inside the recent action`() =
        runTest {
            val nodes = (0..3).map {
                mock<FileNode>()
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
            whenever(recentActionsRepository.getRecentActions()).thenReturn(list)

            underTest()

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
            whenever(recentActionsRepository.getRecentActions()).thenReturn(list)
            whenever(addNodeType(node1)).thenReturn(mock<TypedFileNode>())
            whenever(addNodeType(node2)).thenReturn(mock<TypedFolderNode>())

            val result = underTest()

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
            whenever(recentActionsRepository.getRecentActions()).thenReturn(list)

            underTest()

            verify(areCredentialsVerifiedUseCase).invoke(any())
        }

    @Test
    fun `test that isCurrentUserOwner is invoked for each RecentActionBucket`() = runTest {
        whenever(recentActionsRepository.getRecentActions()).thenReturn(
            listOf(dummyRecentActionBucketUnTyped)
        )

        underTest()

        verify(getAccountDetailsUseCase).invoke(false)
    }

    @Test
    fun `test that isCurrentUserOwner is false when email doesn't match`() = runTest {
        val userAccount = mock<UserAccount> {
            on { email }.thenReturn("ccc@gmail.com")
        }
        whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
        whenever(getAccountDetailsUseCase(false)).thenReturn(userAccount)
        whenever(recentActionsRepository.getRecentActions()).thenReturn(
            listOf(dummyRecentActionBucketUnTyped)
        )

        val result = underTest()

        assertThat(result[0].currentUserIsOwner).isFalse()
        verify(getAccountDetailsUseCase).invoke(false)
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
            whenever(recentActionsRepository.getRecentActions()).thenReturn(list)

            val result = underTest()

            assertThat(result).isEmpty()
        }

    @Test
    fun `test that the recent action user name item is populated with the fullName if retrieved from email`() =
        runTest {
            val expected = "FirstName LastName"
            val contact = mock<ContactData> {
                on { fullName }.thenReturn(expected)
            }
            val contactItem = mock<ContactItem> {
                on { email }.thenReturn("aaa@aaa.com")
                on { contactData }.thenReturn(contact)
            }
            val userAccount = mock<UserAccount> {
                on { email }.thenReturn("aaa@aaa.com")
            }
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getAccountDetailsUseCase(false)).thenReturn(userAccount)
            whenever(getVisibleContactsUseCase()).thenReturn(listOf(contactItem))
            whenever(recentActionsRepository.getRecentActions()).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest()

            assertThat(result[0].userName).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action user name item is populated with empty string if not retrieved from email`() =
        runTest {
            val expected = ""
            val contactItem = mock<ContactItem> {
                on { email }.thenReturn("aaa@aaa.com")
                on { contactData }.thenReturn(mock())
            }
            val userAccount = mock<UserAccount> {
                on { email }.thenReturn("aaa@aaa.com")
            }
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getAccountDetailsUseCase(false)).thenReturn(userAccount)
            whenever(getVisibleContactsUseCase()).thenReturn(listOf(contactItem))
            whenever(recentActionsRepository.getRecentActions()).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest()

            assertThat(result[0].userName).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action parent folder name item is set to empty string if not retrieved from the parent node`() =
        runTest {
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
            }
            val expected = ""
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions()).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest()

            assertThat(result[0].parentFolderName).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action parent folder name item is set if retrieved from the parent node`() =
        runTest {
            val expected = "Cloud drive"
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { name }.thenReturn(expected)
            }
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions()).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest()

            assertThat(result[0].parentFolderName).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to INCOMING_SHARES if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.INCOMING_SHARES
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isIncomingShare }.thenReturn(true)
            }
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions()).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest()

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to OUTGOING_SHARES if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.OUTGOING_SHARES
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isShared }.thenReturn(true)
            }
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions()).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest()

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to PENDING_OUTGOING_SHARES if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.PENDING_OUTGOING_SHARES
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isPendingShare }.thenReturn(true)
            }

            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions()).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest()

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }

    @Test
    fun `test that the recent action shares type item is set to NONE if parent root node is in incoming shares`() =
        runTest {
            val expected = RecentActionsSharesType.NONE
            val parentNode = mock<TypedFolderNode> {
                on { parentId }.thenReturn(NodeId(1L))
                on { isIncomingShare }.thenReturn(false)
                on { isShared }.thenReturn(false)
                on { isPendingShare }.thenReturn(false)
            }
            whenever(getNodeByIdUseCase(NodeId(321L))).thenReturn(parentNode)
            whenever(addNodeType(dummyNode1)).thenReturn(mock<TypedFileNode>())
            whenever(getNodeByIdUseCase(NodeId(1L))).thenReturn(null)
            whenever(recentActionsRepository.getRecentActions()).thenReturn(
                listOf(dummyRecentActionBucketUnTyped)
            )

            val result = underTest()

            assertThat(result[0].parentFolderSharesType).isEqualTo(expected)
        }
}