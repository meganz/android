package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultSearchFromMegaNodeParent
import mega.privacy.android.app.domain.usecase.GetSearchFromMegaNodeParent
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSearchFromMegaNodeParentTest {
    private lateinit var underTest: GetSearchFromMegaNodeParent
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getCloudSortOrder: GetCloudSortOrder = mock()
    private val megaCancelToken: MegaCancelToken = mock()
    private val query = "Some query"
    private val parent: MegaNode = mock()
    private val parentHandleSearch = 0L
    private val invalidParentHandleSearch = -1L

    @Before
    fun setUp() {
        underTest = DefaultSearchFromMegaNodeParent(
            megaNodeRepository = megaNodeRepository,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @Test
    fun `test search when parent is null returns empty list`() = runTest {
        val list = underTest(
            parent = null,
            parentHandleSearch = parentHandleSearch,
            query = query,
            megaCancelToken = megaCancelToken
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
        val list = underTest(
            parent = parent,
            parentHandleSearch = parentHandleSearch,
            query = "",
            megaCancelToken = megaCancelToken
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
        val list = underTest(
            parent = parent,
            parentHandleSearch = parentHandleSearch,
            query = query,
            megaCancelToken = megaCancelToken
        )
        verify(megaNodeRepository, times(1)).getChildrenNode(
            parentNode = parent,
            order = getCloudSortOrder()
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
                megaCancelToken = megaCancelToken,
                order = getCloudSortOrder()
            )
        ).thenReturn(listOf(mock()))
        val list = underTest(
            parent = parent,
            query = query,
            parentHandleSearch = invalidParentHandleSearch,
            megaCancelToken = megaCancelToken
        )

        verify(megaNodeRepository, times(1)).search(
            parentNode = parent,
            query = query,
            megaCancelToken = megaCancelToken,
            order = getCloudSortOrder()
        )

        Truth.assertThat(list).hasSize(1)
    }
}