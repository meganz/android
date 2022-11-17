package test.mega.privacy.android.app.presentation.photos.albums.photosselection

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
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionViewModel
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetUserAlbum
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AlbumPhotosSelectionViewModelTest {
    private var underTest: AlbumPhotosSelectionViewModel? = null

    private val savedStateHandle = SavedStateHandle()
    private val getUserAlbum = mock<GetUserAlbum>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = createSUT()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that album collect behaves correctly`() = runTest {
        val id = 1L
        val expectedAlbum = createUserAlbum(
            id = AlbumId(id),
            title = "Album 1",
        )

        savedStateHandle[ALBUM_ID] = id
        whenever(getUserAlbum(expectedAlbum.id)).thenReturn(flowOf(expectedAlbum))

        underTest?.state?.drop(1)?.test {
            val actualAlbum = awaitItem().album
            assertThat(expectedAlbum).isEqualTo(actualAlbum)
        }
    }

    @Test
    fun `test that selected location is updated correctly`() = runTest {
        underTest?.state?.test {
            underTest?.updateLocation(ALL_PHOTOS)
            assertThat(awaitItem().selectedLocation).isEqualTo(ALL_PHOTOS)

            underTest?.updateLocation(CLOUD_DRIVE)
            assertThat(awaitItem().selectedLocation).isEqualTo(CLOUD_DRIVE)

            underTest?.updateLocation(CAMERA_UPLOAD)
            assertThat(awaitItem().selectedLocation).isEqualTo(CAMERA_UPLOAD)
        }
    }

    @Test
    fun `test that clear selection behaves correctly`() = runTest {
        underTest?.clearSelection()

        underTest?.state?.test {
            assertThat(awaitItem().selectedPhotoIds.isEmpty()).isTrue()
        }
    }

    private fun createSUT() = AlbumPhotosSelectionViewModel(
        savedStateHandle = savedStateHandle,
        getUserAlbum = getUserAlbum,
        defaultDispatcher = UnconfinedTestDispatcher(),
    )

    private fun createUserAlbum(
        id: AlbumId,
        title: String = "",
        cover: Photo? = null,
        modificationTime: Long = 0L,
    ) = Album.UserAlbum(id, title, cover, modificationTime)
}
