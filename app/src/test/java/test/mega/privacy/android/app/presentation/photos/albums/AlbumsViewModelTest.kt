package test.mega.privacy.android.app.presentation.photos.albums

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.entity.AlbumItemInfo
import mega.privacy.android.app.domain.usecase.GetFavouriteAlbumItems
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.model.AlbumsLoadState
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AlbumsViewModelTest {
    private lateinit var underTest: AlbumsViewModel

    private val getFavouriteAlbumItems = mock<GetFavouriteAlbumItems>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = AlbumsViewModel(
            getFavouriteAlbumItems = getFavouriteAlbumItems,
        )
    }

    @Test
    fun `test default state` () = runTest {
        underTest.favouritesState.test {
            assertTrue(awaitItem() is AlbumsLoadState.Loading)
        }
    }

    @Test
    fun `test that start with loading state and there is no favourite item`() = runTest {
        whenever(getFavouriteAlbumItems()).thenReturn(
            flowOf(emptyList())
        )
        underTest.favouritesState.test {
            assertTrue(awaitItem() is AlbumsLoadState.Loading)
            assertTrue(awaitItem() is AlbumsLoadState.Success)
        }
    }

    @Test
    fun `test that start with loading state and load favourites success`() = runTest {
        val node = mock<MegaNode>()
        whenever(node.handle).thenReturn(12345)
        whenever(node.base64Handle).thenReturn("12345")
        whenever(node.modificationTime).thenReturn(1234567890)
        val albumItemInfo = AlbumItemInfo(
            handle = node.handle,
            base64Handle = node.base64Handle,
            modifiedTime = node.modificationTime
        )
        val list = listOf(albumItemInfo)
        whenever(getFavouriteAlbumItems()).thenReturn(
            flowOf(list)
        )
        underTest.favouritesState.test {
            assertTrue(awaitItem() is AlbumsLoadState.Loading)
            assertTrue(awaitItem() is AlbumsLoadState.Success)
        }
    }
}