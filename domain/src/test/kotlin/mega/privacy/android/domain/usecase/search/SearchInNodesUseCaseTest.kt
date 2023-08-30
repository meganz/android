package mega.privacy.android.domain.usecase.search

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchInNodesUseCaseTest {
    private lateinit var underTest: SearchInNodesUseCase
    private val addNodeType: AddNodeType = mock()
    private val nodeRepository: NodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()

    @Before
    fun setUp() {
        underTest = SearchInNodesUseCase(
            addNodeType = addNodeType,
            nodeRepository = nodeRepository,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @Test
    fun `test that when searched by any query it executes it returns list of typed node`() =
        runTest {
            val query = "Any query"
            val nodeHandles = listOf(1L, 2L)
            val nodeName = listOf("abc", "xyz")
            val searchType = -1
            val parentNodeId = NodeId(111L)
            val folderNodes = nodeHandles.mapIndexed { index, nodeId ->
                mock<FolderNode> {
                    on { id }.thenReturn(NodeId(nodeId))
                    on { name }.thenReturn(nodeName[index])
                }
            }
            val typedFolderNodes = nodeHandles.mapIndexed { index, nodeId ->
                mock<TypedFolderNode> {
                    on { id }.thenReturn(NodeId(nodeId))
                    on { name }.thenReturn(nodeName[index])
                }
            }
            folderNodes.forEachIndexed { index, node ->
                whenever(addNodeType(node)).thenReturn(typedFolderNodes[index])
            }
            whenever(
                nodeRepository.search(
                    nodeId = parentNodeId,
                    query = query,
                    searchType = searchType,
                    order = getCloudSortOrder()
                )
            ).thenReturn(folderNodes)

            val actual = underTest(
                nodeId = parentNodeId,
                searchType = searchType,
                query = query
            )

            Truth.assertThat(actual).isEqualTo(typedFolderNodes)

        }
}