package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetAlbumPhotosTest {
    private lateinit var underTest: GetAlbumPhotos

    private val albumRepository = mock<AlbumRepository>()
    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetAlbumPhotos(
            albumRepository = albumRepository,
            photosRepository = photosRepository,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that album photo collect is working`() = runTest {
        val albumId = AlbumId(1L)
        val albumPhotoIds =
            (1..2L).map { AlbumPhotoId(id = it, nodeId = NodeId(it), albumId = albumId) }

        val expectedImage = createImage()
        val expectedVideo = createVideo()

        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(albumPhotoIds)
        whenever(albumRepository.monitorAlbumElementIds(albumId)).thenReturn(flowOf())

        whenever(photosRepository.getPhotoFromNodeID(albumPhotoIds[0].nodeId, albumPhotoIds[0])).thenReturn(
            expectedImage
        )
        whenever(photosRepository.getPhotoFromNodeID(albumPhotoIds[1].nodeId, albumPhotoIds[1])).thenReturn(
            expectedVideo
        )

        underTest(albumId).test {
            val actualAlbumPhotos = awaitItem()

            assertThat(actualAlbumPhotos.size).isEqualTo(2)
            assertThat(actualAlbumPhotos[0]).isEqualTo(expectedImage)
            assertThat(actualAlbumPhotos[1]).isEqualTo(expectedVideo)

            awaitComplete()
        }
    }

    private fun createImage(
        id: Long = 0L,
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

    private fun createVideo(
        id: Long = 0L,
        albumPhotoId: Long? = null,
        parentId: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        thumbnailFilePath: String? = null,
        previewFilePath: String? = null,
        duration: Int = 0,
        fileTypeInfo: VideoFileTypeInfo = VideoFileTypeInfo(mimeType = "",
            extension = "",
            duration = duration),
    ): Photo = Photo.Video(
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
