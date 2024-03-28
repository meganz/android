package mega.privacy.android.domain.usecase.recentactions

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.UserAccount
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
    fun `test that isCurrentUserOwner is invoked for each RecentActionBucket`() = runBlockingTest {
        val list = listOf(
            RecentActionBucketUnTyped(
                timestamp = 0L,
                userEmail = "aaa@aaa.com",
                parentNodeId = NodeId(0L),
                isUpdate = false,
                isMedia = false,
                nodes = listOf(mock<FileNode>())
            ),
        )
        whenever(recentActionsRepository.getRecentActions()).thenReturn(list)
        underTest()
        verify(getAccountDetailsUseCase).invoke(false)
    }

    @Test
    fun `test that isCurrentUserOwner is false when email doesn't match`() = runBlockingTest {
        val node1 = mock<FileNode>()
        val list = listOf(
            RecentActionBucketUnTyped(
                timestamp = 0L,
                userEmail = "aaa@aaa.com",
                parentNodeId = NodeId(0L),
                isUpdate = false,
                isMedia = false,
                nodes = listOf(node1)
            ),
        )
        val userAccount = mock<UserAccount> {
            on { email }.thenReturn("ccc@gmail.com")
        }
        whenever(addNodeType(node1)).thenReturn(mock<TypedFileNode>())
        whenever(getAccountDetailsUseCase(false)).thenReturn(userAccount)
        whenever(recentActionsRepository.getRecentActions()).thenReturn(list)
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
}