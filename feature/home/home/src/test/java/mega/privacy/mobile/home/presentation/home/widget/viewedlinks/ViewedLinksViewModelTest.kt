package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.asSnapshot
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.viewedlinks.ClearViewedLinksUseCase
import mega.privacy.android.domain.usecase.viewedlinks.MonitorViewedLinksUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/**
 * Tests for [ViewedLinksViewModel].
 *
 * Verifies that:
 * - File links resolve icon and preview path via [GetPublicNodeUseCase]
 * - Folder links use a static folder icon with no preview
 * - Unresolvable file links fall back to extension-based icon via [FileTypeIconMapper]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ViewedLinksViewModelTest {
    private val monitorViewedLinksUseCase = mock<MonitorViewedLinksUseCase>()
    private val getPublicNodeUseCase = mock<GetPublicNodeUseCase>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val clearViewedLinksUseCase = mock<ClearViewedLinksUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    private lateinit var underTest: ViewedLinksViewModel

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorViewedLinksUseCase,
            getPublicNodeUseCase,
            fileTypeIconMapper,
            clearViewedLinksUseCase,
            snackbarEventQueue,
        )
    }

    private fun initViewModel(links: List<ViewedLink>) {
        whenever(monitorViewedLinksUseCase()).thenAnswer { fakePagingSource(links) }
        underTest = ViewedLinksViewModel(
            monitorViewedLinksUseCase = monitorViewedLinksUseCase,
            getPublicNodeUseCase = getPublicNodeUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            clearViewedLinksUseCase = clearViewedLinksUseCase,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @Test
    fun `test that file link resolves icon and preview from GetPublicNodeUseCase`() = runTest {
        val viewedLink = ViewedLink(
            nodeHandle = 1L,
            name = "test.pdf",
            linkUrl = "https://mega.nz/file/abc",
            type = RecentlyViewedLinkType.FileLink,
            accessedTimestamp = 1000L,
        )
        val typedFileNode = mock<TypedFileNode> {
            on { previewPath }.thenReturn("/cache/preview.jpg")
            on { type }.thenReturn(PdfFileTypeInfo)
        }

        whenever(getPublicNodeUseCase("https://mega.nz/file/abc")).thenReturn(typedFileNode)
        whenever(fileTypeIconMapper(any(), any()))
            .thenReturn(iconPackR.drawable.ic_pdf_medium_solid)

        initViewModel(listOf(viewedLink))

        val items = underTest.pagedItems.asSnapshot()
        assertThat(items).hasSize(1)
        assertThat(items[0].previewPath).isEqualTo("/cache/preview.jpg")
        assertThat(items[0].iconRes).isEqualTo(iconPackR.drawable.ic_pdf_medium_solid)
        verify(fileTypeIconMapper).invoke(eq("pdf"), any())
    }

    @Test
    fun `test that folder link uses folder icon and null preview`() = runTest {
        val folderLink = ViewedLink(
            nodeHandle = 2L,
            name = "My Folder",
            linkUrl = "https://mega.nz/folder/def",
            type = RecentlyViewedLinkType.FolderLink,
            accessedTimestamp = 2000L,
        )

        initViewModel(listOf(folderLink))

        val items = underTest.pagedItems.asSnapshot()
        assertThat(items).hasSize(1)
        assertThat(items[0].previewPath).isNull()
        assertThat(items[0].iconRes)
            .isEqualTo(iconPackR.drawable.ic_folder_users_small_solid)
        verifyNoMoreInteractions(getPublicNodeUseCase)
        verifyNoMoreInteractions(fileTypeIconMapper)
    }

    @Test
    fun `test that unresolvable file link falls back to file type icon`() = runTest {
        val expectedIcon = iconPackR.drawable.ic_video_medium_solid
        val viewedLink = ViewedLink(
            nodeHandle = 1L,
            name = "video.mp4",
            linkUrl = "https://mega.nz/file/xyz",
            type = RecentlyViewedLinkType.FileLink,
            accessedTimestamp = 1000L,
        )

        whenever(getPublicNodeUseCase("https://mega.nz/file/xyz"))
            .thenThrow(RuntimeException("Not found"))
        whenever(fileTypeIconMapper(any(), any())).thenReturn(expectedIcon)

        initViewModel(listOf(viewedLink))

        val items = underTest.pagedItems.asSnapshot()
        assertThat(items).hasSize(1)
        assertThat(items[0].previewPath).isNull()
        assertThat(items[0].iconRes).isEqualTo(expectedIcon)
        verify(fileTypeIconMapper).invoke(eq("mp4"), any())
    }

    @Test
    fun `test that empty paging data emits empty snapshot`() = runTest {
        initViewModel(emptyList())

        val items = underTest.pagedItems.asSnapshot()
        assertThat(items).isEmpty()
        verifyNoMoreInteractions(getPublicNodeUseCase)
        verifyNoMoreInteractions(fileTypeIconMapper)
    }

    @Test
    fun `test that clearing all viewed links invokes clearViewedLinksUseCase`() = runTest {
        initViewModel(emptyList())
        underTest.clearAllLinks()
        verify(clearViewedLinksUseCase).invoke()
    }

    @Test
    fun `test that clearing all viewed links queues snackbar message when use case succeeds`() =
        runTest {
            initViewModel(emptyList())
            underTest.clearAllLinks()
            verify(snackbarEventQueue)
                .queueMessage(sharedR.string.home_widget_viewed_links_clear_history_success_message)
        }

    private fun fakePagingSource(items: List<ViewedLink>): PagingSource<Int, ViewedLink> =
        object : PagingSource<Int, ViewedLink>() {
            override fun getRefreshKey(state: PagingState<Int, ViewedLink>): Int? = null
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ViewedLink> =
                LoadResult.Page(data = items, prevKey = null, nextKey = null)
        }
}
