package mega.privacy.android.domain.usecase.media

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetUserAlbumCoverPhotoUseCaseTest {

    private lateinit var underTest: GetUserAlbumCoverPhotoUseCase

    private val albumRepository: AlbumRepository = mock()
    private val photosRepository: PhotosRepository = mock()

    @BeforeEach
    fun resetMocks() {
        reset(albumRepository, photosRepository)
    }

    @BeforeAll
    fun setup() {
        underTest = GetUserAlbumCoverPhotoUseCase(
            albumRepository = albumRepository,
            photosRepository = photosRepository
        )
    }

    @Test
    fun `test that null is returned when album has no elements`() = runTest {
        val albumId = AlbumId(1L)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(emptyList())

        val result = underTest(albumId)

        assertThat(result).isNull()
        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = false)
    }

    @Test
    fun `test that selected cover element is used when present`() = runTest {
        val albumId = AlbumId(1L)
        val selectedCoverId = 100L
        val first = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val selected = albumPhotoId(id = 100L, nodeId = 2000L, albumId = albumId.id)
        val elements = listOf(first, selected)
        val expectedPhoto = mock<Photo.Image>()

        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = selected.nodeId,
                albumPhotoId = selected,
                refresh = false
            )
        ).thenReturn(expectedPhoto)

        val result = underTest(albumId, selectedCoverId = selectedCoverId, refresh = false)

        assertThat(result).isEqualTo(expectedPhoto)
        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(selected.nodeId, selected, false)
    }

    @Test
    fun `test that last element is used when no cover is set`() = runTest {
        val albumId = AlbumId(2L)
        val first = albumPhotoId(id = 10L, nodeId = 1000L, albumId = albumId.id)
        val last = albumPhotoId(id = 20L, nodeId = 3000L, albumId = albumId.id)
        val elements = listOf(first, last)
        val expectedPhoto = mock<Photo.Image>()

        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = false))
            .thenReturn(elements)
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = last.nodeId,
                albumPhotoId = last,
                refresh = false
            )
        ).thenReturn(expectedPhoto)

        val result = underTest(albumId, selectedCoverId = null, refresh = false)

        assertThat(result).isEqualTo(expectedPhoto)
        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = false)
        verify(photosRepository).getPhotoFromNodeID(last.nodeId, last, false)
    }

    @Test
    fun `test that refresh flag is forwarded to both repositories`() = runTest {
        val albumId = AlbumId(3L)
        val last = albumPhotoId(id = 30L, nodeId = 4000L, albumId = albumId.id)
        whenever(albumRepository.getAlbumElementIDs(albumId = albumId, refresh = true))
            .thenReturn(listOf(last))
        whenever(
            photosRepository.getPhotoFromNodeID(
                nodeId = last.nodeId,
                albumPhotoId = last,
                refresh = true
            )
        ).thenReturn(null)

        underTest(albumId, selectedCoverId = null, refresh = true)

        verify(albumRepository).getAlbumElementIDs(albumId = albumId, refresh = true)
        verify(photosRepository).getPhotoFromNodeID(last.nodeId, last, true)
    }

    private fun albumPhotoId(id: Long, nodeId: Long, albumId: Long): AlbumPhotoId =
        AlbumPhotoId(
            id = id,
            nodeId = NodeId(nodeId),
            albumId = AlbumId(albumId)
        )
}


