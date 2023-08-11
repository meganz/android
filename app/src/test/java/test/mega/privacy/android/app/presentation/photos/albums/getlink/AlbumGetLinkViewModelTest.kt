package test.mega.privacy.android.app.presentation.photos.albums.getlink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.albums.getlink.AlbumGetLinkViewModel
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumIdLink
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AlbumGetLinkViewModelTest {
    private val getUserAlbumUseCase: GetUserAlbum = mock()

    private val getAlbumPhotosUseCase: GetAlbumPhotos = mock()

    private val downloadThumbnailUseCase: DownloadThumbnailUseCase = mock()

    private val exportAlbumsUseCase: ExportAlbumsUseCase = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that fetch link works correctly`() = runTest {
        // given
        val userAlbum = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )
        val expectedLink = AlbumLink("Link 1")

        val underTest = AlbumGetLinkViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ALBUM_ID to 1L)),
            getUserAlbumUseCase = getUserAlbumUseCase,
            getAlbumPhotosUseCase = getAlbumPhotosUseCase,
            downloadThumbnailUseCase = downloadThumbnailUseCase,
            exportAlbumsUseCase = exportAlbumsUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

        whenever(getUserAlbumUseCase(userAlbum.id))
            .thenReturn(flowOf(userAlbum))

        whenever(getAlbumPhotosUseCase(userAlbum.id))
            .thenReturn(flowOf(listOf()))

        whenever(exportAlbumsUseCase(listOf(userAlbum.id)))
            .thenReturn(listOf(AlbumIdLink(userAlbum.id, expectedLink)))

        // when
        underTest.initialize()

        // then
        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.link).contains(expectedLink.link)
        }
    }

    @Test
    fun `test that if album or link is invalid, it should exit screen`() = runTest {
        // given
        val userAlbum = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
        )

        val underTest = AlbumGetLinkViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ALBUM_ID to 1L)),
            getUserAlbumUseCase = getUserAlbumUseCase,
            getAlbumPhotosUseCase = getAlbumPhotosUseCase,
            downloadThumbnailUseCase = downloadThumbnailUseCase,
            exportAlbumsUseCase = exportAlbumsUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

        whenever(getUserAlbumUseCase(userAlbum.id))
            .thenReturn(flowOf())

        whenever(getAlbumPhotosUseCase(userAlbum.id))
            .thenReturn(flowOf(listOf()))

        whenever(exportAlbumsUseCase(listOf()))
            .thenReturn(listOf())

        // when
        underTest.initialize()

        // then
        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.exitScreen).isTrue()
        }
    }
}
