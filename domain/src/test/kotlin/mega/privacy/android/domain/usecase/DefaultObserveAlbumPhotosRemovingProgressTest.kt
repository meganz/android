package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultObserveAlbumPhotosRemovingProgressTest {
    private lateinit var underTest: ObserveAlbumPhotosRemovingProgress

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = DefaultObserveAlbumPhotosRemovingProgress(
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        val albumId = AlbumId(1L)
        val expectedProgress = AlbumPhotosRemovingProgress(true, 0)

        whenever(albumRepository.observeAlbumPhotosRemovingProgress(albumId))
            .thenReturn(flowOf(expectedProgress))

        underTest(albumId).test {
            val actualProgress = awaitItem()
            assertThat(actualProgress?.isProgressing).isTrue()
            assertThat(actualProgress?.totalRemovedPhotos).isEqualTo(0)

            awaitComplete()
        }
    }
}
