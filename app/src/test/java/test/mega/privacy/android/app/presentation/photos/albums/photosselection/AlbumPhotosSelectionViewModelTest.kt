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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionViewModel
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.AddPhotosToAlbum
import mega.privacy.android.domain.usecase.DownloadThumbnail
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.GetUserAlbum
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class AlbumPhotosSelectionViewModelTest {
    private var underTest: AlbumPhotosSelectionViewModel? = null

    private val savedStateHandle = SavedStateHandle()
    private val getUserAlbum = mock<GetUserAlbum>()
    private val getAlbumPhotos = mock<GetAlbumPhotos>()
    private val getTimelinePhotosUseCase = mock<GetTimelinePhotosUseCase>()
    private val downloadThumbnail = mock<DownloadThumbnail>()
    private val filterCloudDrivePhotos = mock<FilterCloudDrivePhotos>()
    private val filterCameraUploadPhotos = mock<FilterCameraUploadPhotos>()
    private val addPhotosToAlbum = mock<AddPhotosToAlbum>()

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
    fun `test that photos collect behaves correctly`() = runTest {
        val images = listOf(
            createImage(id = 1L),
            createImage(id = 2L),
            createImage(id = 3L),
        )
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(images))
        whenever(filterCloudDrivePhotos(any())).thenReturn(images)
        whenever(filterCameraUploadPhotos(any())).thenReturn(images)

        underTest = createSUT()

        underTest?.state?.drop(1)?.test {
            val actualPhotos = awaitItem().photos
            assertThat(actualPhotos.size).isEqualTo(3)
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
    fun `test that select all photos behaves correctly`() = runTest {
        val images = listOf(
            createImage(id = 1L),
            createImage(id = 2L),
            createImage(id = 3L),
        )
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(images))
        whenever(filterCloudDrivePhotos(any())).thenReturn(images)
        whenever(filterCameraUploadPhotos(any())).thenReturn(images)

        underTest = createSUT()
        advanceUntilIdle()

        underTest?.selectAllPhotos()

        underTest?.state?.drop(1)?.test {
            val actualSelectedPhotoIds = awaitItem().selectedPhotoIds
            assertThat(actualSelectedPhotoIds).isEqualTo(setOf(1L, 2L, 3L))
        }
    }

    @Test
    fun `test that clear selection behaves correctly`() = runTest {
        underTest?.clearSelection()

        underTest?.state?.test {
            assertThat(awaitItem().selectedPhotoIds.isEmpty()).isTrue()
        }
    }

    @Test
    fun `test that select photo behaves correctly`() = runTest {
        val image = createImage(id = 1L)

        underTest?.selectPhoto(image)

        underTest?.state?.test {
            assertThat(awaitItem().selectedPhotoIds).isEqualTo(setOf(image.id))
        }
    }

    @Test
    fun `test that unselect photo behaves correctly`() = runTest {
        val image = createImage(id = 1L)

        underTest?.selectPhoto(image)
        underTest?.unselectPhoto(image)

        underTest?.state?.test {
            assertThat(!awaitItem().selectedPhotoIds.contains(1L)).isTrue()
        }
    }

    @Test
    fun `test that add photos to album behaves correctly`() = runTest {
        val album = createUserAlbum(id = AlbumId(1L))
        val photoIds = listOf(
            NodeId(1L),
            NodeId(2L),
            NodeId(3L),
        )
        whenever(addPhotosToAlbum(album.id, photoIds)).thenReturn(Unit)

        underTest?.addPhotos(album, photoIds.map { it.longValue }.toSet())

        underTest?.state?.drop(1)?.test {
            val state = awaitItem()
            assertThat(state.isSelectionCompleted).isTrue()
        }
    }

    private fun createSUT() = AlbumPhotosSelectionViewModel(
        savedStateHandle = savedStateHandle,
        getUserAlbum = getUserAlbum,
        getAlbumPhotos = getAlbumPhotos,
        getTimelinePhotosUseCase = getTimelinePhotosUseCase,
        downloadThumbnail = downloadThumbnail,
        filterCloudDrivePhotos = filterCloudDrivePhotos,
        filterCameraUploadPhotos = filterCameraUploadPhotos,
        addPhotosToAlbum = addPhotosToAlbum,
        defaultDispatcher = UnconfinedTestDispatcher(),
    )

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
