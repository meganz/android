package test.mega.privacy.android.app.domain.usecase.search

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.search.IncomingExplorerSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.search.GetSearchFromMegaNodeParentUseCase
import mega.privacy.android.app.domain.usecase.search.GetSearchInSharesNodesUseCase
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
class IncomingExplorerSearchNodeUseCaseTest {
    private lateinit var underTest: IncomingExplorerSearchNodeUseCase
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getSearchFromMegaNodeParentUseCase: GetSearchFromMegaNodeParentUseCase = mock()
    private val getSearchInSharesNodesUseCase: GetSearchInSharesNodesUseCase = mock()

    @Before
    fun setUp() {
        underTest = IncomingExplorerSearchNodeUseCase(
            megaNodeRepository = megaNodeRepository,
            getSearchFromMegaNodeParentUseCase = getSearchFromMegaNodeParentUseCase,
            getSearchInSharesNodesUseCase = getSearchInSharesNodesUseCase
        )
    }

    @Test
    fun `test incoming explorer search when query is null returns empty list`() = runTest {
        val list = underTest(
            query = null,
            parentHandle = 0L,
            parentHandleSearch = 0L,
            searchFilter = null
        )
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test incoming explorer search when parent handle is invalid handle`() = runTest {
        val searchFilter = SearchFilter(SearchCategory.VIDEO, "Video")
        val parentHandle = -1L
        val parent: MegaNode = mock()
        val query = "Some Query"
        whenever(megaNodeRepository.getNodeByHandle(parentHandle)).thenReturn(parent)
        whenever(
            getSearchFromMegaNodeParentUseCase(
                parentHandleSearch = parentHandle,
                parent = parent,
                query = query,
                searchFilter = searchFilter
            )
        ).thenReturn(listOf(mock()))
        val list = underTest(
            query = query,
            parentHandleSearch = parentHandle,
            parentHandle = parentHandle,
            searchFilter = searchFilter
        )
        verify(megaNodeRepository, times(1)).getNodeByHandle(parentHandle)
        verify(getSearchInSharesNodesUseCase, times(1)).invoke(
            query = query
        )
        verify(getSearchFromMegaNodeParentUseCase, times(1)).invoke(
            parentHandleSearch = parentHandle,
            parent = parent,
            query = query,
            searchFilter = searchFilter
        )
        Truth.assertThat(list).hasSize(1)
    }

}