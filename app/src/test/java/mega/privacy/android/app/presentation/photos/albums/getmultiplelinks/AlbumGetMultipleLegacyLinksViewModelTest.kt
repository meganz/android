package mega.privacy.android.app.presentation.photos.albums.getmultiplelinks

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.albums.getmultiplelinks.AlbumGetMultipleLinksViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumIdLink
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.ShouldShowCopyrightUseCase
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AlbumGetMultipleLegacyLinksViewModelTest {
    private lateinit var underTest: AlbumGetMultipleLinksViewModel

    private val getUserAlbumUseCase: GetUserAlbum = mock()

    private val getAlbumPhotosUseCase: GetAlbumPhotos = mock()

    private val downloadThumbnailUseCase: DownloadThumbnailUseCase = mock()

    private val exportAlbumsUseCase: ExportAlbumsUseCase = mock()

    private val shouldShowCopyrightUseCase: ShouldShowCopyrightUseCase = mock()

    @Test
    fun `test that fetch link works correctly`() = runTest {
        whenever(shouldShowCopyrightUseCase())
            .thenReturn(false)

        underTest = AlbumGetMultipleLinksViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ALBUM_ID to longArrayOf(1L, 2L))),
            getUserAlbumUseCase = getUserAlbumUseCase,
            getAlbumPhotosUseCase = getAlbumPhotosUseCase,
            downloadThumbnailUseCase = downloadThumbnailUseCase,
            exportAlbumsUseCase = exportAlbumsUseCase,
            shouldShowCopyrightUseCase = shouldShowCopyrightUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            ioDispatcher = UnconfinedTestDispatcher(),
            getFeatureFlagValueUseCase = mock {
                onBlocking { invoke(any()) }.thenReturn(false)
            },
        )
        val userAlbum1 = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )

        val userAlbum2 = Album.UserAlbum(
            id = AlbumId(2L),
            title = "Album 2",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )

        val selectedAlbumsIds = listOf(userAlbum1.id, userAlbum2.id)

        whenever(getUserAlbumUseCase(userAlbum1.id))
            .thenReturn(flowOf(userAlbum1))

        whenever(getUserAlbumUseCase(userAlbum2.id))
            .thenReturn(flowOf(userAlbum2))

        whenever(getAlbumPhotosUseCase(userAlbum1.id))
            .thenReturn(flowOf(listOf()))

        whenever(getAlbumPhotosUseCase(userAlbum2.id))
            .thenReturn(flowOf(listOf()))

        whenever(exportAlbumsUseCase(selectedAlbumsIds))
            .thenReturn(
                listOf(
                    AlbumIdLink(userAlbum1.id, AlbumLink("Link 1")),
                    AlbumIdLink(userAlbum2.id, AlbumLink("Link 2"))
                )
            )

        // then
        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.showCopyright).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
