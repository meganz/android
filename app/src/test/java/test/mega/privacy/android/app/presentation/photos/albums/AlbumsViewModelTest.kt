package test.mega.privacy.android.app.presentation.photos.albums

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.usecase.GetAlbums
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsLoadState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
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

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test default state`() = runTest {
        underTest.loadState.test {
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
        underTest.loadState.test {
            assertTrue(awaitItem() is AlbumsLoadState.Loading)
            assertTrue(awaitItem() is AlbumsLoadState.Success)
        }
    }

    @Test
    fun `test that start with empty list and returns a single album`() = runTest{
        whenever(getAlbums()).thenReturn(
            flowOf(
                createAlbums()
            )
        )

        underTest.state.test {
            assertEquals(0, awaitItem().albums.size)
            assertEquals(1, awaitItem().albums.size)
        }
    }

    @Test
    fun `test that an error in the flow returns an empty state`() = runTest {
        whenever(getAlbums()).thenReturn(flow { throw Exception("Error") })

        underTest.loadState.test {
            assertThat(awaitItem()).isSameInstanceAs(AlbumsLoadState.Loading)
            assertThat(awaitItem()).isSameInstanceAs(AlbumsLoadState.Empty)
        }
    }

    @Test
    fun `test that an error would return an empty list`() = runTest {
        whenever(getAlbums()).thenReturn(flow { throw Exception("Error") })

        underTest.state.test {
            assertEquals(emptyList(), awaitItem().albums)
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