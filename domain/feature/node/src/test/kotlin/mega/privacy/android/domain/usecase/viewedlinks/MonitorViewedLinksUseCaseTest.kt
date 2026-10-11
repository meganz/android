package mega.privacy.android.domain.usecase.viewedlinks

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.domain.repository.ViewedLinksRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorViewedLinksUseCaseTest {

    private val viewedLinksRepository: ViewedLinksRepository = mock()
    private lateinit var underTest: MonitorViewedLinksUseCase

    @BeforeEach
    fun setUp() {
        reset(viewedLinksRepository)
        underTest = MonitorViewedLinksUseCase(viewedLinksRepository)
    }

    @Test
    fun `test that invoke returns the repository paging source for given sort`() {
        val source = emptyPagingSource()
        whenever(
            viewedLinksRepository.getViewedLinksPagingSource(
                ViewedLinksSortField.Name,
                SortDirection.Ascending,
            )
        ).thenReturn(source)

        val result = underTest(ViewedLinksSortField.Name, SortDirection.Ascending)

        assertThat(result).isSameInstanceAs(source)
        verify(viewedLinksRepository).getViewedLinksPagingSource(
            ViewedLinksSortField.Name,
            SortDirection.Ascending,
        )
    }

    @Test
    fun `test that invoke forwards Created Descending to the repository`() {
        val source = emptyPagingSource()
        whenever(
            viewedLinksRepository.getViewedLinksPagingSource(
                ViewedLinksSortField.LastAccessed,
                SortDirection.Descending,
            )
        ).thenReturn(source)

        underTest(ViewedLinksSortField.LastAccessed, SortDirection.Descending)

        verify(viewedLinksRepository).getViewedLinksPagingSource(
            ViewedLinksSortField.LastAccessed,
            SortDirection.Descending,
        )
    }

    private fun emptyPagingSource(): PagingSource<Int, ViewedLink> =
        object : PagingSource<Int, ViewedLink>() {
            override fun getRefreshKey(state: PagingState<Int, ViewedLink>): Int? = null
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ViewedLink> =
                LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        }
}
