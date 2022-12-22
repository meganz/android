package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.fail

@ExperimentalCoroutinesApi
class DefaultRemoveAlbumsTest {
    private lateinit var underTest: RemoveAlbums

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = DefaultRemoveAlbums(
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        val albumIds = listOf(
            AlbumId(1L),
            AlbumId(2L),
            AlbumId(3L),
        )
        whenever(albumRepository.removeAlbums(any())).thenReturn(Unit)

        try {
            underTest(albumIds)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }
}
