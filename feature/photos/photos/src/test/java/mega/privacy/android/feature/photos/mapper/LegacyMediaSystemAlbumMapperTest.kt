package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.feature.photos.presentation.albums.model.FavouriteSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.GifSystemAlbum
import mega.privacy.android.feature.photos.presentation.albums.model.RawSystemAlbum
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegacyMediaSystemAlbumMapperTest {

    private lateinit var underTest: LegacyMediaSystemAlbumMapper
    private lateinit var systemAlbums: Set<SystemAlbum>
    private val favouriteSystemAlbum: FavouriteSystemAlbum = mock()
    private val gifSystemAlbum: GifSystemAlbum = mock()
    private val rawSystemAlbum: RawSystemAlbum = mock()

    @BeforeAll
    fun setup() {
        systemAlbums = setOf(
            favouriteSystemAlbum,
            gifSystemAlbum,
            rawSystemAlbum
        )
        underTest = LegacyMediaSystemAlbumMapper(systemAlbums)
    }

    @BeforeEach
    fun resetMocks() {
        reset(favouriteSystemAlbum, gifSystemAlbum, rawSystemAlbum)
        systemAlbums = setOf(
            favouriteSystemAlbum,
            gifSystemAlbum,
            rawSystemAlbum
        )
    }

    @Test
    fun `test that FavouriteAlbum is mapped correctly without cover`() {
        val album = Album.FavouriteAlbum

        val result = underTest(album, null)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(favouriteSystemAlbum)
        assertThat(result?.cover).isNull()
    }

    @Test
    fun `test that FavouriteAlbum is mapped correctly with cover`() {
        val album = Album.FavouriteAlbum
        val coverPhoto = createMockPhoto(id = 123L, name = "cover.jpg")

        val result = underTest(album, coverPhoto)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(favouriteSystemAlbum)
        assertThat(result?.cover).isEqualTo(coverPhoto)
    }

    @Test
    fun `test that GifAlbum is mapped correctly without cover`() {
        val album = Album.GifAlbum

        val result = underTest(album, null)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(gifSystemAlbum)
        assertThat(result?.cover).isNull()
    }

    @Test
    fun `test that GifAlbum is mapped correctly with cover`() {
        val album = Album.GifAlbum
        val coverPhoto = createMockPhoto(id = 456L, name = "animated.gif")

        val result = underTest(album, coverPhoto)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(gifSystemAlbum)
        assertThat(result?.cover).isEqualTo(coverPhoto)
    }

    @Test
    fun `test that RawAlbum is mapped correctly without cover`() {
        val album = Album.RawAlbum

        val result = underTest(album, null)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(rawSystemAlbum)
        assertThat(result?.cover).isNull()
    }

    @Test
    fun `test that RawAlbum is mapped correctly with cover`() {
        val album = Album.RawAlbum
        val coverPhoto = createMockPhoto(id = 789L, name = "photo.raw")

        val result = underTest(album, coverPhoto)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(rawSystemAlbum)
        assertThat(result?.cover).isEqualTo(coverPhoto)
    }

    @Test
    fun `test that UserAlbum returns null`() {
        val coverPhoto = createMockPhoto(id = 100L, name = "cover.jpg")
        val album = Album.UserAlbum(
            id = AlbumId(1L),
            title = "Test Album",
            cover = coverPhoto,
            creationTime = 1000L,
            modificationTime = 2000L,
            isExported = false
        )

        val result = underTest(album, null)

        assertThat(result).isNull()
    }

    @Test
    fun `test that null is returned when system album is not in the set`() {
        val emptySystemAlbums = emptySet<SystemAlbum>()
        val mapperWithEmptySet = LegacyMediaSystemAlbumMapper(emptySystemAlbums)
        val album = Album.FavouriteAlbum

        val result = mapperWithEmptySet(album, null)

        assertThat(result).isNull()
    }

    private fun createMockPhoto(
        id: Long = 0L,
        name: String = "test.jpg",
        isFavourite: Boolean = false,
        creationTime: LocalDateTime = LocalDateTime.now(),
        modificationTime: LocalDateTime = LocalDateTime.now(),
        fileTypeInfo: FileTypeInfo = mock<StaticImageFileTypeInfo>(),
    ): Photo.Image {
        return Photo.Image(
            id = id,
            albumPhotoId = null,
            parentId = 0L,
            name = name,
            isFavourite = isFavourite,
            creationTime = creationTime,
            modificationTime = modificationTime,
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = fileTypeInfo,
            size = 0L,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false,
            base64Id = null,
        )
    }
}

