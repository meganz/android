package test.mega.privacy.android.app.presentation.photos.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.Album
import mega.privacy.android.domain.usecase.GetAlbums
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.model.AlbumsLoadState
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AlbumsViewModelTest {
    private lateinit var underTest: AlbumsViewModel

    private val getAlbums = mock<GetAlbums>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = AlbumsViewModel(
            getAlbums = getAlbums,
        )
    }

    @Test
    fun `test default state`() = runTest {
        underTest.albumsState.test {
            assertTrue(awaitItem() is AlbumsLoadState.Loading)
        }
    }

    @Test
    fun `test that start with loading state and load albums success`() = runTest {
        whenever(getAlbums()).thenReturn(
            flowOf(
                createAlbums()
            )
        )
        underTest.albumsState.test {
            assertTrue(awaitItem() is AlbumsLoadState.Loading)
            assertTrue(awaitItem() is AlbumsLoadState.Success)
        }
    }

    @Test
    fun `test that an error in the flow returns an empty state`() = runTest {
        whenever(getAlbums()).thenReturn(flow { throw Exception("Error") })

        underTest.albumsState.test {
            assertThat(awaitItem()).isSameInstanceAs(AlbumsLoadState.Loading)
            assertThat(awaitItem()).isSameInstanceAs(AlbumsLoadState.Empty)
        }
    }

    private fun createAlbums(): List<Album> {
        val favouriteAlbum = Album.FavouriteAlbum(
            null,
            0
        )
        return listOf(
            favouriteAlbum
        )
    }
}