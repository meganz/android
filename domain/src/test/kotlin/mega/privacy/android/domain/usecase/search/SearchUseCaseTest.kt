package mega.privacy.android.domain.usecase.search

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.node.AddNodesTypeUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SearchUseCaseTest {

    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val searchRepository: SearchRepository = mock()
    private val addNodesTypeUseCase: AddNodesTypeUseCase = mock()
    private val underTest = SearchUseCase(getCloudSortOrder, searchRepository, addNodesTypeUseCase)

    @Test
    fun `test that getInShares is called when query is empty and parentHandle is invalid and searchTarget is INCOMING_SHARE`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            underTest("", NodeId(-1), NodeSourceType.INCOMING_SHARES, SearchCategory.ALL)
            verify(searchRepository).getInShares()
        }

    @Test
    fun `test that getOutShares is called when query is empty and parentHandle is invalid and searchTarget is OUTGOING_SHARE`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            underTest("", NodeId(-1), NodeSourceType.OUTGOING_SHARES, SearchCategory.ALL)
            verify(searchRepository).getOutShares()
        }

    @Test
    fun `test that getPublicLinks is called when query is empty and parentHandle is invalid and searchTarget is LINKS_SHARE`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            underTest("", NodeId(-1), NodeSourceType.LINKS, SearchCategory.ALL)
            verify(searchRepository).getPublicLinks()
        }

    @Test
    fun `test that getChildren is called when query is empty`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getRootNodeId()).thenReturn(NodeId(-1))
            underTest("", NodeId(-1), NodeSourceType.CLOUD_DRIVE, SearchCategory.ALL)
            verify(searchRepository).getChildren(
                nodeId = NodeId(-1),
                searchCategory = SearchCategory.ALL,
                query = "",
                searchTarget = SearchTarget.ROOT_NODES,
                order = getCloudSortOrder(),
                modificationDate = null,
                creationDate = null
            )
        }

    @Test
    fun `test that getChildren is called when query is not empty and source type backup`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getBackUpNodeId()).thenReturn(NodeId(-1))
            underTest("", NodeId(-1), NodeSourceType.BACKUPS, SearchCategory.ALL)
            verify(searchRepository).getChildren(
                nodeId = NodeId(-1),
                searchCategory = SearchCategory.ALL,
                query = "",
                searchTarget = SearchTarget.ROOT_NODES,
                order = getCloudSortOrder(),
                modificationDate = null,
                creationDate = null
            )
        }

    @Test
    fun `test that getChildren is called when query is not empty and source type rubbish`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getRubbishNodeId()).thenReturn(NodeId(-1))
            underTest("", NodeId(-1), NodeSourceType.RUBBISH_BIN, SearchCategory.ALL)
            verify(searchRepository).getChildren(
                nodeId = NodeId(-1),
                searchCategory = SearchCategory.ALL,
                query = "",
                searchTarget = SearchTarget.ROOT_NODES,
                order = getCloudSortOrder(),
                modificationDate = null,
                creationDate = null
            )
        }

    @Test
    fun `test that getChildren is called when query is not empty parent handle not empty`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getRubbishNodeId()).thenReturn(NodeId(-1))
            underTest("", NodeId(123456), NodeSourceType.RUBBISH_BIN, SearchCategory.ALL)
            verify(searchRepository).getChildren(
                nodeId = NodeId(123456),
                searchCategory = SearchCategory.ALL,
                query = "",
                searchTarget = SearchTarget.ROOT_NODES,
                order = getCloudSortOrder(),
                modificationDate = null,
                creationDate = null
            )
        }

    @Test
    fun `test that search is called when query is not empty`() {
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getRubbishNodeId()).thenReturn(NodeId(-1))
            underTest("test", NodeId(123456), NodeSourceType.RUBBISH_BIN, SearchCategory.ALL)
            verify(searchRepository).search(
                nodeId = NodeId(123456),
                searchCategory = SearchCategory.ALL,
                query = "test",
                searchTarget = SearchTarget.ROOT_NODES,
                order = getCloudSortOrder(),
                modificationDate = null,
                creationDate = null
            )
        }
    }

}