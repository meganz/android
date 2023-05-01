package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ExportAlbumsUseCaseTest {
    private lateinit var underTest: ExportAlbumsUseCase

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = ExportAlbumsUseCase(
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        // given
        val albumIds = listOf(
            AlbumId(1L),
            AlbumId(2L),
            AlbumId(3L),
        )
        val expectedAlbumIdLinks = listOf(
            albumIds[0] to AlbumLink("Link 1"),
            albumIds[1] to AlbumLink("Link 2"),
            albumIds[2] to AlbumLink("Link 3"),
        )
        whenever(albumRepository.exportAlbums(albumIds))
            .thenReturn(expectedAlbumIdLinks)

        // when
        val actualAlbumIdLinks = underTest(albumIds)

        // then
        assertThat(expectedAlbumIdLinks).isEqualTo(actualAlbumIdLinks)
    }
}
