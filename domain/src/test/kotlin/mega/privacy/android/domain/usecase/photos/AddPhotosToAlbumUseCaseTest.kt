package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AddPhotosToAlbumUseCaseTest {
    private lateinit var underTest: AddPhotosToAlbumUseCase

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = AddPhotosToAlbumUseCase(
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        // given
        val albumId = AlbumId(1L)
        val photoIds = listOf(
            NodeId(1L),
            NodeId(2L),
            NodeId(3L),
        )

        whenever(albumRepository.addBulkPhotosToAlbum(albumId, photoIds))
            .thenReturn(photoIds.size)

        // when
        val success = underTest(albumId, photoIds)

        // then
        assertThat(success).isEqualTo(photoIds.size)
    }
}
