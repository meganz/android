package test.mega.privacy.android.app.presentation.photos.albums.coverselection

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
import mega.privacy.android.app.presentation.photos.albums.coverselection.AlbumCoverSelectionViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_ID
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.photos.UpdateAlbumCoverUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class AlbumCoverSelectionViewModelTest {

    private val updateAlbumCoverUseCase: UpdateAlbumCoverUseCase = mock<UpdateAlbumCoverUseCase>()

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that fetch album and photos returns correct result`() = runTest {
        // given
        val expectedAlbum = createUserAlbum(
            id = AlbumId(1L),
            title = "Album 1",
        )

        val expectedPhotos = listOf(
            createImage(id = 1L),
            createImage(id = 2L),
            createImage(id = 3L),
        )

        // when
        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ALBUM_ID to 1L)),
            getUserAlbum = { flowOf(expectedAlbum) },
            getAlbumPhotos = { flowOf(expectedPhotos) },
            downloadThumbnail = { _, _ -> },
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )

        // then
        underTest.state.drop(1).test {
            val actualAlbum = awaitItem().album
            assertThat(expectedAlbum).isEqualTo(actualAlbum)

            val actualPhotos = awaitItem().photos
            assertThat(expectedPhotos.sortedByDescending { it.modificationTime })
                .isEqualTo(actualPhotos)
        }
    }

    @Test
    fun `test that select photo returns correct result`() = runTest {
        // given
        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            getUserAlbum = { flowOf() },
            getAlbumPhotos = { flowOf() },
            downloadThumbnail = { _, _ -> },
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )

        val expectedPhoto = createImage(id = 1L)

        // when
        underTest.selectPhoto(expectedPhoto)

        // then
        underTest.state.test {
            val actualPhoto = awaitItem().selectedPhoto
            assertThat(expectedPhoto).isEqualTo(actualPhoto)
        }
    }

    @Test
    fun `test that update cover returns correct result`() = runTest {
        // given
        val underTest = AlbumCoverSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            getUserAlbum = { flowOf() },
            getAlbumPhotos = { flowOf() },
            downloadThumbnail = { _, _ -> },
            updateAlbumCoverUseCase = updateAlbumCoverUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )

        val album = createUserAlbum(id = AlbumId(1L))
        val photo = createImage(id = 2L, albumPhotoId = 2L)

        // when
        underTest.updateCover(album, photo)

        // then
        underTest.state.drop(1).test {
            val isCompleted = awaitItem().isSelectionCompleted
            assertThat(isCompleted).isTrue()
        }
    }

    private fun createUserAlbum(
        id: AlbumId,
        title: String = "",
        cover: Photo? = null,
        creationTime: Long = 0L,
        modificationTime: Long = 0L,
        isExported: Boolean = false,
    ) = Album.UserAlbum(id, title, cover, creationTime, modificationTime, isExported)

    private fun createImage(
        id: Long,
        albumPhotoId: Long? = null,
        parentId: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        thumbnailFilePath: String? = null,
        previewFilePath: String? = null,
        fileTypeInfo: FileTypeInfo = UnknownFileTypeInfo(mimeType = "", extension = ""),
    ): Photo = Photo.Image(
        id,
        albumPhotoId,
        parentId,
        name,
        isFavourite,
        creationTime,
        modificationTime,
        thumbnailFilePath,
        previewFilePath,
        fileTypeInfo,
    )
}
