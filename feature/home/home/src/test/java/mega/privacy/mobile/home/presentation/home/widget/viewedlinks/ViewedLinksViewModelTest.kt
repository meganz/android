package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.viewedlinks.ClearViewedLinksUseCase
import mega.privacy.android.domain.usecase.viewedlinks.MonitorViewedLinksUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
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
    private val fakeFlow = MutableSharedFlow<List<ViewedLink>>()

    private lateinit var underTest: ViewedLinksViewModel

    @BeforeAll
    fun setUp() {
        whenever(monitorViewedLinksUseCase()).thenReturn(fakeFlow)
        underTest = ViewedLinksViewModel(
            monitorViewedLinksUseCase = monitorViewedLinksUseCase,
            getPublicNodeUseCase = getPublicNodeUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            clearViewedLinksUseCase = clearViewedLinksUseCase,
            snackbarEventQueue = snackbarEventQueue
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getPublicNodeUseCase, fileTypeIconMapper, clearViewedLinksUseCase, snackbarEventQueue)
    }

    @Test
    fun `test that file link resolves icon and preview from GetPublicNodeUseCase`() = runTest {
        val viewedLink = ViewedLink(
            nodeHandle = 1L,
            name = "test.pdf",
            linkUrl = "https://mega.nz/file/abc",
            type = RecentlyUsedType.FileLink,
            accessedTimestamp = 1000L,
        )
        val typedFileNode = mock<TypedFileNode> {
            on { previewPath }.thenReturn("/cache/preview.jpg")
            on { type }.thenReturn(PdfFileTypeInfo)
        }

        whenever(getPublicNodeUseCase("https://mega.nz/file/abc")).thenReturn(typedFileNode)
        whenever(
            fileTypeIconMapper(
                any(),
                any()
            )
        ).thenReturn(iconPackR.drawable.ic_pdf_medium_solid)

        underTest.uiState.test {
            awaitItem()
            fakeFlow.emit(listOf(viewedLink))
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].previewPath).isEqualTo("/cache/preview.jpg")
            assertThat(state.items[0].iconRes).isEqualTo(iconPackR.drawable.ic_pdf_medium_solid)
            verify(fileTypeIconMapper).invoke(eq("pdf"), any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that folder link uses folder icon and null preview`() = runTest {
        val folderLink = ViewedLink(
            nodeHandle = 2L,
            name = "My Folder",
            linkUrl = "https://mega.nz/folder/def",
            type = RecentlyUsedType.FolderLink,
            accessedTimestamp = 2000L,
        )

        underTest.uiState.test {
            awaitItem()
            fakeFlow.emit(listOf(folderLink))
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].previewPath).isNull()
            assertThat(state.items[0].iconRes)
                .isEqualTo(iconPackR.drawable.ic_folder_users_small_solid)
            verifyNoMoreInteractions(getPublicNodeUseCase)
            verifyNoMoreInteractions(fileTypeIconMapper)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that unresolvable file link falls back to file type icon`() = runTest {
        val expectedIcon = iconPackR.drawable.ic_video_medium_solid
        val viewedLink = ViewedLink(
            nodeHandle = 1L,
            name = "video.mp4",
            linkUrl = "https://mega.nz/file/xyz",
            type = RecentlyUsedType.FileLink,
            accessedTimestamp = 1000L,
        )

        whenever(
            getPublicNodeUseCase("https://mega.nz/file/xyz")
        ).thenThrow(RuntimeException("Not found"))
        whenever(fileTypeIconMapper(any(), any())).thenReturn(expectedIcon)

        underTest.uiState.test {
            awaitItem() // consume current/stale StateFlow value
            fakeFlow.emit(listOf(viewedLink))
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
            assertThat(state.items[0].previewPath).isNull()
            assertThat(state.items[0].iconRes).isEqualTo(expectedIcon)
            verify(fileTypeIconMapper).invoke(eq("mp4"), any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that empty list emits empty state`() = runTest {
        underTest.uiState.test {
            awaitItem()
            fakeFlow.emit(emptyList())
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).isEmpty()
            verifyNoMoreInteractions(getPublicNodeUseCase)
            verifyNoMoreInteractions(fileTypeIconMapper)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that clearing all viewed links invokes use case and snackbar `() = runTest {
        underTest.clearAllLinks()

        verify(clearViewedLinksUseCase).invoke()
        verify(snackbarEventQueue).queueMessage(sharedR.string.home_widget_viewed_links_clear_history_success_message)
    }

    @Test
    fun `test that clearing all viewed links invokes clearViewedLinksUseCase`() = runTest {
        underTest.clearAllLinks()
        verify(clearViewedLinksUseCase).invoke()
    }

    @Test
    fun `test that clearing all viewed links queues snackbar message when use case succeeds`() =
        runTest {
            underTest.clearAllLinks()
            verify(snackbarEventQueue).queueMessage(sharedR.string.home_widget_viewed_links_clear_history_success_message)
        }

}
