package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.GetSearchInSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchLinkSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchOutSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchFromMegaNodeParent
import mega.privacy.android.app.domain.usecase.SearchNodeUseCase
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SearchNodeUseCaseTest {
    private lateinit var underTest: SearchNodeUseCase
    private val getSearchLinkSharesNodes: GetSearchLinkSharesNodes = mock()
    private val getSearchOutSharesNodes: GetSearchOutSharesNodes = mock()
    private val getSearchInSharesNodes: GetSearchInSharesNodes = mock()
    private val getSearchFromMegaNodeParent: GetSearchFromMegaNodeParent = mock()
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val megaCancelToken: MegaCancelToken = mock()
    private val parentHandleSearch = 0L
    private val parentHandle = 0L
    private val invalidParentHandleSearch = -1L
    private val invalidParentHandle = -1L
    private val query = "Some Query"

    @Before
    fun setUp() {
        underTest = DefaultSearchNodeUseCase(
            getSearchInSharesNodes = getSearchInSharesNodes,
            getSearchFromMegaNodeParent = getSearchFromMegaNodeParent,
            getSearchLinkSharesNodes = getSearchLinkSharesNodes,
            getSearchOutSharesNodes = getSearchOutSharesNodes,
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
            megaCancelToken = megaCancelToken,
            isFirstLevel = true
        )
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test when search query is something with a valid parent handle valid parent handle search returns some search items`() =
        runTest {
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(parentHandleSearch)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    megaCancelToken = megaCancelToken,
                    query = query,
                    parentHandleSearch = parentHandleSearch,
                    parent = parent
                )
            ).thenReturn(listOf(mock(), mock()))
            val list = underTest(
                query = query,
                parentHandleSearch = parentHandleSearch,
                parentHandle = parentHandle,
                drawerItem = null,
                sharesTab = 0,
                megaCancelToken = megaCancelToken,
                isFirstLevel = true
            )
            verify(megaNodeRepository, times(1)).getNodeByHandle(parentHandleSearch)
            Truth.assertThat(list).hasSize(2)
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item HOMEPAGE returns some search items`() =
        runTest {
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getRootNode()).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    megaCancelToken = megaCancelToken,
                    parent = parent
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
                megaCancelToken = megaCancelToken,
                isFirstLevel = true
            )
            Truth.assertThat(list).hasSize(2)
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item CLOUD_DRIVE returns some search items`() =
        runTest {
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(invalidParentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    parent = parent,
                    megaCancelToken = megaCancelToken,
                    query = query,
                    parentHandleSearch = invalidParentHandle
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
                megaCancelToken = megaCancelToken,
                isFirstLevel = true
            )
            Truth.assertThat(list).hasSize(2)
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item RUBBISH_BIN returns empty list`() =
        runTest {
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = null,
                    megaCancelToken = megaCancelToken
                )
            ).thenReturn(emptyList())
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.RUBBISH_BIN,
                sharesTab = 0,
                megaCancelToken = megaCancelToken,
                isFirstLevel = true
            )
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item INBOX returns empty list`() =
        runTest {
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getInboxNode()).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    megaCancelToken = megaCancelToken
                )
            ).thenReturn(
                emptyList()
            )
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.INBOX,
                sharesTab = 0,
                megaCancelToken = megaCancelToken,
                isFirstLevel = true
            )
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item SHARED_ITEMS returns some items`() =
        runTest {
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(invalidParentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    megaCancelToken = megaCancelToken
                )
            ).thenReturn(emptyList())
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.SHARED_ITEMS,
                sharesTab = 0,
                megaCancelToken = megaCancelToken,
                isFirstLevel = true
            )
            verify(getSearchInSharesNodes, times(1)).invoke(query, megaCancelToken)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item OUTGOING_SHARED_ITEMS returns some items`() =
        runTest {
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(invalidParentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    megaCancelToken = megaCancelToken
                )
            ).thenReturn(emptyList())
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.SHARED_ITEMS,
                sharesTab = 1,
                megaCancelToken = megaCancelToken,
                isFirstLevel = true
            )
            verify(getSearchOutSharesNodes, times(1)).invoke(query, megaCancelToken)
            Truth.assertThat(list).isEmpty()
        }

    @Test
    fun `test when search query is something with a invalid ParentHandle invalid ParentSearchHandle and drawer item LINKS_SHARED_ITEMS returns some items`() =
        runTest {
            val parent: MegaNode = mock()
            whenever(megaNodeRepository.getNodeByHandle(invalidParentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = invalidParentHandleSearch,
                    parent = parent,
                    megaCancelToken = megaCancelToken
                )
            ).thenReturn(emptyList())
            val list = underTest(
                query = query,
                parentHandleSearch = invalidParentHandleSearch,
                parentHandle = invalidParentHandle,
                drawerItem = DrawerItem.SHARED_ITEMS,
                sharesTab = 2,
                megaCancelToken = megaCancelToken,
                isFirstLevel = true
            )
            verify(getSearchLinkSharesNodes, times(1)).invoke(query, megaCancelToken, true)
            Truth.assertThat(list).isEmpty()
        }
}