package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetUserAlbumsTest {
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
        val nodeIds = (1..2L).map { NodeId(it) }

        val expectedImage = createImage()
        val expectedVideo = createVideo()

        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(nodeIds)
        whenever(photosRepository.getPhotoFromNodeID(nodeIds[0])).thenReturn(expectedImage)
        whenever(photosRepository.getPhotoFromNodeID(nodeIds[1])).thenReturn(expectedVideo)

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
        parentId: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        thumbnailFilePath: String? = null,
        previewFilePath: String? = null,
        fileTypeInfo: FileTypeInfo = UnknownFileTypeInfo(type = "", extension = ""),
    ): Photo = Photo.Image(
        id,
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
        parentId: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        thumbnailFilePath: String? = null,
        previewFilePath: String? = null,
        duration: Int = 0,
        fileTypeInfo: FileTypeInfo = UnknownFileTypeInfo(type = "", extension = ""),
    ): Photo = Photo.Video(
        id,
        parentId,
        name,
        isFavourite,
        creationTime,
        modificationTime,
        thumbnailFilePath,
        previewFilePath,
        duration,
        fileTypeInfo,
    )
}
