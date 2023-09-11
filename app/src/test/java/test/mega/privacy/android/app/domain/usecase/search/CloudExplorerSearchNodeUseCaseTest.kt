package test.mega.privacy.android.app.domain.usecase.search

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.search.CloudExplorerSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.search.GetSearchFromMegaNodeParentUseCase
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.search.SearchCategory
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CloudExplorerSearchNodeUseCaseTest {
    private lateinit var underTest: CloudExplorerSearchNodeUseCase
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getSearchFromMegaNodeParent: GetSearchFromMegaNodeParentUseCase = mock()

    @Before
    fun setUp() {
        underTest = CloudExplorerSearchNodeUseCase(
            megaNodeRepository = megaNodeRepository,
            getSearchFromMegaNodeParentUseCase = getSearchFromMegaNodeParent
        )
    }

    @Test
    fun `test that when query is null in cloud explorer then it returns empty list`() = runTest {
        val list = underTest(
            query = null,
            parentHandle = -1L,
            parentHandleSearch = -1L,
            searchFilter = null
        )
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test that some items should be returned when any search query is invoked in cloud explorer`() =
        runTest {
            val searchFilter = SearchFilter(SearchCategory.ALL, "All")
            val parentHandle = -1L
            val parent: MegaNode = mock()
            val query = "Some query"
            whenever(megaNodeRepository.getNodeByHandle(parentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = parentHandle,
                    parent = parent,
                    searchFilter = searchFilter
                )
            ).thenReturn(listOf(mock()))
            val list = underTest(
                query = query,
                parentHandle = parentHandle,
                parentHandleSearch = parentHandle,
                searchFilter = searchFilter
            )
            verify(megaNodeRepository, times(1)).getNodeByHandle(parentHandle)
            verify(getSearchFromMegaNodeParent, times(1)).invoke(
                query = query,
                parentHandleSearch = parentHandle,
                parent = parent,
                searchFilter = searchFilter
            )
            Truth.assertThat(list).hasSize(1)
        }
}