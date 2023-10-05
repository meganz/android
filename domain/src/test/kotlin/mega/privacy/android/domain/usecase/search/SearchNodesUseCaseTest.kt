package mega.privacy.android.domain.usecase.search

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetBackupsNodeUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchNodesUseCaseTest {
    private val incomingSharesTabSearchUseCase = mock<IncomingSharesTabSearchUseCase>()
    private val outgoingSharesTabSearchUseCase = mock<OutgoingSharesTabSearchUseCase>()
    private val linkSharesTabSearchUseCase = mock<LinkSharesTabSearchUseCase>()
    private val searchInNodesUseCase = mock<SearchInNodesUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val getNodeByHandleUseCase = mock<GetNodeByHandleUseCase>()
    private val getRubbishNodeUseCase = mock<GetRubbishNodeUseCase>()
    private val getBackupsNodeUseCase = mock<GetBackupsNodeUseCase>()
    private val nodeRepository = mock<NodeRepository>()
    private val nodeHandles = listOf(1L, 2L)
    private val nodeName = listOf("abc", "xyz")
    private val typedFolderNodes = nodeHandles.mapIndexed { index, nodeId ->
        mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(nodeId))
            on { name }.thenReturn(nodeName[index])
        }
    }

    @BeforeEach
    fun resetMock() {
        reset(
            incomingSharesTabSearchUseCase,
            outgoingSharesTabSearchUseCase,
            linkSharesTabSearchUseCase,
            searchInNodesUseCase,
            getRootNodeUseCase,
            getNodeByHandleUseCase,
            getRubbishNodeUseCase,
            getBackupsNodeUseCase,
            nodeRepository
        )
    }

    private val underTest = SearchNodesUseCase(
        incomingSharesTabSearchUseCase = incomingSharesTabSearchUseCase,
        outgoingSharesTabSearchUseCase = outgoingSharesTabSearchUseCase,
        linkSharesTabSearchUseCase = linkSharesTabSearchUseCase,
        searchInNodesUseCase = searchInNodesUseCase,
        getRootNodeUseCase = getRootNodeUseCase,
        getNodeByHandleUseCase = getNodeByHandleUseCase,
        getRubbishNodeUseCase = getRubbishNodeUseCase,
        getBackupsNodeUseCase = getBackupsNodeUseCase,
        nodeRepository = nodeRepository
    )

    @ParameterizedTest(name = "test that when search type is {0} search results are returned correctly")
    @EnumSource(SearchType::class)
    fun `test that search results are available for given search type`(searchType: SearchType) =
        runTest {
            val query = "query"
            val parentHandle = 12345L
            val node = mock<DefaultTypedFolderNode> {
                on { id }.thenReturn(NodeId(parentHandle))
            }
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(incomingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
            whenever(outgoingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
            whenever(linkSharesTabSearchUseCase(query, false)).thenReturn(typedFolderNodes)
            whenever(
                searchInNodesUseCase(
                    nodeId = NodeId(longValue = parentHandle),
                    searchCategory = SearchCategory.ALL,
                    query = query
                )
            ).thenReturn(typedFolderNodes)
            whenever(getNodeByHandleUseCase(parentHandle)).thenReturn(node)
            underTest(
                query = query,
                parentHandle = parentHandle,
                isFirstLevel = false,
                searchType = searchType
            )
        }

    @ParameterizedTest(name = "test that when search type is {0} search results are returned correctly when parent handle is invalid")
    @EnumSource(SearchType::class)
    fun `test that search results are available for given search type when parent handle is invalid`(
        searchType: SearchType,
    ) =
        runTest {
            val query = "query"
            val parentHandle = -1L
            val node = mock<DefaultTypedFolderNode> {
                on { id }.thenReturn(NodeId(parentHandle))
            }
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(incomingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
            whenever(outgoingSharesTabSearchUseCase(query)).thenReturn(typedFolderNodes)
            whenever(linkSharesTabSearchUseCase(query, false)).thenReturn(typedFolderNodes)
            whenever(
                searchInNodesUseCase(
                    nodeId = NodeId(longValue = parentHandle),
                    searchCategory = SearchCategory.ALL,
                    query = query
                )
            ).thenReturn(typedFolderNodes)
            whenever(getRootNodeUseCase()).thenReturn(node)
            whenever(getRubbishNodeUseCase()).thenReturn(node)
            whenever(getBackupsNodeUseCase()).thenReturn(node)
            underTest(
                query = query,
                parentHandle = parentHandle,
                isFirstLevel = false,
                searchType = searchType
            )
        }

}