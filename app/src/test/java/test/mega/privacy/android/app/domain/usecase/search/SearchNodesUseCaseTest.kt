package test.mega.privacy.android.app.domain.usecase.search

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.search.GetSearchFromMegaNodeParentUseCase
import mega.privacy.android.app.domain.usecase.search.GetSearchInSharesNodesUseCase
import mega.privacy.android.app.domain.usecase.search.GetSearchLinkSharesNodesUseCase
import mega.privacy.android.app.domain.usecase.search.GetSearchOutSharesNodesUseCase
import mega.privacy.android.app.domain.usecase.search.SearchNodesUseCase
import mega.privacy.android.app.main.DrawerItem
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

class SearchNodesUseCaseTest {
    private lateinit var underTest: SearchNodesUseCase
    private val getSearchLinkSharesNodes: GetSearchLinkSharesNodesUseCase = mock()
    private val getSearchOutSharesNodes: GetSearchOutSharesNodesUseCase = mock()
    private val getSearchInSharesNodes: GetSearchInSharesNodesUseCase = mock()
    private val getSearchFromMegaNodeParent: GetSearchFromMegaNodeParentUseCase = mock()
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val parentHandleSearch = 0L
    private val parentHandle = 0L
    private val invalidParentHandleSearch = -1L
    private val invalidParentHandle = -1L
    private val query = "Some Query"

    @Before
    fun setUp() {
        underTest = SearchNodesUseCase(
            getSearchInSharesNodesUseCase = getSearchInSharesNodes,
            getSearchFromMegaNodeParentUseCase = getSearchFromMegaNodeParent,
            getSearchLinkSharesNodesUseCase = getSearchLinkSharesNodes,
            getSearchOutSharesNodesUseCase = getSearchOutSharesNodes,
            megaNodeRepository = megaNodeRepository
        )
    }

    @Test
    fun `test when search query is null returns empty list`() = runTest {
        val list = underTest(
            query = null,
            parentHandleSearch = parentHandleSearch,
            parentHandle = parentHandle,
            drawerItem = null,
            sharesTab = 0,
            isFirstLevel = true
        )
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test when search query is something with a valid parent handle valid parent handle search returns some search items`() =
        runTest {
            val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(parentHandleSearch)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = parentHandleSearch,
                    parent = parent,
                    searchFilter = searchFilter
                )
            ).thenReturn(listOf(mock(), mock()))
            val list = underTest(
                query = query,
                parentHandleSearch = parentHandleSearch,
                parentHandle = parentHandle,
                drawerItem = null,
                sharesTab = 0,
                isFirstLevel = true,
                searchFilter = searchFilter
            )
            verify(megaNodeRepository, times(1)).getNodeByHandle(parentHandleSearch)
            Truth.assertThat(list).hasSize(2)
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item HOMEPAGE returns some search items`() =
        runTest {
            val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getRootNode()).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    searchFilter = searchFilter
                )
            ).thenReturn(
                listOf(mock(), mock())
            )
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.HOMEPAGE,
                sharesTab = 0,
                isFirstLevel = true,
                searchFilter = searchFilter
            )
            Truth.assertThat(list).hasSize(2)
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item CLOUD_DRIVE returns some search items`() =
        runTest {
            val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(invalidParentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    parent = parent,
                    query = query,
                    parentHandleSearch = invalidParentHandle,
                    searchFilter = searchFilter
                )
            ).thenReturn(
                listOf(mock(), mock())
            )
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.CLOUD_DRIVE,
                sharesTab = 0,
                isFirstLevel = true,
                searchFilter = searchFilter
            )
            Truth.assertThat(list).hasSize(2)
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item RUBBISH_BIN returns empty list`() =
        runTest {
            val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = null,
                    searchFilter = searchFilter
                )
            ).thenReturn(emptyList())
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.RUBBISH_BIN,
                sharesTab = 0,
                isFirstLevel = true,
                searchFilter = searchFilter
            )
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item BACKUPS returns empty list`() =
        runTest {
            val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getBackupsNode()).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    searchFilter = searchFilter
                )
            ).thenReturn(
                emptyList()
            )
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.BACKUPS,
                sharesTab = 0,
                isFirstLevel = true,
                searchFilter = searchFilter
            )
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item SHARED_ITEMS returns some items`() =
        runTest {
            val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(invalidParentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    searchFilter = searchFilter
                )
            ).thenReturn(emptyList())
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.SHARED_ITEMS,
                sharesTab = 0,
                isFirstLevel = true,
                searchFilter = searchFilter
            )
            verify(getSearchInSharesNodes, times(1)).invoke(query)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item OUTGOING_SHARED_ITEMS returns some items`() =
        runTest {
            val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(invalidParentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    searchFilter = searchFilter
                )
            ).thenReturn(emptyList())
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.SHARED_ITEMS,
                sharesTab = 1,
                isFirstLevel = true,
                searchFilter = searchFilter
            )
            verify(getSearchOutSharesNodes, times(1)).invoke(query)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item LINKS_SHARED_ITEMS returns some items`() =
        runTest {
            val searchFilter = SearchFilter(filter = SearchCategory.ALL, name = "All")
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(invalidParentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    searchFilter = searchFilter
                )
            ).thenReturn(emptyList())
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.SHARED_ITEMS,
                sharesTab = 2,
                isFirstLevel = true,
                searchFilter = searchFilter
            )
            verify(getSearchLinkSharesNodes, times(1)).invoke(query, true)
            Truth.assertThat(list).isEmpty()
        }
}