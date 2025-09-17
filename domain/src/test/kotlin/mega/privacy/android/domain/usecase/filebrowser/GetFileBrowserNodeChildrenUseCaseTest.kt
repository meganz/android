package mega.privacy.android.domain.usecase.filebrowser

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetFileBrowserNodeChildrenUseCaseTest {
    private lateinit var underTest: GetFileBrowserNodeChildrenUseCase

    private val getRootNodeIdUseCase: GetRootNodeIdUseCase = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val nodeRepository: NodeRepository = mock()
    private val getFolderTypeDataUseCase: GetFolderTypeDataUseCase = mock()

    @Before
    fun setUp() {
        underTest = GetFileBrowserNodeChildrenUseCase(
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            getCloudSortOrder = getCloudSortOrder,
            nodeRepository = nodeRepository,
            getFolderTypeDataUseCase = getFolderTypeDataUseCase
        )
    }

    @Test
    fun `test that getRootNodeUseCase is invoked once when the parent handle is -1`() = runTest {
        whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
        underTest(-1)
        verify(getRootNodeIdUseCase).invoke()
    }

    @Test
    fun `test that the file browser children is empty when getRootNodeUseCase returns null`() =
        runTest {
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getRootNodeIdUseCase()).thenReturn(null)
            val list = underTest(-1)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test that the file browser children exists when the parent handle exists and getNodeByHandleUseCase returns a node`() =
        runTest {
            val handle = 1234L
            val node = mock<DefaultTypedFolderNode> {
                on { id }.thenReturn(NodeId(handle))
            }
            val nodes = listOf(node)
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
            whenever(
                nodeRepository.getTypedNodesById(
                    nodeId = NodeId(handle),
                    getCloudSortOrder()
                )
            ).thenReturn(nodes)
            val list = underTest(handle)
            Truth.assertThat(list).isEqualTo(nodes)
        }
}