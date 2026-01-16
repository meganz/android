package mega.privacy.android.feature.photos.presentation.albums.photosselection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.AddPhotosToAlbum
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.MonitorPaginatedTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class AlbumPhotosSelectionViewModelTest {
    private lateinit var underTest: AlbumPhotosSelectionViewModel

    private val savedStateHandle = SavedStateHandle()
    private val getUserAlbum = mock<GetUserAlbum>()
    private val getAlbumPhotos = mock<GetAlbumPhotos>()
    private val getTimelinePhotosUseCase = mock<GetTimelinePhotosUseCase>()
    private val downloadThumbnailUseCase = mock<DownloadThumbnailUseCase>()
    private val filterCloudDrivePhotos = mock<FilterCloudDrivePhotos>()
    private val filterCameraUploadPhotos = mock<FilterCameraUploadPhotos>()
    private val addPhotosToAlbum = mock<AddPhotosToAlbum>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorPaginatedTimelinePhotosUseCase =
        mock<MonitorPaginatedTimelinePhotosUseCase>()
    private val photoUiStateMapper = mock<PhotoUiStateMapper>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()

    private val accountLevelDetail = mock<AccountLevelDetail> {
        on { accountType }.thenReturn(AccountType.PRO_III)
    }
    private val accountDetail = mock<AccountDetail> {
        on { levelDetail }.thenReturn(accountLevelDetail)
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorShowHiddenItemsUseCase() }.thenReturn(flowOf(false))
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(flowOf(accountDetail))
        whenever(
            fileTypeIconMapper(
                any(),
                any()
            )
        ).thenReturn(iconPackR.drawable.ic_image_medium_solid)
        underTest = createSUT()
    }

    @Test
    fun `test that album collect behaves correctly`() = runTest {
        val id = 1L
        val expectedAlbum = createUserAlbum(
            id = AlbumId(id),
            title = "Album 1",
        )

        savedStateHandle["album_id"] = id
        whenever(getUserAlbum(expectedAlbum.id)).thenReturn(flowOf(expectedAlbum))
        whenever(getAlbumPhotos(expectedAlbum.id)).thenReturn(flowOf(emptyList()))
        underTest = createSUT(albumId = id)
        advanceUntilIdle()

        underTest.state.test {
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
        val photoUiStates = images.map { createPhotoUiState(it) }
        images.forEachIndexed { index, photo ->
            whenever(photoUiStateMapper(photo)).thenReturn(photoUiStates[index])
        }
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(images))
        whenever(filterCloudDrivePhotos(any())).thenReturn(images)
        whenever(filterCameraUploadPhotos(any())).thenReturn(images)

        underTest = createSUT()
        advanceUntilIdle()

        underTest.state.test {
            // Wait for photos to be loaded
            var state = awaitItem()
            while (state.photosNodeContentTypes.isEmpty() || state.isLoading) {
                state = awaitItem()
            }
            val photoNodes = state.photosNodeContentTypes
                .filterIsInstance<PhotosNodeContentType.PhotoNodeItem>()
            assertThat(photoNodes.size).isEqualTo(3)
        }
    }

    @Test
    fun `test that select all photos behaves correctly`() = runTest {
        val images = listOf(
            createImage(id = 1L),
            createImage(id = 2L),
            createImage(id = 3L),
        )
        val photoUiStates = images.map { createPhotoUiState(it) }
        images.forEachIndexed { index, photo ->
            whenever(photoUiStateMapper(photo)).thenReturn(photoUiStates[index])
        }
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(images))
        whenever(filterCloudDrivePhotos(any())).thenReturn(images)
        whenever(filterCameraUploadPhotos(any())).thenReturn(images)

        underTest = createSUT()
        advanceUntilIdle()

        // Wait for photos to be loaded and photosNodeContentTypes to be populated
        underTest.state.test {
            var state = awaitItem()
            var photoNodes = state.photosNodeContentTypes
                .filterIsInstance<PhotosNodeContentType.PhotoNodeItem>()
            while (photoNodes.size < 3 || state.isLoading) {
                state = awaitItem()
                photoNodes = state.photosNodeContentTypes
                    .filterIsInstance<PhotosNodeContentType.PhotoNodeItem>()
            }
            // Verify photos are loaded before selecting
            assertThat(photoNodes.size).isEqualTo(3)
            cancelAndIgnoreRemainingEvents()
        }

        underTest.selectAllPhotos()
        advanceUntilIdle()

        underTest.state.test {
            var state = awaitItem()
            // Wait for selection to be applied - selectAllPhotos adds photo IDs to selectedPhotoIds
            // We need to wait until the selectedPhotoIds contains all 3 photo IDs
            while (state.selectedPhotoIds.size < 3) {
                state = awaitItem()
            }
            val actualSelectedPhotoIds = state.selectedPhotoIds
            assertThat(actualSelectedPhotoIds).isEqualTo(setOf(1L, 2L, 3L))
        }
    }

    @Test
    fun `test that clear selection behaves correctly`() = runTest {
        underTest.clearSelection()

        underTest.state.test {
            assertThat(awaitItem().selectedPhotoIds.isEmpty()).isTrue()
        }
    }

    @Test
    fun `test that select photo behaves correctly`() = runTest {
        val image = createImage(id = 1L)
        val photoUiState = createPhotoUiState(image)
        whenever(photoUiStateMapper(image)).thenReturn(photoUiState)
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf(image)))
        whenever(filterCloudDrivePhotos(any())).thenReturn(listOf(image))
        whenever(filterCameraUploadPhotos(any())).thenReturn(listOf(image))

        underTest = createSUT()
        advanceUntilIdle()

        underTest.selectPhoto(photoUiState)

        underTest.state.test {
            assertThat(awaitItem().selectedPhotoIds).isEqualTo(setOf(image.id))
        }
    }

    @Test
    fun `test that unselect photo behaves correctly`() = runTest {
        val image = createImage(id = 1L)
        val photoUiState = createPhotoUiState(image)
        whenever(photoUiStateMapper(image)).thenReturn(photoUiState)
        whenever(getTimelinePhotosUseCase()).thenReturn(flowOf(listOf(image)))
        whenever(filterCloudDrivePhotos(any())).thenReturn(listOf(image))
        whenever(filterCameraUploadPhotos(any())).thenReturn(listOf(image))

        underTest = createSUT()
        advanceUntilIdle()

        underTest.selectPhoto(photoUiState)
        underTest.unselectPhoto(photoUiState)

        underTest.state.test {
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
        whenever(addPhotosToAlbum(album.id, photoIds, false)).thenReturn(Unit)

        underTest = createSUT()
        underTest.addPhotos(album, photoIds.map { it.longValue }.toSet())
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.photosSelectionCompletedEvent).isEqualTo(triggered(3))
        }
    }

    @Test
    fun `test that on pagination enabled should monitor paginated photos`() = runTest {
        val images = listOf(
            createImage(id = 1L),
            createImage(id = 2L),
            createImage(id = 3L),
        )
        val photoUiStates = images.map { createPhotoUiState(it) }
        images.forEachIndexed { index, photo ->
            whenever(photoUiStateMapper(photo)).thenReturn(photoUiStates[index])
        }
        whenever(getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)).thenReturn(true)
        whenever(monitorPaginatedTimelinePhotosUseCase()).thenReturn(flowOf(images))
        whenever(filterCloudDrivePhotos(any())).thenReturn(images)
        whenever(filterCameraUploadPhotos(any())).thenReturn(images)

        underTest = createSUT()
        advanceUntilIdle()

        underTest.state.test {
            // Wait for photos to be loaded
            var state = awaitItem()
            while (state.photosNodeContentTypes.isEmpty() || state.isLoading) {
                state = awaitItem()
            }
            val photoNodes = state.photosNodeContentTypes
                .filterIsInstance<PhotosNodeContentType.PhotoNodeItem>()
            assertThat(photoNodes.size).isEqualTo(3)
        }
    }

    private fun createSUT(albumId: Long? = null, selectionMode: Int? = null) =
        AlbumPhotosSelectionViewModel(
            savedStateHandle = savedStateHandle,
            getUserAlbum = getUserAlbum,
            getAlbumPhotos = getAlbumPhotos,
            getTimelinePhotosUseCase = getTimelinePhotosUseCase,
            monitorPaginatedTimelinePhotosUseCase = monitorPaginatedTimelinePhotosUseCase,
            downloadThumbnailUseCase = downloadThumbnailUseCase,
            filterCloudDrivePhotos = filterCloudDrivePhotos,
            filterCameraUploadPhotos = filterCameraUploadPhotos,
            addPhotosToAlbum = addPhotosToAlbum,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            photoUiStateMapper = photoUiStateMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            albumId = albumId,
            selectionMode = selectionMode,
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

    private fun createPhotoUiState(photo: Photo): PhotoUiState.Image {
        return PhotoUiState.Image(
            id = photo.id,
            albumPhotoId = photo.albumPhotoId,
            parentId = photo.parentId,
            name = photo.name,
            isFavourite = photo.isFavourite,
            creationTime = photo.creationTime,
            modificationTime = photo.modificationTime,
            thumbnailFilePath = photo.thumbnailFilePath,
            previewFilePath = photo.previewFilePath,
            fileTypeInfo = photo.fileTypeInfo,
            base64Id = photo.base64Id,
            size = photo.size,
            isTakenDown = photo.isTakenDown,
            isSensitive = photo.isSensitive,
            isSensitiveInherited = photo.isSensitiveInherited,
        )
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
