package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultIncomingExplorerSearchNode
import mega.privacy.android.app.domain.usecase.GetIncomingExplorerSearchNode
import mega.privacy.android.app.domain.usecase.GetSearchFromMegaNodeParent
import mega.privacy.android.app.domain.usecase.GetSearchInSharesNodes
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
class DefaultIncomingExplorerSearchNodeTest {
    private lateinit var underTest: GetIncomingExplorerSearchNode
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getSearchFromMegaNodeParent: GetSearchFromMegaNodeParent = mock()
    private val getSearchInSharesNodes: GetSearchInSharesNodes = mock()
    private val megaCancelToken: MegaCancelToken = mock()

    @Before
    fun setUp() {
        underTest = DefaultIncomingExplorerSearchNode(
            megaNodeRepository = megaNodeRepository,
            getSearchFromMegaNodeParent = getSearchFromMegaNodeParent,
            getSearchInSharesNodes = getSearchInSharesNodes
        )
    }

    @Test
    fun `test incoming explorer search when query is null returns empty list`() = runTest {
        val list = underTest(
            query = null,
            parentHandle = 0L,
            parentHandleSearch = 0L,
            megaCancelToken = megaCancelToken
        )
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test incoming explorer search when parent handle is invalid handle`() = runTest {
        val parentHandle = -1L
        val parent: MegaNode = mock()
        val query = "Some Query"
        whenever(megaNodeRepository.getNodeByHandle(parentHandle)).thenReturn(parent)
        whenever(
            getSearchFromMegaNodeParent(
                parentHandleSearch = parentHandle,
                parent = parent,
                query = query,
                megaCancelToken = megaCancelToken
            )
        ).thenReturn(listOf(mock()))
        val list = underTest(
            query = query,
            parentHandleSearch = parentHandle,
            parentHandle = parentHandle,
            megaCancelToken = megaCancelToken
        )
        verify(megaNodeRepository, times(1)).getNodeByHandle(parentHandle)
        verify(getSearchInSharesNodes, times(1)).invoke(
            query = query, megaCancelToken = megaCancelToken
        )
        verify(getSearchFromMegaNodeParent, times(1)).invoke(
            parentHandleSearch = parentHandle,
            parent = parent,
            query = query,
            megaCancelToken = megaCancelToken
        )
        Truth.assertThat(list).hasSize(1)
    }

}