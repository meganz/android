package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumTitle
import mega.privacy.android.feature.photos.presentation.albums.model.FavouriteSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.GifSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.RawSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UIAlbumMapperTest {

    private lateinit var underTest: UIAlbumMapper

    @BeforeAll
    fun setup() {
        underTest = UIAlbumMapper()
    }

    @Test
    fun `test that FavouriteAlbum is mapped correctly with resource title`() {
        val count = 10
        val imageCount = 7
        val videoCount = 3
        val cover = createPhoto(id = 1L, name = "cover.jpg")
        val defaultCover = createPhoto(id = 2L, name = "default_cover.jpg")

        val result = underTest(
            count = count,
            imageCount = imageCount,
            videoCount = videoCount,
            cover = cover,
            defaultCover = defaultCover,
            album = Album.FavouriteAlbum
        )

        assertThat(result.id).isEqualTo(Album.FavouriteAlbum)
        assertThat(result.title).isEqualTo(AlbumTitle.ResourceTitle(sharedResR.string.system_album_favourites_title))
        assertThat(result.count).isEqualTo(count)
        assertThat(result.imageCount).isEqualTo(imageCount)
        assertThat(result.videoCount).isEqualTo(videoCount)
        assertThat(result.coverPhoto).isEqualTo(cover)
        assertThat(result.defaultCover).isEqualTo(defaultCover)
    }

    @Test
    fun `test that GifAlbum is mapped correctly with resource title`() {
        val count = 5
        val imageCount = 0
        val videoCount = 5
        val cover: Photo? = null
        val defaultCover = createPhoto(id = 3L, name = "gif_default.gif")

        val result = underTest(
            count = count,
            imageCount = imageCount,
            videoCount = videoCount,
            cover = cover,
            defaultCover = defaultCover,
            album = Album.GifAlbum
        )

        assertThat(result.id).isEqualTo(Album.GifAlbum)
        assertThat(result.title).isEqualTo(AlbumTitle.ResourceTitle(sharedResR.string.system_album_gif_title))
        assertThat(result.count).isEqualTo(count)
        assertThat(result.imageCount).isEqualTo(imageCount)
        assertThat(result.videoCount).isEqualTo(videoCount)
        assertThat(result.coverPhoto).isNull()
        assertThat(result.defaultCover).isEqualTo(defaultCover)
    }

    @Test
    fun `test that RawAlbum is mapped correctly with resource title`() {
        val count = 20
        val imageCount = 20
        val videoCount = 0
        val cover = createPhoto(id = 4L, name = "raw.dng")
        val defaultCover: Photo? = null

        val result = underTest(
            count = count,
            imageCount = imageCount,
            videoCount = videoCount,
            cover = cover,
            defaultCover = defaultCover,
            album = Album.RawAlbum
        )

        assertThat(result.id).isEqualTo(Album.RawAlbum)
        assertThat(result.title).isEqualTo(AlbumTitle.ResourceTitle(sharedResR.string.system_album_raw_title))
        assertThat(result.count).isEqualTo(count)
        assertThat(result.imageCount).isEqualTo(imageCount)
        assertThat(result.videoCount).isEqualTo(videoCount)
        assertThat(result.coverPhoto).isEqualTo(cover)
        assertThat(result.defaultCover).isNull()
    }

    @Test
    fun `test that UserAlbum is mapped correctly with string title`() {
        val albumId = AlbumId(123L)
        val albumTitle = "My Vacation Photos"
        val userAlbum = Album.UserAlbum(
            id = albumId,
            title = albumTitle,
            cover = null,
            creationTime = 1000L,
            modificationTime = 2000L,
            isExported = false
        )
        val count = 50
        val imageCount = 40
        val videoCount = 10
        val cover = createPhoto(id = 5L, name = "vacation.jpg")
        val defaultCover = createPhoto(id = 6L, name = "vacation_default.jpg")

        val result = underTest(
            count = count,
            imageCount = imageCount,
            videoCount = videoCount,
            cover = cover,
            defaultCover = defaultCover,
            album = userAlbum
        )

        assertThat(result.id).isEqualTo(userAlbum)
        assertThat(result.title).isEqualTo(AlbumTitle.StringTitle(albumTitle))
        assertThat(result.count).isEqualTo(count)
        assertThat(result.imageCount).isEqualTo(imageCount)
        assertThat(result.videoCount).isEqualTo(videoCount)
        assertThat(result.coverPhoto).isEqualTo(cover)
        assertThat(result.defaultCover).isEqualTo(defaultCover)
    }

    @Test
    fun `test that album with null cover and null default cover is mapped correctly`() {
        val result = underTest(
            count = 0,
            imageCount = 0,
            videoCount = 0,
            cover = null,
            defaultCover = null,
            album = Album.FavouriteAlbum
        )

        assertThat(result.coverPhoto).isNull()
        assertThat(result.defaultCover).isNull()
    }

    @Test
    fun `test that MediaAlbum System with FavouriteSystemAlbum is mapped correctly`() {
        val mockFavouriteSystemAlbum = mock<FavouriteSystemAlbum>()
        val albumName = "Favourites"
        whenever(mockFavouriteSystemAlbum.albumName).thenReturn(albumName)
        val cover = createPhoto(id = 10L, name = "fav_cover.jpg")

        val mediaAlbum = MediaAlbum.System(
            id = mockFavouriteSystemAlbum,
            cover = cover
        )

        val result = underTest(mediaAlbum)

        assertThat(result.id).isEqualTo(Album.FavouriteAlbum)
        assertThat(result.title).isEqualTo(AlbumTitle.StringTitle(albumName))
        assertThat(result.count).isEqualTo(0)
        assertThat(result.imageCount).isEqualTo(0)
        assertThat(result.videoCount).isEqualTo(0)
        assertThat(result.coverPhoto).isEqualTo(cover)
        assertThat(result.defaultCover).isEqualTo(cover)
    }

    @Test
    fun `test that MediaAlbum System with GifSystemAlbum is mapped correctly`() {
        val mockGifSystemAlbum = mock<GifSystemAlbum>()
        val albumName = "GIFs"
        whenever(mockGifSystemAlbum.albumName).thenReturn(albumName)
        val cover = createPhoto(id = 11L, name = "gif_cover.gif")

        val mediaAlbum = MediaAlbum.System(
            id = mockGifSystemAlbum,
            cover = cover
        )

        val result = underTest(mediaAlbum)

        assertThat(result.id).isEqualTo(Album.GifAlbum)
        assertThat(result.title).isEqualTo(AlbumTitle.StringTitle(albumName))
        assertThat(result.count).isEqualTo(0)
        assertThat(result.imageCount).isEqualTo(0)
        assertThat(result.videoCount).isEqualTo(0)
        assertThat(result.coverPhoto).isEqualTo(cover)
        assertThat(result.defaultCover).isEqualTo(cover)
    }

    @Test
    fun `test that MediaAlbum System with RawSystemAlbum is mapped correctly`() {
        val mockRawSystemAlbum = mock<RawSystemAlbum>()
        val albumName = "RAW"
        whenever(mockRawSystemAlbum.albumName).thenReturn(albumName)

        val mediaAlbum = MediaAlbum.System(
            id = mockRawSystemAlbum,
            cover = null
        )

        val result = underTest(mediaAlbum)

        assertThat(result.id).isEqualTo(Album.RawAlbum)
        assertThat(result.title).isEqualTo(AlbumTitle.StringTitle(albumName))
        assertThat(result.count).isEqualTo(0)
        assertThat(result.imageCount).isEqualTo(0)
        assertThat(result.videoCount).isEqualTo(0)
        assertThat(result.coverPhoto).isNull()
        assertThat(result.defaultCover).isNull()
    }

    @Test
    fun `test that MediaAlbum System with unknown SystemAlbum defaults to RawAlbum`() {
        val unknownSystemAlbum = mock<mega.privacy.android.domain.entity.media.SystemAlbum>()
        val albumName = "Unknown Album"
        whenever(unknownSystemAlbum.albumName).thenReturn(albumName)

        val mediaAlbum = MediaAlbum.System(
            id = unknownSystemAlbum,
            cover = null
        )

        val result = underTest(mediaAlbum)

        assertThat(result.id).isEqualTo(Album.RawAlbum)
        assertThat(result.title).isEqualTo(AlbumTitle.StringTitle(albumName))
    }

    @Test
    fun `test that MediaAlbum User is mapped correctly`() {
        val albumId = AlbumId(456L)
        val title = "My Custom Album"
        val creationTime = 3000L
        val modificationTime = 4000L
        val cover = createPhoto(id = 20L, name = "custom_cover.jpg")

        val mediaAlbum = MediaAlbum.User(
            id = albumId,
            title = title,
            creationTime = creationTime,
            modificationTime = modificationTime,
            isExported = true,
            cover = cover
        )

        val result = underTest(mediaAlbum)

        assertThat(result.title).isEqualTo(AlbumTitle.StringTitle(title))
        assertThat(result.count).isEqualTo(0)
        assertThat(result.imageCount).isEqualTo(0)
        assertThat(result.videoCount).isEqualTo(0)
        assertThat(result.coverPhoto).isEqualTo(cover)
        assertThat(result.defaultCover).isEqualTo(cover)
        assertThat(result.isExported).isEqualTo(true)

        val expectedUserAlbum = Album.UserAlbum(
            id = albumId,
            title = title,
            cover = cover,
            creationTime = creationTime,
            modificationTime = modificationTime,
            isExported = true
        )
        assertThat(result.id).isEqualTo(expectedUserAlbum)
    }

    @Test
    fun `test that MediaAlbum User without cover is mapped correctly`() {
        val albumId = AlbumId(789L)
        val title = "Empty Album"

        val mediaAlbum = MediaAlbum.User(
            id = albumId,
            title = title,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false,
            cover = null
        )

        val result = underTest(mediaAlbum)

        assertThat(result.title).isEqualTo(AlbumTitle.StringTitle(title))
        assertThat(result.coverPhoto).isNull()
        assertThat(result.defaultCover).isNull()
        assertThat(result.isExported).isEqualTo(false)

        val expectedUserAlbum = Album.UserAlbum(
            id = albumId,
            title = title,
            cover = null,
            creationTime = 0L,
            modificationTime = 0L,
            isExported = false
        )
        assertThat(result.id).isEqualTo(expectedUserAlbum)
    }

    private fun createPhoto(
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
