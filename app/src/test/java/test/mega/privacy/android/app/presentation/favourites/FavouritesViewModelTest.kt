package test.mega.privacy.android.app.presentation.favourites

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.favourites.FavouritesViewModel
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetFavouriteSortOrder
import mega.privacy.android.domain.usecase.MapFavouriteSortOrder
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class FavouritesViewModelTest {
    private lateinit var underTest: FavouritesViewModel

    private val getAllFavorites = mock<GetAllFavorites>()
    private val stringUtilWrapper = mock<StringUtilWrapper>()
    private val favouriteMapper = mock<FavouriteMapper>()
    private val getSortOrder = mock<GetFavouriteSortOrder>()

    private val megaNode = mock<MegaNode>()

    private val fetchNodeWrapper =
        mock<FetchNodeWrapper> { onBlocking { invoke(any()) }.thenReturn(megaNode) }

    private val mapFavouriteSortOrder = mock<MapFavouriteSortOrder>()

    private val megaUtilWrapper =
        mock<MegaUtilWrapper> { onBlocking { availableOffline(any(), any()) }.thenReturn(false) }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = FavouritesViewModel(
            context = mock(),
            ioDispatcher = UnconfinedTestDispatcher(),
            getAllFavorites = getAllFavorites,
            stringUtilWrapper = stringUtilWrapper,
            megaUtilWrapper = megaUtilWrapper,
            getSortOrder = getSortOrder,
            removeFavourites = mock(),
            favouriteMapper = favouriteMapper,
            fetchNode = fetchNodeWrapper,
            mapOrder = mapFavouriteSortOrder,
        )
    }

    @Test
    fun `test default state`() = runTest {
        underTest.favouritesState.test {
            assertThat(awaitItem()).isEqualTo(FavouriteLoadState.Loading)
        }
    }

    @Test
    fun `test that start with loading state and there is no favourite item`() = runTest {
        whenever(getSortOrder()).thenReturn(FavouriteSortOrder.Name(false))
        whenever(getAllFavorites()).thenReturn(
            flowOf(emptyList())
        )
        underTest.favouritesState.test {
            assertThat(awaitItem()).isEqualTo(FavouriteLoadState.Loading)
            assertThat(awaitItem()).isEqualTo(FavouriteLoadState.Empty)
        }
    }

    @Test
    fun `test that start with loading state and load favourites success`() = runTest {
        val favourite = mock<TypedNode>()
        val list = listOf(favourite)
        whenever(getSortOrder()).thenReturn(FavouriteSortOrder.Name(false))
        whenever(getAllFavorites()).thenReturn(
            flowOf(list)
        )
        whenever(stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
        whenever(favouriteMapper(any(), any(), any(), any(), any())).thenReturn(mock())
        underTest.favouritesState.test {
            assertThat(awaitItem()).isEqualTo(FavouriteLoadState.Loading)
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
        }
    }

    @Test
    fun `test that favourites are mapped acording to returned order`() = runTest {
        val timeAscending = 1L..10L
        val nodes = timeAscending.map { time ->
            mock<TypedFileNode> { on { modificationTime }.thenReturn(time) }
        }

        whenever(getAllFavorites()).thenReturn(flowOf(nodes))
        whenever(getSortOrder()).thenReturn(FavouriteSortOrder.ModifiedDate(false))
        whenever(favouriteMapper(any(), any(), any(), any(), any())).thenAnswer {
            val time = (it.arguments[1] as TypedFileNode).modificationTime
            mock<Favourite> { on { modificationTime }.thenReturn(time) }
        }
        whenever(stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")

        underTest.favouritesState.test {
            assertThat(awaitItem()).isEqualTo(FavouriteLoadState.Loading)
            val items = awaitItem()
            assertThat(items).isInstanceOf(FavouriteLoadState.Success::class.java)
            assertThat((items as FavouriteLoadState.Success).favourites.drop(1)
                .map { it.favourite?.modificationTime }).containsExactlyElementsIn(
                timeAscending).inOrder()
        }

    }

    @Test
    fun `test that sort order is changed when set from event`() = runTest {
        val timeAscending = 1L..10L
        val nodes = timeAscending.map { time ->
            mock<TypedFileNode> { on { modificationTime }.thenReturn(time) }
        }

        whenever(getAllFavorites()).thenReturn(flowOf(nodes))
        val sortOrder = FavouriteSortOrder.ModifiedDate(false)
        whenever(getSortOrder()).thenReturn(sortOrder)
        whenever(favouriteMapper(any(), any(), any(), any(), any())).thenAnswer {
            val time = (it.arguments[1] as TypedFileNode).modificationTime
            mock<Favourite> { on { modificationTime }.thenReturn(time) }
        }
        whenever(stringUtilWrapper.getFolderInfo(0, 0)).thenReturn("info")
        whenever(mapFavouriteSortOrder(any())).thenReturn(sortOrder.copy(sortDescending = !sortOrder.sortDescending))


        underTest.favouritesState.test {
            assertThat(awaitItem()).isEqualTo(FavouriteLoadState.Loading)
            val items = awaitItem()
            assertThat(items).isInstanceOf(FavouriteLoadState.Success::class.java)
            assertThat((items as FavouriteLoadState.Success).favourites.drop(1)
                .map { it.favourite?.modificationTime }).containsExactlyElementsIn(
                timeAscending).inOrder()
            underTest.onOrderChange(SortOrder.ORDER_CREATION_DESC)
            val sortedItems = awaitItem()
            assertThat((sortedItems as FavouriteLoadState.Success).favourites.drop(1)
                .map { it.favourite?.modificationTime }).containsExactlyElementsIn(
                timeAscending.reversed()).inOrder()
        }
    }


}