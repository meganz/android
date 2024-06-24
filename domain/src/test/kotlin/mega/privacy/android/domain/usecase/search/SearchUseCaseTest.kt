package mega.privacy.android.domain.usecase.search

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchParameters
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
            underTest(
                parentHandle = NodeId(-1),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                searchParameters = SearchParameters(
                    query = "",
                    searchTarget = SearchTarget.INCOMING_SHARE,
                    searchCategory = SearchCategory.ALL
                )
            )
            verify(searchRepository).getInShares()
        }

    @Test
    fun `test that getOutShares is called when query is empty and parentHandle is invalid and searchTarget is OUTGOING_SHARE`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            underTest(
                parentHandle = NodeId(-1),
                nodeSourceType = NodeSourceType.OUTGOING_SHARES,
                searchParameters = SearchParameters(
                    query = "",
                    searchTarget = SearchTarget.OUTGOING_SHARE,
                    searchCategory = SearchCategory.ALL
                )
            )
            verify(searchRepository).getOutShares()
        }

    @Test
    fun `test that getPublicLinks is called when query is empty and parentHandle is invalid and searchTarget is LINKS_SHARE`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            underTest(
                parentHandle = NodeId(-1),
                nodeSourceType = NodeSourceType.LINKS,
                searchParameters = SearchParameters(
                    query = "",
                    searchTarget = SearchTarget.LINKS_SHARE,
                    searchCategory = SearchCategory.ALL
                )
            )
            verify(searchRepository).getPublicLinks()
        }

    @Test
    fun `test that getChildren is called when query is empty`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getRootNodeId()).thenReturn(NodeId(-1))
            underTest(
                parentHandle = NodeId(-1),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                searchParameters = SearchParameters(
                    query = "",
                    searchCategory = SearchCategory.ALL
                )
            )
            verify(searchRepository).getChildren(
                nodeId = NodeId(-1),
                order = getCloudSortOrder(),
                parameters = SearchParameters(
                    query = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL,
                ),
            )
        }

    @Test
    fun `test that getChildren is called when query is not empty and source type backup`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getBackUpNodeId()).thenReturn(NodeId(-1))
            underTest(
                parentHandle = NodeId(-1),
                nodeSourceType = NodeSourceType.BACKUPS,
                searchParameters = SearchParameters(
                    query = "",
                    searchCategory = SearchCategory.ALL
                )
            )
            verify(searchRepository).getChildren(
                nodeId = NodeId(-1),
                order = getCloudSortOrder(),
                parameters = SearchParameters(
                    query = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL,
                ),
            )
        }

    @Test
    fun `test that getChildren is called when query is not empty and source type rubbish`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getRubbishNodeId()).thenReturn(NodeId(-1))
            underTest(
                parentHandle = NodeId(-1),
                nodeSourceType = NodeSourceType.RUBBISH_BIN,
                searchParameters = SearchParameters(
                    query = "",
                    searchCategory = SearchCategory.ALL
                )
            )
            verify(searchRepository).getChildren(
                nodeId = NodeId(-1),
                order = getCloudSortOrder(),
                parameters = SearchParameters(
                    query = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL,
                ),
            )
        }

    @Test
    fun `test that getChildren is called when query is not empty parent handle not empty`() =
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getRubbishNodeId()).thenReturn(NodeId(-1))
            underTest(
                parentHandle = NodeId(123456),
                nodeSourceType = NodeSourceType.RUBBISH_BIN,
                searchParameters = SearchParameters(
                    query = "",
                    searchCategory = SearchCategory.ALL
                )
            )
            verify(searchRepository).getChildren(
                nodeId = NodeId(123456),
                order = getCloudSortOrder(),
                parameters = SearchParameters(
                    query = "",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL,
                ),
            )
        }

    @Test
    fun `test that search is called when query is not empty`() {
        runTest {
            whenever(searchRepository.getInvalidHandle()).thenReturn(NodeId(-1))
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            whenever(searchRepository.getRubbishNodeId()).thenReturn(NodeId(-1))
            underTest(
                parentHandle = NodeId(123456),
                nodeSourceType = NodeSourceType.RUBBISH_BIN,
                searchParameters = SearchParameters(
                    query = "test",
                    searchCategory = SearchCategory.ALL,
                    modificationDate = DateFilterOption.Today,
                )
            )
            verify(searchRepository).search(
                nodeId = NodeId(123456),
                order = getCloudSortOrder(),
                parameters = SearchParameters(
                    query = "test",
                    searchTarget = SearchTarget.ROOT_NODES,
                    searchCategory = SearchCategory.ALL,
                    modificationDate = DateFilterOption.Today,
                    creationDate = null
                ),
            )
        }
    }
}
