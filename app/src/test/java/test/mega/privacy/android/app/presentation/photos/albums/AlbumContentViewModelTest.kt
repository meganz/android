package test.mega.privacy.android.app.presentation.photos.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.photos.albums.AlbumContentViewModel
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AlbumContentViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that adding progress observation behaves correctly`() = runTest {
        val albumId = AlbumId(1L)

        val underTest = AlbumContentViewModel(
            observeAlbumPhotosAddingProgress = {
                flowOf(AlbumPhotosAddingProgress(false, 100))
            },
            updateAlbumPhotosAddingProgressCompleted = {},
            observeAlbumPhotosRemovingProgress = {
                flowOf(AlbumPhotosRemovingProgress(false, 0))
            },
            updateAlbumPhotosRemovingProgressCompleted = {},
        )
        underTest.observePhotosAddingProgress(albumId)

        underTest.state.drop(1).test {
            val state = awaitItem()
            assertThat(state.totalAddedPhotos).isEqualTo(100)
        }
    }

    @Test
    fun `test that update adding progress completed behaves correctly`() = runTest {
        val albumId = AlbumId(1L)
        var updated = false

        val underTest = AlbumContentViewModel(
            observeAlbumPhotosAddingProgress = { flowOf() },
            updateAlbumPhotosAddingProgressCompleted = { updated = true },
            observeAlbumPhotosRemovingProgress = { flowOf() },
            updateAlbumPhotosRemovingProgressCompleted = {},
        )
        underTest.updatePhotosAddingProgressCompleted(albumId)

        advanceUntilIdle()
        assertThat(updated).isTrue()
    }

    @Test
    fun `test that removing progress observation behaves correctly`() = runTest {
        val albumId = AlbumId(1L)

        val underTest = AlbumContentViewModel(
            observeAlbumPhotosAddingProgress = {
                flowOf(AlbumPhotosAddingProgress(false, 0))
            },
            updateAlbumPhotosAddingProgressCompleted = {},
            observeAlbumPhotosRemovingProgress = {
                flowOf(AlbumPhotosRemovingProgress(false, 100))
            },
            updateAlbumPhotosRemovingProgressCompleted = {},
        )
        underTest.observePhotosRemovingProgress(albumId)

        underTest.state.drop(1).test {
            val state = awaitItem()
            assertThat(state.totalRemovedPhotos).isEqualTo(100)
        }
    }

    @Test
    fun `test that update removing progress completed behaves correctly`() = runTest {
        val albumId = AlbumId(1L)
        var updated = false

        val underTest = AlbumContentViewModel(
            observeAlbumPhotosAddingProgress = { flowOf() },
            updateAlbumPhotosAddingProgressCompleted = { },
            observeAlbumPhotosRemovingProgress = { flowOf() },
            updateAlbumPhotosRemovingProgressCompleted = { updated = true },
        )
        underTest.updatePhotosRemovingProgressCompleted(albumId)

        advanceUntilIdle()
        assertThat(updated).isTrue()
    }
}
