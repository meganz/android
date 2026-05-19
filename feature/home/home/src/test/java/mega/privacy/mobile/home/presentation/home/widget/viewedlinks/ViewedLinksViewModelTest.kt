package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.asSnapshot
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.viewedlinks.ClearViewedLinksUseCase
import mega.privacy.android.domain.usecase.viewedlinks.MonitorViewedLinksSortPreferenceUseCase
import mega.privacy.android.domain.usecase.viewedlinks.MonitorViewedLinksUseCase
import mega.privacy.android.domain.usecase.viewedlinks.SetViewedLinksSortUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.mapper.ViewedLinksSortMapper
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

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ViewedLinksViewModelTest {
    private val monitorViewedLinksUseCase = mock<MonitorViewedLinksUseCase>()
    private val monitorViewedLinksSortPreferenceUseCase =
        mock<MonitorViewedLinksSortPreferenceUseCase>()
    private val setViewedLinksSortUseCase = mock<SetViewedLinksSortUseCase>()
    private val viewedLinksSortMapper = mock<ViewedLinksSortMapper>()
    private val getPublicNodeUseCase = mock<GetPublicNodeUseCase>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val clearViewedLinksUseCase = mock<ClearViewedLinksUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val monitorViewTypeUseCase = mock<MonitorViewType>()
    private val setViewTypeUseCase = mock<SetViewType>()

    private lateinit var underTest: ViewedLinksViewModel

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorViewedLinksUseCase,
            monitorViewedLinksSortPreferenceUseCase,
            setViewedLinksSortUseCase,
            viewedLinksSortMapper,
            getPublicNodeUseCase,
            fileTypeIconMapper,
            clearViewedLinksUseCase,
            snackbarEventQueue,
            monitorViewTypeUseCase,
            setViewTypeUseCase,
        )
    }

    private fun initViewModel(
        links: List<ViewedLink>,
        sortField: ViewedLinksSortField = ViewedLinksSortField.LastAccessed,
        sortDirection: SortDirection = SortDirection.Descending,
        viewType: ViewType = ViewType.LIST,
    ) {
        whenever(monitorViewedLinksSortPreferenceUseCase())
            .thenReturn(flowOf(sortField to sortDirection))
        whenever(monitorViewedLinksUseCase(any(), any())).thenAnswer { fakePagingSource(links) }
        whenever(monitorViewTypeUseCase()).thenReturn(flowOf(viewType))
        whenever(viewedLinksSortMapper(any<NodeSortOption>())).thenAnswer { invocation ->
            when (invocation.getArgument<NodeSortOption>(0)) {
                NodeSortOption.Name -> ViewedLinksSortField.Name
                else -> ViewedLinksSortField.LastAccessed
            }
        }
        whenever(
            viewedLinksSortMapper(
                any<ViewedLinksSortField>(),
                any()
            )
        ).thenAnswer { invocation ->
            val field = invocation.getArgument<ViewedLinksSortField>(0)
            val direction = invocation.getArgument<SortDirection>(1)
            NodeSortConfiguration(
                sortOption = when (field) {
                    ViewedLinksSortField.Name -> NodeSortOption.Name
                    ViewedLinksSortField.LastAccessed -> NodeSortOption.LastAccessed
                },
                sortDirection = direction,
            )
        }
        underTest = ViewedLinksViewModel(
            monitorViewedLinksUseCase = monitorViewedLinksUseCase,
            monitorViewedLinksSortPreferenceUseCase = monitorViewedLinksSortPreferenceUseCase,
            setViewedLinksSortUseCase = setViewedLinksSortUseCase,
            viewedLinksSortMapper = viewedLinksSortMapper,
            getPublicNodeUseCase = getPublicNodeUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            clearViewedLinksUseCase = clearViewedLinksUseCase,
            snackbarEventQueue = snackbarEventQueue,
            monitorViewTypeUseCase = monitorViewTypeUseCase,
            setViewTypeUseCase = setViewTypeUseCase,
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

    @Test
    fun `test that clearing all viewed links triggers clearAllLinksEvent`() = runTest {
        initViewModel(emptyList())
        underTest.clearAllLinks()

        underTest.uiState.test {
            assertThat(awaitItem().clearAllLinksEvent).isEqualTo(triggered)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onClearAllLinksEventConsumed resets the event to consumed`() = runTest {
        initViewModel(emptyList())
        underTest.clearAllLinks()
        underTest.onClearAllLinksEventConsumed()

        underTest.uiState.test {
            assertThat(awaitItem().clearAllLinksEvent).isEqualTo(consumed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that pagedItems uses sort preference for the paging factory`() = runTest {
        initViewModel(
            links = emptyList(),
            sortField = ViewedLinksSortField.Name,
            sortDirection = SortDirection.Ascending,
        )

        underTest.pagedItems.asSnapshot()

        verify(monitorViewedLinksUseCase)
            .invoke(ViewedLinksSortField.Name, SortDirection.Ascending)
    }

    @Test
    fun `test that uiState reflects persisted sort preference`() = runTest {
        initViewModel(
            links = emptyList(),
            sortField = ViewedLinksSortField.Name,
            sortDirection = SortDirection.Descending,
        )

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.sortConfiguration.sortOption).isEqualTo(NodeSortOption.Name)
            assertThat(state.sortConfiguration.sortDirection)
                .isEqualTo(SortDirection.Descending)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that updateSortConfiguration calls setViewedLinksSortUseCase with mapped field`() =
        runTest {
            initViewModel(emptyList())

            underTest.updateSortConfiguration(
                NodeSortConfiguration(NodeSortOption.Name, SortDirection.Descending)
            )

            verify(setViewedLinksSortUseCase)
                .invoke(ViewedLinksSortField.Name, SortDirection.Descending)
        }

    @Test
    fun `test that updateSortConfiguration maps LastAccessed to LastAccessed field`() = runTest {
        initViewModel(emptyList())

        underTest.updateSortConfiguration(
            NodeSortConfiguration(NodeSortOption.LastAccessed, SortDirection.Ascending)
        )

        verify(setViewedLinksSortUseCase)
            .invoke(ViewedLinksSortField.LastAccessed, SortDirection.Ascending)
    }

    @Test
    fun `test that uiState reflects view type emitted by monitorViewTypeUseCase`() = runTest {
        initViewModel(links = emptyList(), viewType = ViewType.GRID)

        underTest.uiState.test {
            awaitViewType(ViewType.GRID)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that changeViewType calls setViewTypeUseCase with GRID when current is LIST`() =
        runTest {
            initViewModel(links = emptyList(), viewType = ViewType.LIST)

            underTest.uiState.test {
                awaitViewType(ViewType.LIST)
                underTest.changeViewType()
                cancelAndIgnoreRemainingEvents()
            }

            verify(setViewTypeUseCase).invoke(ViewType.GRID)
        }

    @Test
    fun `test that changeViewType calls setViewTypeUseCase with LIST when current is GRID`() =
        runTest {
            initViewModel(links = emptyList(), viewType = ViewType.GRID)

            underTest.uiState.test {
                awaitViewType(ViewType.GRID)
                underTest.changeViewType()
                cancelAndIgnoreRemainingEvents()
            }

            verify(setViewTypeUseCase).invoke(ViewType.LIST)
        }

    private suspend fun ReceiveTurbine<ViewedLinksUiState>.awaitViewType(expected: ViewType) {
        var state = awaitItem()
        while (state.currentViewType != expected) {
            state = awaitItem()
        }
        assertThat(state.currentViewType).isEqualTo(expected)
    }

    private fun fakePagingSource(items: List<ViewedLink>): PagingSource<Int, ViewedLink> =
        object : PagingSource<Int, ViewedLink>() {
            override fun getRefreshKey(state: PagingState<Int, ViewedLink>): Int? = null
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ViewedLink> =
                LoadResult.Page(data = items, prevKey = null, nextKey = null)
        }
}
