package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.DateFilterOptionLongMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.data.mapper.search.SearchCategoryIntMapper
import mega.privacy.android.data.mapper.search.SearchCategoryMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
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
    private val dateFilterOptionLongMapper: DateFilterOptionLongMapper = mock()
    private val megaCancelToken: MegaCancelToken = mock()
    private val megsSearchFilterMapper: MegaSearchFilterMapper = mock()
    private val typedNode: TypedFileNode = mock {
        on { id } doReturn nodeId
    }
    private val megaNode: MegaNode = mock {
        on { handle } doReturn nodeHandle
    }


    @BeforeAll
    fun setUp() {
        underTest = SearchRepositoryImpl(
            searchCategoryMapper = SearchCategoryMapper(),
            searchCategoryIntMapper = SearchCategoryIntMapper(),
            nodeMapper = nodeMapper,
            megaApiGateway = megaApiGateway,
            ioDispatcher = ioDispatcher,
            cancelTokenProvider = cancelTokenProvider,
            getLinksSortOrder = getLinksSortOrder,
            sortOrderIntMapper = sortOrderIntMapper,
            dateFilterOptionLongMapper = dateFilterOptionLongMapper,
            megaSearchFilterMapper = megsSearchFilterMapper,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @Test
    fun `test that getSearchCategories returns list of search categories`() {
        val actual = underTest.getSearchCategories()
        assertThat(actual.sorted()).isEqualTo(
            listOf(
                SearchCategory.ALL,
                SearchCategory.AUDIO,
                SearchCategory.VIDEO,
                SearchCategory.ALL_DOCUMENTS,
                SearchCategory.IMAGES
            ).sorted()
        )
    }

    @Test
    fun `test that when search called with empty query calls getNodeChildren() once`() = runTest {
        whenever(sortOrderIntMapper(any())).thenReturn(0)
        whenever(megaApiGateway.getMegaNodeByHandle(nodeId.longValue)).thenReturn(null)

        val nodeId = NodeId(1L)
        val list = underTest.search(
            nodeId = nodeId,
            searchCategory = SearchCategory.ALL,
            query = "",
            order = SortOrder.ORDER_NONE
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

        whenever(megaNode.handle).thenReturn(-1L)
        whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(megaCancelToken)
        whenever(megaApiGateway.getMegaNodeByHandle(nodeID.longValue)).thenReturn(megaNode)

        whenever(
            megaApiGateway.search(
                parent = megaNode,
                query = query,
                megaCancelToken = megaCancelToken,
                order = sortOrderIntMapper(order)
            )
        ).thenReturn(emptyList())

        underTest.search(
            nodeId = nodeID,
            searchCategory = SearchCategory.ALL,
            query = query,
            order = order
        )
        verify(megaApiGateway).search(
            megaNode,
            query,
            megaCancelToken,
            sortOrderIntMapper(SortOrder.ORDER_NONE)
        )
    }

    @Test
    fun `test that when search called with some query and search type calls search() once`() =
        runTest {
            val order = SortOrder.ORDER_NONE
            whenever(sortOrderIntMapper(order)).thenReturn(0)
            val nodeID = NodeId(-1L)
            val megaNode: MegaNode = mock()
            val query = "Some query"

            whenever(megaNode.handle).thenReturn(-1L)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(megaCancelToken)
            whenever(megaApiGateway.getMegaNodeByHandle(nodeID.longValue)).thenReturn(megaNode)
            whenever(
                megaApiGateway.searchByType(
                    parentNode = megaNode,
                    searchString = query,
                    cancelToken = megaCancelToken,
                    recursive = true,
                    order = sortOrderIntMapper(SortOrder.ORDER_NONE),
                    type = MegaApiAndroid.FILE_TYPE_AUDIO
                )
            ).thenReturn(emptyList())

            underTest.search(
                nodeId = nodeID,
                searchCategory = SearchCategory.AUDIO,
                query = query,
                order = order
            )

            verify(megaApiGateway).searchByType(
                parentNode = megaNode,
                searchString = query,
                cancelToken = megaCancelToken,
                recursive = true,
                order = sortOrderIntMapper(SortOrder.ORDER_NONE),
                type = MegaApiAndroid.FILE_TYPE_AUDIO
            )
        }

    @Test
    fun `test that when search called with some query on incoming shares and it calls searchOnInShares() once`() =
        runTest {
            val query = "Some Query"
            val order = SortOrder.ORDER_NONE

            whenever(sortOrderIntMapper(any())).thenReturn(0)
            whenever(
                megaApiGateway.searchOnInShares(
                    query = query,
                    megaCancelToken = megaCancelToken,
                    order = sortOrderIntMapper(order)
                )
            ).thenReturn(emptyList())

            underTest.searchInShares(query = query, order = order)
            verify(megaApiGateway).searchOnInShares(
                query = query,
                megaCancelToken = megaCancelToken,
                order = sortOrderIntMapper(order)
            )
        }

    @Test
    fun `test that when search called with some query on outgoing shares and it calls searchOnOutShares() once`() =
        runTest {
            val query = "Some Query"
            val order = SortOrder.ORDER_NONE

            whenever(sortOrderIntMapper(any())).thenReturn(0)
            whenever(
                megaApiGateway.searchOnOutShares(
                    query = query,
                    megaCancelToken = megaCancelToken,
                    order = sortOrderIntMapper(order)
                )
            ).thenReturn(emptyList())

            underTest.searchOutShares(query = query, order = order)
            verify(megaApiGateway).searchOnInShares(
                query = query,
                megaCancelToken = megaCancelToken,
                order = sortOrderIntMapper(order)
            )
        }

    @Test
    fun `test that when search called with some query on link shares and it calls searchOnLinkShares() once`() =
        runTest {
            val query = "Some Query"
            val order = SortOrder.ORDER_NONE

            whenever(sortOrderIntMapper(order)).thenReturn(0)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(megaCancelToken)
            whenever(
                megaApiGateway.searchOnLinkShares(
                    query = query,
                    megaCancelToken = megaCancelToken,
                    order = sortOrderIntMapper(order)
                )
            ).thenReturn(emptyList())
            whenever(getLinksSortOrder()).thenReturn(SortOrder.ORDER_NONE)
            underTest.searchLinkShares(query = query, order = order, isFirstLevelNavigation = true)
            verify(megaApiGateway).searchOnLinkShares(
                query = query,
                megaCancelToken = megaCancelToken,
                order = sortOrderIntMapper(order)
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

    companion object {
        private const val nodeHandle = 1L
        private val nodeId = NodeId(nodeHandle)
    }
}