package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.dao.RecentlyViewedLinkDao
import mega.privacy.android.data.database.entity.RecentlyUsedEntity
import mega.privacy.android.data.database.entity.RecentlyViewedLinkEntity
import mega.privacy.android.data.database.entity.ViewedLinkRawItem
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.mapper.continuewhereleftoff.RecentlyUsedTypeIdMapper
import mega.privacy.android.data.mapper.viewedlinks.ViewedLinkRawItemMapper
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.ViewedLink
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
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
    private val recentlyUsedTypeIdMapper = RecentlyUsedTypeIdMapper()
    private val deviceGateway: DeviceGateway = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setUp() {
        underTest = ViewedLinksRepositoryImpl(
            recentlyViewedLinkDao = recentlyViewedLinkDao,
            viewedLinkRawItemMapper = viewedLinkRawItemMapper,
            recentlyUsedTypeIdMapper = recentlyUsedTypeIdMapper,
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
    fun `test that monitorLinks returns mapped domain entities`() = runTest {
        val rawItems = listOf(
            ViewedLinkRawItem(
                nodeHandle = 1L,
                typeId = 5,
                fileName = "test.pdf",
                lastAccessedTimestamp = 1000L,
                linkUrl = "https://mega.nz/file/abc",
            ),
            ViewedLinkRawItem(
                nodeHandle = 2L,
                typeId = 6,
                fileName = "my-folder",
                lastAccessedTimestamp = 2000L,
                linkUrl = "https://mega.nz/folder/def",
            ),
        )
        val expectedLinks = listOf(
            ViewedLink(
                nodeHandle = 1L,
                name = "test.pdf",
                linkUrl = "https://mega.nz/file/abc",
                type = RecentlyUsedType.FileLink,
                accessedTimestamp = 1000L,
            ),
            ViewedLink(
                nodeHandle = 2L,
                name = "my-folder",
                linkUrl = "https://mega.nz/folder/def",
                type = RecentlyUsedType.FolderLink,
                accessedTimestamp = 2000L,
            ),
        )
        whenever(recentlyViewedLinkDao.monitorViewedLinks()).thenReturn(flowOf(rawItems))
        whenever(viewedLinkRawItemMapper(rawItems)).thenReturn(expectedLinks)

        underTest.monitorLinks().test {
            val result = awaitItem()
            assertThat(result).isEqualTo(expectedLinks)
            awaitComplete()
        }
    }

    @Test
    fun `test that monitorLinks delegates to viewedLinkRawItemMapper`() = runTest {
        val rawItems = listOf(
            ViewedLinkRawItem(
                nodeHandle = 1L,
                typeId = 5,
                fileName = "test.pdf",
                lastAccessedTimestamp = 1000L,
                linkUrl = "https://mega.nz/file/abc",
            ),
        )
        whenever(recentlyViewedLinkDao.monitorViewedLinks()).thenReturn(flowOf(rawItems))
        whenever(viewedLinkRawItemMapper(any<List<ViewedLinkRawItem>>())).thenReturn(emptyList())

        underTest.monitorLinks().test {
            awaitItem()
            awaitComplete()
        }

        verify(viewedLinkRawItemMapper).invoke(rawItems)
    }

    @Test
    fun `test that monitorLinks returns empty list when no links`() = runTest {
        whenever(recentlyViewedLinkDao.monitorViewedLinks()).thenReturn(flowOf(emptyList()))
        whenever(viewedLinkRawItemMapper(any<List<ViewedLinkRawItem>>())).thenReturn(emptyList())

        underTest.monitorLinks().test {
            val result = awaitItem()
            assertThat(result).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun `test that saveLink calls saveViewedLink with correct entities for file link`() =
        runTest {
            val viewedLink = ViewedLink(
                nodeHandle = 123L,
                name = "document.pdf",
                linkUrl = "https://mega.nz/file/abc123",
                type = RecentlyUsedType.FileLink,
                accessedTimestamp = 5000L,
            )

            underTest.saveLink(viewedLink)

            verify(recentlyViewedLinkDao).saveViewedLink(
                recentlyUsedEntity = RecentlyUsedEntity(
                    nodeHandle = 123L,
                    typeId = 5,
                    fileName = "document.pdf",
                    lastAccessedTimestamp = 5000L,
                ),
                recentlyViewedLinkEntity = RecentlyViewedLinkEntity(
                    nodeHandle = 123L,
                    linkUrl = "https://mega.nz/file/abc123",
                ),
            )
        }

    @Test
    fun `test that saveLink calls saveViewedLink with correct entities for folder link`() =
        runTest {
            val viewedLink = ViewedLink(
                nodeHandle = 456L,
                name = "shared-folder",
                linkUrl = "https://mega.nz/folder/def456",
                type = RecentlyUsedType.FolderLink,
                accessedTimestamp = 9000L,
            )

            underTest.saveLink(viewedLink)

            verify(recentlyViewedLinkDao).saveViewedLink(
                recentlyUsedEntity = RecentlyUsedEntity(
                    nodeHandle = 456L,
                    typeId = 6,
                    fileName = "shared-folder",
                    lastAccessedTimestamp = 9000L,
                ),
                recentlyViewedLinkEntity = RecentlyViewedLinkEntity(
                    nodeHandle = 456L,
                    linkUrl = "https://mega.nz/folder/def456",
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
}
