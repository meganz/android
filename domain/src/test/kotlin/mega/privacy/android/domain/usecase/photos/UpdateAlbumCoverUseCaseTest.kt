package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.fail

@ExperimentalCoroutinesApi
class UpdateAlbumCoverUseCaseTest {
    private lateinit var underTest: UpdateAlbumCoverUseCase

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = UpdateAlbumCoverUseCase(
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that use case executes properly`() = runTest {
        val albumId = AlbumId(1L)
        val elementId = NodeId(2L)
        whenever(albumRepository.updateAlbumCover(albumId, elementId)).thenReturn(Unit)

        try {
            underTest(albumId, elementId)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }
}
