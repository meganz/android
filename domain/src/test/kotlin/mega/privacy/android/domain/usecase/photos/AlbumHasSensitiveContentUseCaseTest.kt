package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [AlbumHasSensitiveContentUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AlbumHasSensitiveContentUseCaseTest {

    private lateinit var underTest: AlbumHasSensitiveContentUseCase

    private val albumRepository = mock<AlbumRepository>()
    private val photosRepository = mock<PhotosRepository>()

    @BeforeEach
    fun resetMocks() {
        reset(albumRepository, photosRepository)
    }

    private fun initUseCase() {
        underTest = AlbumHasSensitiveContentUseCase(
            albumRepository = albumRepository,
            photosRepository = photosRepository,
        )
    }

    @Test
    fun `test that false is returned when album has no elements`() = runTest {
        val albumId = AlbumId(1L)
        whenever(albumRepository.getAlbumElementIDs(albumId, refresh = false))
            .thenReturn(emptyList())

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isFalse()
        verify(albumRepository).getAlbumElementIDs(albumId, refresh = false)
    }

    @Test
    fun `test that false is returned when album has only non-sensitive photos`() = runTest {
        val albumId = AlbumId(1L)
        val photo1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val photo2 = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(photo1, photo2)

        val nonSensitivePhoto1 = createMockPhoto(isSensitive = false, isSensitiveInherited = false)
        val nonSensitivePhoto2 = createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        whenever(albumRepository.getAlbumElementIDs(albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo1.nodeId,
                albumPhotoId = photo1,
                refresh = false,
            )
        ).thenReturn(nonSensitivePhoto1)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo2.nodeId,
                albumPhotoId = photo2,
                refresh = false,
            )
        ).thenReturn(nonSensitivePhoto2)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isFalse()
        verify(albumRepository).getAlbumElementIDs(albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(photo1.nodeId, photo1, false)
        verify(photosRepository).getPhotoFromNodeID(photo2.nodeId, photo2, false)
    }

    @Test
    fun `test that true is returned when album has sensitive photo`() = runTest {
        val albumId = AlbumId(1L)
        val photo1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val photo2 = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(photo1, photo2)

        val sensitivePhoto = createMockPhoto(isSensitive = true, isSensitiveInherited = false)
        val nonSensitivePhoto = createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        whenever(albumRepository.getAlbumElementIDs(albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo1.nodeId,
                albumPhotoId = photo1,
                refresh = false,
            )
        ).thenReturn(sensitivePhoto)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo2.nodeId,
                albumPhotoId = photo2,
                refresh = false,
            )
        ).thenReturn(nonSensitivePhoto)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isTrue()
        verify(albumRepository).getAlbumElementIDs(albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(photo1.nodeId, photo1, false)
        // Should stop checking after finding sensitive photo
    }

    @Test
    fun `test that true is returned when album has sensitive inherited photo`() = runTest {
        val albumId = AlbumId(1L)
        val photo1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val photo2 = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(photo1, photo2)

        val sensitiveInheritedPhoto =
            createMockPhoto(isSensitive = false, isSensitiveInherited = true)
        val nonSensitivePhoto = createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        whenever(albumRepository.getAlbumElementIDs(albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo1.nodeId,
                albumPhotoId = photo1,
                refresh = false,
            )
        ).thenReturn(sensitiveInheritedPhoto)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo2.nodeId,
                albumPhotoId = photo2,
                refresh = false,
            )
        ).thenReturn(nonSensitivePhoto)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isTrue()
        verify(albumRepository).getAlbumElementIDs(albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(photo1.nodeId, photo1, false)
    }

    @Test
    fun `test that true is returned when album has both sensitive and sensitive inherited photos`() =
        runTest {
            val albumId = AlbumId(1L)
            val photo1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
            val photo2 = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
            val elements = listOf(photo1, photo2)

            val sensitivePhoto = createMockPhoto(isSensitive = true, isSensitiveInherited = false)
            val sensitiveInheritedPhoto =
                createMockPhoto(isSensitive = false, isSensitiveInherited = true)

            whenever(albumRepository.getAlbumElementIDs(albumId, refresh = false))
                .thenReturn(elements)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nodeId = photo1.nodeId,
                    albumPhotoId = photo1,
                    refresh = false,
                )
            ).thenReturn(sensitivePhoto)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nodeId = photo2.nodeId,
                    albumPhotoId = photo2,
                    refresh = false,
                )
            ).thenReturn(sensitiveInheritedPhoto)

            initUseCase()
            val result = underTest(albumId)

            assertThat(result).isTrue()
            verify(albumRepository).getAlbumElementIDs(albumId, refresh = false)
            verify(photosRepository).getPhotoFromNodeID(photo1.nodeId, photo1, false)
        }

    @Test
    fun `test that false is returned when all photos are null`() = runTest {
        val albumId = AlbumId(1L)
        val photo1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val photo2 = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(photo1, photo2)

        whenever(albumRepository.getAlbumElementIDs(albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo1.nodeId,
                albumPhotoId = photo1,
                refresh = false,
            )
        ).thenReturn(null)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo2.nodeId,
                albumPhotoId = photo2,
                refresh = false,
            )
        ).thenReturn(null)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isFalse()
        verify(albumRepository).getAlbumElementIDs(albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(photo1.nodeId, photo1, false)
        verify(photosRepository).getPhotoFromNodeID(photo2.nodeId, photo2, false)
    }

    @Test
    fun `test that true is returned when sensitive photo is found after null photos`() = runTest {
        val albumId = AlbumId(1L)
        val photo1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val photo2 = albumPhotoId(id = 20L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(photo1, photo2)

        val sensitivePhoto = createMockPhoto(isSensitive = true, isSensitiveInherited = false)

        whenever(albumRepository.getAlbumElementIDs(albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo1.nodeId,
                albumPhotoId = photo1,
                refresh = false,
            )
        ).thenReturn(null)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo2.nodeId,
                albumPhotoId = photo2,
                refresh = false,
            )
        ).thenReturn(sensitivePhoto)

        initUseCase()
        val result = underTest(albumId)

        assertThat(result).isTrue()
        verify(albumRepository).getAlbumElementIDs(albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(photo1.nodeId, photo1, false)
        verify(photosRepository).getPhotoFromNodeID(photo2.nodeId, photo2, false)
    }

    @Test
    fun `test that refresh parameter is passed correctly`() = runTest {
        val albumId = AlbumId(1L)
        val photo1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val elements = listOf(photo1)

        val nonSensitivePhoto = createMockPhoto(isSensitive = false, isSensitiveInherited = false)

        whenever(albumRepository.getAlbumElementIDs(albumId, refresh = true))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = photo1.nodeId,
                albumPhotoId = photo1,
                refresh = true,
            )
        ).thenReturn(nonSensitivePhoto)

        initUseCase()
        val result = underTest(albumId, refresh = true)

        assertThat(result).isFalse()
        verify(albumRepository).getAlbumElementIDs(albumId, refresh = true)
        verify(photosRepository).getPhotoFromNodeID(photo1.nodeId, photo1, true)
    }

    @Test
    fun `test that true is returned when photo has both sensitive and sensitive inherited flags`() =
        runTest {
            val albumId = AlbumId(1L)
            val photo1 = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
            val elements = listOf(photo1)

            val sensitivePhoto = createMockPhoto(isSensitive = true, isSensitiveInherited = true)

            whenever(albumRepository.getAlbumElementIDs(albumId, refresh = false))
                .thenReturn(elements)
            whenever(
                photosRepository.getPhotoFromNodeID(
                    nodeId = photo1.nodeId,
                    albumPhotoId = photo1,
                    refresh = false,
                )
            ).thenReturn(sensitivePhoto)

            initUseCase()
            val result = underTest(albumId)

            assertThat(result).isTrue()
            verify(albumRepository).getAlbumElementIDs(albumId, refresh = false)
            verify(photosRepository).getPhotoFromNodeID(photo1.nodeId, photo1, false)
        }

    private fun albumPhotoId(id: Long, nodeId: Long, albumId: Long): AlbumPhotoId =
        AlbumPhotoId(
            id = id,
            nodeId = NodeId(nodeId),
            albumId = AlbumId(albumId)
        )

    private fun createMockPhoto(
        isSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): Photo.Image = mock {
        on { this.isSensitive }.thenReturn(isSensitive)
        on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
    }
}
