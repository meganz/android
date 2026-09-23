package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DisableExportAlbumsUseCaseTest {
    private lateinit var underTest: DisableExportAlbumsUseCase

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = DisableExportAlbumsUseCase(
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
        val expectedSuccessfulOperation = albumIds.size
        whenever(albumRepository.disableExportAlbums(albumIds))
            .thenReturn(expectedSuccessfulOperation)

        // when
        val actualSuccessfulOperation = underTest(albumIds)

        // then
        assertThat(expectedSuccessfulOperation).isEqualTo(actualSuccessfulOperation)
    }
}
