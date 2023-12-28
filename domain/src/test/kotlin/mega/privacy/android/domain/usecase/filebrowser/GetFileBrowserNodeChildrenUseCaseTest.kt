package mega.privacy.android.domain.usecase.filebrowser

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetFileBrowserNodeChildrenUseCaseTest {
    private lateinit var underTest: GetFileBrowserNodeChildrenUseCase

    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val nodeRepository: NodeRepository = mock()
    private val addNodeType: AddNodeType = mock()

    @Before
    fun setUp() {
        underTest = GetFileBrowserNodeChildrenUseCase(
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            getCloudSortOrder = getCloudSortOrder,
            nodeRepository = nodeRepository,
            addNodeType = addNodeType,
        )
    }

    @Test
    fun `test that getRootNodeUseCase is invoked once when the parent handle is -1`() = runTest {
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        underTest(-1)
        verify(getRootNodeUseCase).invoke()
    }

    @Test
    fun `test that the file browser children is empty when getRootNodeUseCase returns null`() =
        runTest {
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getRootNodeUseCase()).thenReturn(null)
            val list = underTest(-1)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test that the file browser children is empty when getNodeByHandleUseCase returns null`() =
        runTest {
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            val handle = 1234L
            whenever(getNodeByHandleUseCase(handle)).thenReturn(null)
            val list = underTest(handle)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test that the file browser children exists when the parent handle exists and getNodeByHandleUseCase returns a node`() =
        runTest {
            val handle = 1234L
            val node = mock<DefaultTypedFolderNode> {
                on { id }.thenReturn(NodeId(handle))
            }
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(getNodeByHandleUseCase(handle)).thenReturn(node)
            whenever(
                nodeRepository.getNodeChildren(
                    nodeId = NodeId(handle),
                    getCloudSortOrder()
                )
            ).thenReturn(listOf<FolderNode>(mock()))
            val list = underTest(handle)
            Truth.assertThat(list).isNotEmpty()
        }
}