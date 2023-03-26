package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.photos.RemovePhotosFromAlbumUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class RemovePhotosFromAlbumUseCaseTest {

    private lateinit var underTest: RemovePhotosFromAlbumUseCase
    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = RemovePhotosFromAlbumUseCase(albumRepository)
    }

    @Test
    fun `test that the repository method is called for the correct photos`() = runTest {
        val albumId = AlbumId(1)
        val albumContentIdsToRemove = (1..3L).map {
            AlbumPhotoId(it, NodeId(it), albumId)
        }

        underTest(albumId, albumContentIdsToRemove)

        verify(albumRepository).removePhotosFromAlbum(albumId, albumContentIdsToRemove)
    }
}