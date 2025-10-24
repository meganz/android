package mega.privacy.android.domain.usecase.media

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

/**
 * Test class for [GetMediaSystemAlbumsUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMediaSystemAlbumsUseCaseTest {
    private lateinit var underTest: GetMediaSystemAlbumsUseCase

    private val photosRepository: PhotosRepository = mock()
    private val mockSystemAlbums = setOf(
        createMockSystemAlbum("gif") { photo -> photo.name.endsWith(".gif") },
        createMockSystemAlbum("raw") { photo -> photo.name.endsWith(".raw") },
        createMockSystemAlbum("favourite") { photo -> photo.isFavourite }
    )

    @BeforeEach
    fun setUp() {
        reset(photosRepository)
    }

    private fun initUseCase() {
        underTest = GetMediaSystemAlbumsUseCase(
            photosRepository = photosRepository,
            systemAlbums = mockSystemAlbums,
            defaultDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that system albums are returned with cover photos`() = runTest {
        val mockPhotos = createMockPhotos()
        val gifPhoto = mockPhotos[0]
        val rawPhoto = mockPhotos[1]
        val favouritePhoto = mockPhotos[2]

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))

        initUseCase()

        val result = underTest()

        assertThat(result).hasSize(3)
        assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat((result[0] as MediaAlbum.System).cover).isEqualTo(gifPhoto)
        assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat((result[1] as MediaAlbum.System).cover).isEqualTo(rawPhoto)
        assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat((result[2] as MediaAlbum.System).cover).isEqualTo(favouritePhoto)
    }

    @Test
    fun `test that system albums are returned with null covers when no matching photos found`() =
        runTest {
            val mockPhotos = createMockPhotos()

            // Create system albums that don't match any photos
            val nonMatchingSystemAlbums = setOf(
                createMockSystemAlbum("gif", { photo -> false }),
                createMockSystemAlbum("raw", { photo -> false }),
                createMockSystemAlbum("favourite", { photo -> false })
            )

            whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))

            underTest = GetMediaSystemAlbumsUseCase(
                photosRepository = photosRepository,
                systemAlbums = nonMatchingSystemAlbums,
                defaultDispatcher = UnconfinedTestDispatcher()
            )

            val result = underTest()

            assertThat(result).hasSize(3)
            assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat((result[0] as MediaAlbum.System).cover).isNull()
            assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat((result[1] as MediaAlbum.System).cover).isNull()
            assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat((result[2] as MediaAlbum.System).cover).isNull()
        }

    @Test
    fun `test that system albums are returned with null covers when no photos available`() =
        runTest {
            whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(emptyList()))

            initUseCase()

            val result = underTest()

            assertThat(result).hasSize(3)
            assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat((result[0] as MediaAlbum.System).cover).isNull()
            assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat((result[1] as MediaAlbum.System).cover).isNull()
            assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
            assertThat((result[2] as MediaAlbum.System).cover).isNull()
        }

    @Test
    fun `test that system albums are returned with mixed cover photos`() = runTest {
        val mockPhotos = createMockPhotos()
        val gifPhoto = mockPhotos[0]
        val favouritePhoto = mockPhotos[2]

        // Create system albums where only GIF and Favourite match
        val mixedSystemAlbums = setOf(
            createMockSystemAlbum("gif", { photo -> photo.name.endsWith(".gif") }),
            createMockSystemAlbum("raw", { photo -> false }), // No RAW matches
            createMockSystemAlbum("favourite", { photo -> photo.isFavourite })
        )

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))

        underTest = GetMediaSystemAlbumsUseCase(
            photosRepository = photosRepository,
            systemAlbums = mixedSystemAlbums,
            defaultDispatcher = UnconfinedTestDispatcher()
        )

        val result = underTest()

        assertThat(result).hasSize(3)
        assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat((result[0] as MediaAlbum.System).cover).isEqualTo(gifPhoto)
        assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat((result[1] as MediaAlbum.System).cover).isNull()
        assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat((result[2] as MediaAlbum.System).cover).isEqualTo(favouritePhoto)
    }

    @Test
    fun `test that system albums are returned in correct order`() = runTest {
        val mockPhotos = createMockPhotos()
        val gifPhoto = mockPhotos[0]
        val rawPhoto = mockPhotos[1]
        val favouritePhoto = mockPhotos[2]

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))

        initUseCase()

        val result = underTest()

        assertThat(result).hasSize(3)
        assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat(result[1]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat(result[2]).isInstanceOf(MediaAlbum.System::class.java)
    }

    @Test
    fun `test that use case handles multiple matching photos by returning first match`() = runTest {
        val mockPhotos = createMockPhotos() + createMockPhotos() // Duplicate photos
        val firstGifPhoto = mockPhotos[0]

        whenever(photosRepository.monitorPhotos()).thenReturn(flowOf(mockPhotos))

        initUseCase()

        val result = underTest()

        assertThat(result).hasSize(3)
        assertThat(result[0]).isInstanceOf(MediaAlbum.System::class.java)
        assertThat((result[0] as MediaAlbum.System).cover).isEqualTo(firstGifPhoto)
    }

    private fun createMockSystemAlbum(name: String, filter: (Photo) -> Boolean): SystemAlbum {
        return object : SystemAlbum {
            override val albumName: String = name
            override suspend fun filter(photo: Photo): Boolean = filter(photo)
        }
    }

    private fun createMockPhotos(): List<Photo.Image> {
        return listOf(
            mock<Photo.Image> {
                on { id }.thenReturn(1L)
                on { name }.thenReturn("gif_photo.gif")
                on { isFavourite }.thenReturn(false)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            },
            mock<Photo.Image> {
                on { id }.thenReturn(2L)
                on { name }.thenReturn("raw_photo.raw")
                on { isFavourite }.thenReturn(false)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            },
            mock<Photo.Image> {
                on { id }.thenReturn(3L)
                on { name }.thenReturn("favourite_photo.jpg")
                on { isFavourite }.thenReturn(true)
                on { modificationTime }.thenReturn(LocalDateTime.now())
            }
        )
    }
}
