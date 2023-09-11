package test.mega.privacy.android.app.domain.usecase.search

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.search.GetSearchFromMegaNodeParentUseCase
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.data.mapper.search.SearchCategoryIntMapper
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetSearchFromMegaNodeParentUseCaseTest {
    private lateinit var underTest: GetSearchFromMegaNodeParentUseCase
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val query = "Some query"
    private val parent: MegaNode = mock()
    private val parentHandleSearch = 0L
    private val invalidParentHandleSearch = -1L
    private val searchCategoryIntMapper = SearchCategoryIntMapper()

    @Before
    fun setUp() {
        underTest = GetSearchFromMegaNodeParentUseCase(
            megaNodeRepository = megaNodeRepository,
            getCloudSortOrder = getCloudSortOrder,
            searchCategoryIntMapper = searchCategoryIntMapper
        )
    }

    @Test
    fun `test search when parent is null returns empty list`() = runTest {
        val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
        val list = underTest(
            parent = null,
            parentHandleSearch = parentHandleSearch,
            query = query,
            searchFilter = searchFilter
        )
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test search when query is empty returns some items from list`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(megaNodeRepository.getChildrenNode(parent, getCloudSortOrder())).thenReturn(
            listOf(
                mock(),
                mock()
            )
        )
        val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
        val list = underTest(
            parent = parent,
            parentHandleSearch = parentHandleSearch,
            query = "",
            searchFilter = searchFilter
        )
        verify(megaNodeRepository, times(1)).getChildrenNode(
            parentNode = parent,
            order = getCloudSortOrder()
        )
        Truth.assertThat(list).hasSize(2)
    }

    @Test
    fun `test search when parent search handle search is valid returns empty list`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(megaNodeRepository.getChildrenNode(parent, getCloudSortOrder())).thenReturn(
            emptyList()
        )
        val searchFilter = SearchFilter(filter = SearchCategory.IMAGES, name = "Images")
        val list = underTest(
            parent = parent,
            parentHandleSearch = parentHandleSearch,
            query = query,
            searchFilter = searchFilter
        )
        verify(megaNodeRepository, times(1)).search(
            parentNode = parent,
            query = query,
            order = getCloudSortOrder(),
            searchType = searchCategoryIntMapper(searchFilter.filter)
        )
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test search when parent search handle is invalid returns some items in list`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(
            megaNodeRepository.search(
                parentNode = parent,
                query = query,
                order = getCloudSortOrder()
            )
        ).thenReturn(listOf(mock()))
        val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
        val list = underTest(
            parent = parent,
            query = query,
            parentHandleSearch = invalidParentHandleSearch,
            searchFilter = searchFilter
        )

        verify(megaNodeRepository, times(1)).search(
            parentNode = parent,
            query = query,
            order = getCloudSortOrder()
        )

        Truth.assertThat(list).hasSize(1)
    }
}