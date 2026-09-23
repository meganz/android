package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DownloadPublicAlbumPhotoUseCaseTest {
    private lateinit var underTest: DownloadPublicAlbumPhotoUseCase

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = DownloadPublicAlbumPhotoUseCase(
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that use case returns correct result for thumbnail`() = runTest {
        whenever(albumRepository.downloadPublicThumbnail(1, "path")).thenReturn(true)

        assertThat(underTest(photoId = 1, path = "path", isPreview = false)).isTrue()
    }

    @Test
    fun `test that use case returns correct result for preview`() = runTest {
        whenever(albumRepository.downloadPublicPreview(1, "path")).thenReturn(true)

        assertThat(underTest(photoId = 1, path = "path", isPreview = true)).isTrue()
    }
}
