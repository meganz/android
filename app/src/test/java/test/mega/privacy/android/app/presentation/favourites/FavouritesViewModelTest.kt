package test.mega.privacy.android.app.presentation.favourites

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.app.presentation.favourites.FavouritesViewModel
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.FavouriteFolder
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class FavouritesViewModelTest {
    private lateinit var underTest: FavouritesViewModel

    private val getAllFavorites = mock<GetAllFavorites>()
    private val stringUtilWrapper = mock<StringUtilWrapper>()
    private val favouriteMapper = mock<FavouriteMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    private val megaNode = mock<MegaNode>()

    private val fetchNodeWrapper =
        mock<FetchNodeWrapper> { onBlocking { invoke(any()) }.thenReturn(megaNode) }
    private val sortOrderIntMapper = mock<SortOrderIntMapper>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = FavouritesViewModel(
            context = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
            getAllFavorites = getAllFavorites,
            stringUtilWrapper = stringUtilWrapper,
            megaUtilWrapper = mock(),
            getCloudSortOrder = getCloudSortOrder,
            removeFavourites = mock(),
            favouriteMapper = favouriteMapper,
            fetchNode = fetchNodeWrapper,
            sortOrderIntMapper = sortOrderIntMapper,
        )
    }

    @Test
    fun `test default state`() = runTest {
        underTest.favouritesState.test {
            assertTrue(awaitItem() is FavouriteLoadState.Loading)
        }
    }

    @Test
    fun `test that start with loading state and there is no favourite item`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(getAllFavorites()).thenReturn(
            flowOf(emptyList())
        )
        underTest.favouritesState.test {
            assertTrue(awaitItem() is FavouriteLoadState.Loading)
            assertTrue(awaitItem() is FavouriteLoadState.Empty)
        }
    }

    @Test
    fun `test that start with loading state and load favourites success`() = runTest {
        val node = megaNode
        whenever(node.handle).thenReturn(123)
        whenever(node.label).thenReturn(MegaNode.NODE_LBL_RED)
        whenever(node.size).thenReturn(1000L)
        whenever(node.parentHandle).thenReturn(1234)
        whenever(node.base64Handle).thenReturn("base64Handle")
        whenever(node.modificationTime).thenReturn(1234567890)
        whenever(node.isFolder).thenReturn(true)
        whenever(node.isInShare).thenReturn(true)
        whenever(node.name).thenReturn("testName.txt")
        val favourite = FavouriteFolder(
            id = node.handle,
            name = node.name,
            label = node.label,
            parentId = node.parentHandle,
            base64Id = node.base64Handle,
            hasVersion = false,
            numChildFolders = 0,
            numChildFiles = 0,
            isFavourite = true,
            isExported = false,
            isTakenDown = false,
        )
        val list = listOf(favourite)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(sortOrderIntMapper(SortOrder.ORDER_DEFAULT_ASC)).thenReturn(MegaApiJava.ORDER_DEFAULT_ASC)
        whenever(getAllFavorites()).thenReturn(
            flowOf(list)
        )
        whenever(stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
        whenever(favouriteMapper(any(), any(), any(), any(), any())).thenReturn(mock())
        underTest.favouritesState.test {
            assertTrue(awaitItem() is FavouriteLoadState.Loading)
            assertTrue(awaitItem() is FavouriteLoadState.Success)
        }
    }
}