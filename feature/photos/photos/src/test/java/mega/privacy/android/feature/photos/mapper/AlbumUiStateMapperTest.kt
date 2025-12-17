package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumUiStateMapperTest {

    private lateinit var underTest: AlbumUiStateMapper
    private val mockPhotoUiStateMapper: PhotoUiStateMapper = mock()

    @BeforeAll
    fun setup() {
        underTest = AlbumUiStateMapper(mockPhotoUiStateMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(mockPhotoUiStateMapper)
    }

    @Test
    fun `test that system album is mapped correctly without cover`() {
        val albumName = "Favourites"
        val systemAlbum = createMockSystemAlbum(albumName) { true }
        val mediaAlbum = MediaAlbum.System(
            id = systemAlbum,
            cover = null
        )

        val actual = underTest(mediaAlbum)

        val expected = AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = albumName,
            isExported = false,
            cover = null
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that system album is mapped correctly with cover`() {
        val albumName = "GIFs"
        val systemAlbum = createMockSystemAlbum(albumName) { true }
        val coverPhoto = newImage(id = 100L, name = "cover.jpg")
        val photoUiState = PhotoUiState.Image(
            id = 100L,
            albumPhotoId = null,
            parentId = 0L,
            name = "cover.jpg",
            isFavourite = false,
            creationTime = coverPhoto.creationTime,
            modificationTime = coverPhoto.modificationTime,
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = coverPhoto.fileTypeInfo,
            size = 0L,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false,
            base64Id = null,
        )

        whenever(mockPhotoUiStateMapper(coverPhoto)).thenReturn(photoUiState)

        val mediaAlbum = MediaAlbum.System(
            id = systemAlbum,
            cover = coverPhoto
        )

        val actual = underTest(mediaAlbum)

        val expected = AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = albumName,
            isExported = false,
            cover = photoUiState
        )
        assertThat(actual).isEqualTo(expected)
        verify(mockPhotoUiStateMapper).invoke(coverPhoto)
    }

    @Test
    fun `test that user album is mapped correctly without cover`() {
        val albumId = AlbumId(123L)
        val title = "My Album"
        val mediaAlbum = MediaAlbum.User(
            id = albumId,
            title = title,
            creationTime = 1000L,
            modificationTime = 2000L,
            isExported = false,
            cover = null
        )

        val actual = underTest(mediaAlbum)

        val expected = AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = title,
            isExported = false,
            cover = null
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that user album is mapped correctly with cover`() {
        val albumId = AlbumId(456L)
        val title = "Vacation Photos"
        val coverPhoto = newImage(id = 200L, name = "vacation_cover.jpg")
        val photoUiState = PhotoUiState.Image(
            id = 200L,
            albumPhotoId = null,
            parentId = 0L,
            name = "vacation_cover.jpg",
            isFavourite = false,
            creationTime = coverPhoto.creationTime,
            modificationTime = coverPhoto.modificationTime,
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = coverPhoto.fileTypeInfo,
            size = 0L,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false,
            base64Id = null,
        )

        whenever(mockPhotoUiStateMapper(coverPhoto)).thenReturn(photoUiState)

        val mediaAlbum = MediaAlbum.User(
            id = albumId,
            title = title,
            creationTime = 1500L,
            modificationTime = 2500L,
            isExported = true,
            cover = coverPhoto
        )

        val actual = underTest(mediaAlbum)

        val expected = AlbumUiState(
            mediaAlbum = mediaAlbum,
            title = title,
            isExported = true,
            cover = photoUiState
        )
        assertThat(actual).isEqualTo(expected)
        verify(mockPhotoUiStateMapper).invoke(coverPhoto)
    }

    @Test
    fun `test that system album id is hashcode of album name`() {
        val albumName = "RAW"
        val systemAlbum = createMockSystemAlbum(albumName) { true }
        val mediaAlbum = MediaAlbum.System(
            id = systemAlbum,
            cover = null
        )

        val actual = underTest(mediaAlbum)

        assertThat(actual.mediaAlbum).isEqualTo(mediaAlbum)
        assertThat(actual.title).isEqualTo(albumName)
    }

    @Test
    fun `test that user album id is the album id value`() {
        val albumId = AlbumId(789L)
        val title = "Test Album"
        val mediaAlbum = MediaAlbum.User(
            id = albumId,
            title = title,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
            cover = null
        )

        val actual = underTest(mediaAlbum)

        assertThat(actual.mediaAlbum).isEqualTo(mediaAlbum)
        assertThat(actual.title).isEqualTo(title)
    }

    private fun createMockSystemAlbum(name: String, filter: (Photo) -> Boolean): SystemAlbum {
        return object : SystemAlbum {
            override val albumName: String = name
            override suspend fun filter(photo: Photo): Boolean = filter(photo)
        }
    }

    private fun newImage(
        id: Long = 0L,
        albumPhotoId: Long? = null,
        parentId: Long = 0L,
        name: String = "",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        thumbnailFilePath: String? = null,
        previewFilePath: String? = null,
        fileTypeInfo: FileTypeInfo = mock<StaticImageFileTypeInfo>(),
        base64Id: String? = null,
        size: Long = 0L,
        isTakenDown: Boolean = false,
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ) = Photo.Image(
        id = id,
        albumPhotoId = albumPhotoId,
        parentId = parentId,
        name = name,
        isFavourite = isFavourite,
        creationTime = creationTime,
        modificationTime = modificationTime,
        thumbnailFilePath = thumbnailFilePath,
        previewFilePath = previewFilePath,
        fileTypeInfo = fileTypeInfo,
        size = size,
        isTakenDown = isTakenDown,
        isSensitive = isSensitive,
        isSensitiveInherited = isSensitiveInherited,
        base64Id = base64Id,
    )
}
