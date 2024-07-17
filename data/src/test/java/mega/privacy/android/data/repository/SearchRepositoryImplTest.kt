package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter
import nz.mega.sdk.MegaShare
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryImplTest {
    private lateinit var underTest: SearchRepository
    private val nodeMapper: NodeMapper = mock()
    private val megaApiGateway: MegaApiGateway = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private val cancelTokenProvider: CancelTokenProvider = mock()
    private val getLinksSortOrder: GetLinksSortOrder = mock()
    private val sortOrderIntMapper: SortOrderIntMapper = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val megaCancelToken: MegaCancelToken = mock()
    private val megsSearchFilterMapper: MegaSearchFilterMapper = mock()
    private val typedNode: TypedFileNode = mock {
        on { id } doReturn nodeId
    }
    private val megaNode: MegaNode = mock {
        on { handle } doReturn 123456L
    }


    @BeforeAll
    fun setUp() {
        underTest = SearchRepositoryImpl(
            nodeMapper = nodeMapper,
            megaApiGateway = megaApiGateway,
            ioDispatcher = ioDispatcher,
            cancelTokenProvider = cancelTokenProvider,
            getLinksSortOrder = getLinksSortOrder,
            sortOrderIntMapper = sortOrderIntMapper,
            megaSearchFilterMapper = megsSearchFilterMapper,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @Test
    fun `test that when search called with empty query calls getNodeChildren() once`() = runTest {
        whenever(sortOrderIntMapper(any())).thenReturn(0)
        val nodeID = NodeId(-1L)
        val megaNode: MegaNode = mock()
        val query = "Some query"
        val order = SortOrder.ORDER_NONE
        val filter = mock<MegaSearchFilter>()
        whenever(megaNode.handle).thenReturn(-1L)
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(megaCancelToken)
        whenever(megaApiGateway.getMegaNodeByHandle(nodeID.longValue)).thenReturn(megaNode)
        whenever(
            megsSearchFilterMapper(
                searchQuery = query,
                parentHandle = nodeId,
                searchCategory = SearchCategory.ALL
            )
        ).thenReturn(filter)
        whenever(
            megaApiGateway.getChildren(
                filter = filter,
                order = sortOrderIntMapper(order),
                megaCancelToken = megaCancelToken
            )
        ).thenReturn(emptyList())
        val list = underTest.getChildren(
            nodeId = nodeId,
            order = SortOrder.ORDER_NONE,
            parameters = SearchParameters(
                query = query,
            ),
        )
        assertThat(list).isEmpty()
    }

    @Test
    fun `test that when search called with some query calls search() once`() = runTest {
        whenever(sortOrderIntMapper(any())).thenReturn(0)
        val nodeID = NodeId(-1L)
        val megaNode: MegaNode = mock()
        val query = "Some query"
        val order = SortOrder.ORDER_NONE
        val filter = mock<MegaSearchFilter>()
        whenever(megaNode.handle).thenReturn(-1L)
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(megaCancelToken)
        whenever(megaApiGateway.getMegaNodeByHandle(nodeID.longValue)).thenReturn(megaNode)
        whenever(
            megsSearchFilterMapper(
                searchQuery = query,
                parentHandle = nodeID,
                searchCategory = SearchCategory.ALL
            )
        ).thenReturn(filter)


        whenever(
            megaApiGateway.searchWithFilter(
                filter = filter,
                megaCancelToken = megaCancelToken,
                order = sortOrderIntMapper(order)
            )
        ).thenReturn(emptyList())

        underTest.search(
            nodeId = nodeID,
            order = order,
            parameters = SearchParameters(
                query = query,
            ),
        )
        verify(megaApiGateway).searchWithFilter(
            filter,
            sortOrderIntMapper(SortOrder.ORDER_NONE),
            megaCancelToken
        )
    }

    @Test
    fun `test that getInShares returns list of untyped nodes`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(megaApiGateway.getInShares(sortOrderIntMapper(SortOrder.ORDER_NONE))).thenReturn(
            listOf(megaNode)
        )
        whenever(nodeMapper(megaNode)).thenReturn(typedNode)
        val actual = underTest.getInShares()
        assertThat(actual.first().id).isEqualTo(nodeId)
    }

    @Test
    fun `test that getPublicLinks returns list of untyped nodes`() = runTest {
        whenever(getLinksSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        whenever(megaApiGateway.getPublicLinks(sortOrderIntMapper(getLinksSortOrder())))
            .thenReturn(listOf(megaNode))
        whenever(nodeMapper(megaNode)).thenReturn(typedNode)
        val actual = underTest.getPublicLinks()
        assertThat(actual.first().id).isEqualTo(nodeId)
    }

    @Test
    fun `test that getBackupNodeId returns node id`() = runTest {
        whenever(megaApiGateway.getBackupsNode()).thenReturn(megaNode)
        val actual = underTest.getBackUpNodeId()
        assertThat(actual).isEqualTo(nodeId)
    }

    @Test
    fun `test that getRootNodeId returns node id`() = runTest {
        whenever(megaApiGateway.getRootNode()).thenReturn(megaNode)
        val actual = underTest.getRootNodeId()
        assertThat(actual).isEqualTo(nodeId)
    }

    @Test
    fun `test that getRubbishNodeId returns node id`() = runTest {
        whenever(megaApiGateway.getRubbishBinNode()).thenReturn(megaNode)
        val actual = underTest.getRubbishNodeId()
        assertThat(actual).isEqualTo(nodeId)
    }

    @Test
    fun `test that getOutShares returns list of untyped nodes`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_NONE)
        val share = mock<MegaShare> {
            on { nodeHandle } doReturn 123456L
        }
        val shares = listOf(share)
        whenever(megaApiGateway.getOutgoingSharesNode(sortOrderIntMapper(SortOrder.ORDER_NONE))).thenReturn(
            shares
        )
        whenever(megaApiGateway.getMegaNodeByHandle(megaNode.handle)).thenReturn(megaNode)
        whenever(megaNode.handle).thenReturn(123456L)
        whenever(nodeMapper(megaNode)).thenReturn(typedNode)
        val actual = underTest.getOutShares()
        assertThat(actual.first().id).isEqualTo(nodeId)
    }

    @Test
    fun `test that getInvalidHandle returns invalid node id`() = runTest {
        whenever(megaApiGateway.getInvalidHandle()).thenReturn(-1L)
        val actual = underTest.getInvalidHandle()
        assertThat(actual).isEqualTo(NodeId(-1L))
    }

    companion object {
        private val nodeId = NodeId(123456L)
    }
}