package mega.privacy.android.domain.usecase.media

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

/**
 * Test class for [MonitorUserAlbumByIdUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorUserAlbumByIdUseCaseTest {
    private lateinit var underTest: MonitorUserAlbumByIdUseCase

    private val albumRepository: AlbumRepository = mock()
    private val photosRepository: PhotosRepository = mock()

    @BeforeEach
    fun setUp() {
        reset(albumRepository, photosRepository)
        underTest = MonitorUserAlbumByIdUseCase(
            albumRepository = albumRepository,
            photosRepository = photosRepository
        )
    }

    @Test
    fun `test that cached album is emitted on start`() = runTest {
        val albumId = AlbumId(1L)
        val userSet = createMockUserSet(id = 1L, name = "Test Album")
        val updatedUserSets = listOf(createMockUserSet(id = 2L, name = "Other Album"))

        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(updatedUserSets))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(emptyList())

        underTest(albumId).test {
            // First emission from onStart (cache, refresh = false)
            val cachedAlbum = awaitItem()
            assertThat(cachedAlbum).isNotNull()
            assertThat(cachedAlbum?.id).isEqualTo(albumId)
            assertThat(cachedAlbum?.title).isEqualTo("Test Album")

            awaitComplete()
        }
    }

    @Test
    fun `test that album is updated when user sets are updated`() = runTest {
        val albumId = AlbumId(1L)
        val initialUserSet = createMockUserSet(id = 1L, name = "Initial Album")
        val updatedUserSet = createMockUserSet(id = 1L, name = "Updated Album")

        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(listOf(updatedUserSet)))
        whenever(albumRepository.getUserSet(albumId))
            .thenReturn(initialUserSet) // First call from onStart
            .thenReturn(updatedUserSet) // Second call from mapLatest
        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(emptyList())

        underTest(albumId).test {
            // First emission from onStart
            val cachedAlbum = awaitItem()
            assertThat(cachedAlbum?.title).isEqualTo("Initial Album")

            // Second emission from monitorUserSetsUpdate
            val updatedAlbum = awaitItem()
            assertThat(updatedAlbum?.title).isEqualTo("Updated Album")

            awaitComplete()
        }
    }

    @Test
    fun `test that album cache is cleared when user sets are updated`() = runTest {
        val albumId = AlbumId(1L)
        val userSet = createMockUserSet(id = 1L, name = "Test Album")

        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(listOf(userSet)))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(emptyList())

        underTest(albumId).test {
            // First emission from onStart
            awaitItem()

            // Second emission from monitorUserSetsUpdate
            awaitItem()

            awaitComplete()
        }

        // Verify cache was cleared
        verify(albumRepository).clearAlbumCache(albumId)
    }

    @Test
    fun `test that null is emitted when album does not exist in cache`() = runTest {
        val albumId = AlbumId(999L)
        val updatedUserSets = listOf(createMockUserSet(id = 1L, name = "Other Album"))

        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(updatedUserSets))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(null)

        underTest(albumId).test {
            // First emission from onStart should be null
            val cachedAlbum = awaitItem()
            assertThat(cachedAlbum).isNull()

            awaitComplete()
        }
    }

    @Test
    fun `test that only cached album is emitted when album is not in updated sets`() = runTest {
        val albumId = AlbumId(1L)
        val initialUserSet = createMockUserSet(id = 1L, name = "Test Album")
        val updatedUserSets = listOf(createMockUserSet(id = 2L, name = "Other Album"))

        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(updatedUserSets))
        whenever(albumRepository.getUserSet(albumId))
            .thenReturn(initialUserSet) // First call from onStart
            .thenReturn(null) // Not in updated sets
        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(emptyList())

        underTest(albumId).test {
            // First emission from onStart
            val cachedAlbum = awaitItem()
            assertThat(cachedAlbum).isNotNull()
            assertThat(cachedAlbum?.title).isEqualTo("Test Album")

            // Flow completes since album is not in updated sets (filtered out by mapNotNull)
            awaitComplete()
        }
    }

    @Test
    fun `test that album with cover photo is handled correctly`() = runTest {
        val albumId = AlbumId(1L)
        val coverId = 123L
        val photoNodeId = NodeId(456L)
        val userSet = createMockUserSet(id = 1L, name = "Album with Cover", cover = coverId)
        val albumPhotoId = createMockAlbumPhotoId(id = coverId, nodeId = photoNodeId.longValue)
        val coverPhoto = createMockPhoto()

        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(listOf(userSet)))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(listOf(albumPhotoId))
        whenever(photosRepository.getPhotoFromNodeID(photoNodeId, albumPhotoId, false))
            .thenReturn(coverPhoto)
        whenever(photosRepository.getPhotoFromNodeID(photoNodeId, albumPhotoId, true))
            .thenReturn(coverPhoto)

        underTest(albumId).test {
            // First emission from onStart (refresh = false)
            val cachedAlbum = awaitItem()
            assertThat(cachedAlbum).isNotNull()
            assertThat(cachedAlbum?.cover).isEqualTo(coverPhoto)

            // Second emission from monitorUserSetsUpdate (refresh = true)
            val updatedAlbum = awaitItem()
            assertThat(updatedAlbum).isNotNull()
            assertThat(updatedAlbum?.cover).isEqualTo(coverPhoto)

            awaitComplete()
        }
    }

    @Test
    fun `test that album without cover photo is handled correctly`() = runTest {
        val albumId = AlbumId(1L)
        val userSet = createMockUserSet(id = 1L, name = "Album without Cover", cover = null)

        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(listOf(userSet)))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(emptyList())

        underTest(albumId).test {
            // First emission from onStart
            val cachedAlbum = awaitItem()
            assertThat(cachedAlbum).isNotNull()
            assertThat(cachedAlbum?.cover).isNull()

            // Second emission from monitorUserSetsUpdate
            val updatedAlbum = awaitItem()
            assertThat(updatedAlbum).isNotNull()
            assertThat(updatedAlbum?.cover).isNull()

            awaitComplete()
        }
    }

    @Test
    fun `test that refresh parameter is correct for cache and update flows`() = runTest {
        val albumId = AlbumId(1L)
        val coverId = 123L
        val photoNodeId = NodeId(456L)
        val userSet = createMockUserSet(id = 1L, name = "Test Album", cover = coverId)
        val albumPhotoId = createMockAlbumPhotoId(id = coverId, nodeId = photoNodeId.longValue)
        val coverPhoto = createMockPhoto()

        whenever(albumRepository.monitorUserSetsUpdate()).thenReturn(flowOf(listOf(userSet)))
        whenever(albumRepository.getUserSet(albumId)).thenReturn(userSet)
        whenever(albumRepository.getAlbumElementIDs(albumId)).thenReturn(listOf(albumPhotoId))
        whenever(photosRepository.getPhotoFromNodeID(any(), any(), any())).thenReturn(coverPhoto)

        underTest(albumId).test {
            // First emission from onStart
            awaitItem()

            // Verify refresh = false for cache
            verify(photosRepository).getPhotoFromNodeID(photoNodeId, albumPhotoId, false)

            // Second emission from monitorUserSetsUpdate
            awaitItem()

            awaitComplete()
        }

        // Verify refresh = true for update
        verify(photosRepository).getPhotoFromNodeID(photoNodeId, albumPhotoId, true)
    }

    private fun createMockUserSet(
        id: Long,
        name: String,
        cover: Long? = null,
        creationTime: Long = 1000L,
        modificationTime: Long = 2000L,
        isExported: Boolean = false,
    ): UserSet {
        return mock<UserSet> {
            on { this.id }.thenReturn(id)
            on { this.name }.thenReturn(name)
            on { this.cover }.thenReturn(cover)
            on { this.creationTime }.thenReturn(creationTime)
            on { this.modificationTime }.thenReturn(modificationTime)
            on { this.isExported }.thenReturn(isExported)
        }
    }

    private fun createMockPhoto(): Photo.Image {
        return mock<Photo.Image> {
            on { id }.thenReturn(1L)
            on { name }.thenReturn("test_photo.jpg")
            on { modificationTime }.thenReturn(LocalDateTime.now())
        }
    }

    private fun createMockAlbumPhotoId(id: Long, nodeId: Long, albumId: Long = 1L): AlbumPhotoId {
        return AlbumPhotoId(
            id = id,
            nodeId = NodeId(nodeId),
            albumId = AlbumId(albumId)
        )
    }
}

