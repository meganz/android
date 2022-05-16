package test.mega.privacy.android.app.presentation.favourites

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.usecase.GetAllFavorites
import mega.privacy.android.app.presentation.favourites.FavouritesViewModel
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class FavouritesViewModelTest {
    private lateinit var underTest: FavouritesViewModel

    private val getFavourites = mock<GetAllFavorites>()
    private val stringUtilWrapper = mock<StringUtilWrapper>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = FavouritesViewModel(
            context = mock(),
            getAllFavorites = getFavourites,
            stringUtilWrapper = stringUtilWrapper,
            megaUtilWrapper = mock()
        )
    }

    @Test
    fun `test default state` () = runTest {
        underTest.favouritesState.test {
            assertTrue(awaitItem() is FavouriteLoadState.Loading)
        }
    }

    @Test
    fun `test that start with loading state and there is no favourite item`() = runTest {
        whenever(getFavourites()).thenReturn(
            flowOf(emptyList())
        )
        underTest.favouritesState.test {
            assertTrue(awaitItem() is FavouriteLoadState.Loading)
            assertTrue(awaitItem() is FavouriteLoadState.Empty)
        }
    }

    @Test
    fun `test that start with loading state and load favourites success`() = runTest {
        val node = mock<MegaNode>()
        whenever(node.handle).thenReturn(123)
        whenever(node.parentHandle).thenReturn(1234)
        whenever(node.base64Handle).thenReturn("base64Handle")
        whenever(node.modificationTime).thenReturn(1234567890)
        whenever(node.isFolder).thenReturn(true)
        whenever(node.isInShare).thenReturn(true)
        whenever(node.name).thenReturn("testName.txt")
        val favourite = FavouriteInfo(
            id = node.handle,
            parentId = node.parentHandle,
            base64Id = node.base64Handle,
            modificationTime = node.modificationTime,
            node = node,
            hasVersion = false,
            numChildFolders = 0,
            numChildFiles = 0
        )
        val list = listOf(favourite)
        whenever(getFavourites()).thenReturn(
            flowOf(list)
        )
        whenever(stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
        underTest.favouritesState.test {
            assertTrue(awaitItem() is FavouriteLoadState.Loading)
            assertTrue(awaitItem() is FavouriteLoadState.Success)
        }
    }
}