package mega.privacy.android.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.dao.RecentlyViewedLinkDao
import mega.privacy.android.data.database.entity.RecentlyViewedLinkEntity
import mega.privacy.android.data.database.entity.ViewedLinkRawItem
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.mapper.viewedlinks.RecentlyViewedLinkTypeIdMapper
import mega.privacy.android.data.mapper.viewedlinks.ViewedLinkRawItemMapper
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.ViewedLink
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ViewedLinksRepositoryImplTest {

    private lateinit var underTest: ViewedLinksRepositoryImpl

    private val recentlyViewedLinkDao: RecentlyViewedLinkDao = mock()
    private val viewedLinkRawItemMapper: ViewedLinkRawItemMapper = mock()
    private val recentlyViewedLinkTypeIdMapper = RecentlyViewedLinkTypeIdMapper()
    private val deviceGateway: DeviceGateway = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setUp() {
        underTest = ViewedLinksRepositoryImpl(
            recentlyViewedLinkDao = recentlyViewedLinkDao,
            viewedLinkRawItemMapper = viewedLinkRawItemMapper,
            recentlyViewedLinkTypeIdMapper = recentlyViewedLinkTypeIdMapper,
            deviceGateway = deviceGateway,
            ioDispatcher = testDispatcher,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            recentlyViewedLinkDao,
            viewedLinkRawItemMapper,
            deviceGateway
        )
    }

    @Test
    fun `test that getViewedLinksPagingSource returns mapped domain entities`() = runTest {
        val rawItems = listOf(
            ViewedLinkRawItem(
                nodeHandle = 1L,
                typeId = 1,
                nodeName = "test.pdf",
                lastAccessedTimestamp = 1000L,
                linkUrl = "https://mega.nz/file/abc",
            ),
            ViewedLinkRawItem(
                nodeHandle = 2L,
                typeId = 2,
                nodeName = "my-folder",
                lastAccessedTimestamp = 2000L,
                linkUrl = "https://mega.nz/folder/def",
            ),
        )
        val expectedLinks = listOf(
            ViewedLink(
                nodeHandle = 1L,
                name = "test.pdf",
                linkUrl = "https://mega.nz/file/abc",
                type = RecentlyViewedLinkType.FileLink,
                accessedTimestamp = 1000L,
            ),
            ViewedLink(
                nodeHandle = 2L,
                name = "my-folder",
                linkUrl = "https://mega.nz/folder/def",
                type = RecentlyViewedLinkType.FolderLink,
                accessedTimestamp = 2000L,
            ),
        )
        whenever(recentlyViewedLinkDao.getViewedLinksPagingSource())
            .thenReturn(fakeRawPagingSource(rawItems))
        whenever(viewedLinkRawItemMapper(rawItems[0])).thenReturn(expectedLinks[0])
        whenever(viewedLinkRawItemMapper(rawItems[1])).thenReturn(expectedLinks[1])

        val result = underTest.getViewedLinksPagingSource()
            .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false))

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        assertThat((result as PagingSource.LoadResult.Page).data).isEqualTo(expectedLinks)
    }

    @Test
    fun `test that getViewedLinksPagingSource delegates to viewedLinkRawItemMapper`() = runTest {
        val rawItem = ViewedLinkRawItem(
            nodeHandle = 1L,
            typeId = 1,
            nodeName = "test.pdf",
            lastAccessedTimestamp = 1000L,
            linkUrl = "https://mega.nz/file/abc",
        )
        whenever(recentlyViewedLinkDao.getViewedLinksPagingSource())
            .thenReturn(fakeRawPagingSource(listOf(rawItem)))
        whenever(viewedLinkRawItemMapper(rawItem)).thenReturn(
            ViewedLink(
                nodeHandle = 1L,
                name = "test.pdf",
                linkUrl = "https://mega.nz/file/abc",
                type = RecentlyViewedLinkType.FileLink,
                accessedTimestamp = 1000L,
            )
        )

        underTest.getViewedLinksPagingSource()
            .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false))

        verify(viewedLinkRawItemMapper).invoke(rawItem)
    }

    @Test
    fun `test that getViewedLinksPagingSource returns empty page when no links`() = runTest {
        whenever(recentlyViewedLinkDao.getViewedLinksPagingSource())
            .thenReturn(fakeRawPagingSource(emptyList()))

        val result = underTest.getViewedLinksPagingSource()
            .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false))

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        assertThat((result as PagingSource.LoadResult.Page).data).isEmpty()
    }

    @Test
    fun `test that saveLink calls saveViewedLink with correct entity for file link`() =
        runTest {
            val viewedLink = ViewedLink(
                nodeHandle = 123L,
                name = "document.pdf",
                linkUrl = "https://mega.nz/file/abc123",
                type = RecentlyViewedLinkType.FileLink,
                accessedTimestamp = 5000L,
            )

            underTest.saveLink(viewedLink)

            verify(recentlyViewedLinkDao).insertOrUpdateLink(
                RecentlyViewedLinkEntity(
                    nodeHandle = 123L,
                    typeId = 1,
                    nodeName = "document.pdf",
                    linkUrl = "https://mega.nz/file/abc123",
                    lastAccessedTimestamp = 5000L,
                ),
            )
        }

    @Test
    fun `test that saveLink calls saveViewedLink with correct entity for folder link`() =
        runTest {
            val viewedLink = ViewedLink(
                nodeHandle = 456L,
                name = "shared-folder",
                linkUrl = "https://mega.nz/folder/def456",
                type = RecentlyViewedLinkType.FolderLink,
                accessedTimestamp = 9000L,
            )

            underTest.saveLink(viewedLink)

            verify(recentlyViewedLinkDao).insertOrUpdateLink(
                RecentlyViewedLinkEntity(
                    nodeHandle = 456L,
                    typeId = 2,
                    nodeName = "shared-folder",
                    linkUrl = "https://mega.nz/folder/def456",
                    lastAccessedTimestamp = 9000L,
                ),
            )
        }

    @Test
    fun `test that removeLink calls deleteByNodeHandle`() = runTest {
        underTest.removeLink(789L)

        verify(recentlyViewedLinkDao).deleteByNodeHandle(789L)
    }

    @Test
    fun `test that clearLinks calls deleteAll`() = runTest {
        underTest.clearLinks()

        verify(recentlyViewedLinkDao).deleteAll()
    }

    private fun fakeRawPagingSource(items: List<ViewedLinkRawItem>): PagingSource<Int, ViewedLinkRawItem> =
        object : PagingSource<Int, ViewedLinkRawItem>() {
            override fun getRefreshKey(state: PagingState<Int, ViewedLinkRawItem>): Int? = null
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ViewedLinkRawItem> =
                LoadResult.Page(data = items, prevKey = null, nextKey = null)
        }
}
