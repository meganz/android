package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.data.mapper.node.NodeMapper
import mega.privacy.android.data.mapper.search.MegaSearchFilterMapper
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import mega.privacy.android.domain.repository.FavouritesRepository
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFavouritesRepositoryTest {
    private lateinit var underTest: FavouritesRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    private val node = mock<MegaNode> {
        on { handle }.thenReturn(1L)
        on { name }.thenReturn("testName")
        on { size }.thenReturn(1000L)
        on { label }.thenReturn(MegaNode.NODE_LBL_RED)
    }

    private val favouriteInfo = mock<FolderNode>()

    private val nodeMapper: NodeMapper = mock()
    private val megaSearchFilterMapper: MegaSearchFilterMapper = mock()
    private val sortOrderIntMapper: SortOrderIntMapper = mock()
    private val cancelTokenProvider: CancelTokenProvider = mock()

    @Before
    fun setUp() {
        underTest = DefaultFavouritesRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            nodeMapper = nodeMapper,
            megaSearchFilterMapper = megaSearchFilterMapper,
            sortOrderIntMapper = sortOrderIntMapper,
            cancelTokenProvider = cancelTokenProvider,
        )
    }

    @Test
    fun `test that get all favourites returns successfully via searchWithFilter when excludeSensitives is false`() =
        runTest {
            val filter = mock<MegaSearchFilter>()
            val token = mock<MegaCancelToken>()
            whenever(
                megaSearchFilterMapper(
                    parentHandle = anyOrNull(),
                    searchQuery = any(),
                    searchTarget = any(),
                    searchCategory = eq(SearchCategory.FAVOURITES),
                    modificationDate = anyOrNull(),
                    creationDate = anyOrNull(),
                    description = anyOrNull(),
                    tag = anyOrNull(),
                    useAndForTextQuery = any(),
                    sensitivityFilter = eq(null),
                )
            ).thenReturn(filter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(sortOrderIntMapper(any(), any())).thenReturn(0)
            whenever(megaApiGateway.searchWithFilter(eq(filter), any(), eq(token), anyOrNull()))
                .thenReturn(listOf(node))
            whenever(
                nodeMapper(any(), any(), any(), anyOrNull(), anyOrNull())
            ).thenReturn(favouriteInfo)

            val actual = underTest.getAllFavorites()

            assertThat(actual[0]).isSameInstanceAs(favouriteInfo)
        }

    @Test
    fun `test that get all favourites with excludeSensitives true uses NonSensitiveOnly filter`() =
        runTest {
            val filter = mock<MegaSearchFilter>()
            val token = mock<MegaCancelToken>()
            whenever(
                megaSearchFilterMapper(
                    parentHandle = anyOrNull(),
                    searchQuery = any(),
                    searchTarget = any(),
                    searchCategory = eq(SearchCategory.FAVOURITES),
                    modificationDate = anyOrNull(),
                    creationDate = anyOrNull(),
                    description = anyOrNull(),
                    tag = anyOrNull(),
                    useAndForTextQuery = any(),
                    sensitivityFilter = eq(SensitivityFilterOption.NonSensitiveOnly),
                )
            ).thenReturn(filter)
            whenever(cancelTokenProvider.getOrCreateCancelToken()).thenReturn(token)
            whenever(sortOrderIntMapper(any(), any())).thenReturn(0)
            whenever(megaApiGateway.searchWithFilter(eq(filter), any(), eq(token), anyOrNull()))
                .thenReturn(listOf(node))
            whenever(
                nodeMapper(any(), any(), any(), anyOrNull(), anyOrNull())
            ).thenReturn(favouriteInfo)

            val actual = underTest.getAllFavorites(excludeSensitives = true)

            assertThat(actual[0]).isSameInstanceAs(favouriteInfo)
            verify(megaApiGateway).searchWithFilter(
                eq(filter),
                any(),
                eq(token),
                anyOrNull(),
            )
        }

    @Test
    fun `test that add favourites works properly`() = runTest {
        val nodeIds = listOf(
            NodeId(1L),
            NodeId(2L),
            NodeId(3L),
        )

        whenever(megaApiGateway.getMegaNodeByHandle(any()))
            .thenReturn(mock())
        underTest.addFavourites(nodeIds)
        verify(megaApiGateway, times(3)).setNodeFavourite(any(), any())
    }
}