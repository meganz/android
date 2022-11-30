package test.mega.privacy.android.app.presentation.favourites

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.favourites.FavouritesViewModel
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.toHeader
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetFavouriteSortOrder
import mega.privacy.android.domain.usecase.IsAvailableOffline
import mega.privacy.android.domain.usecase.MapFavouriteSortOrder
import mega.privacy.android.domain.usecase.MonitorConnectivity
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class FavouritesViewModelTest {
    private lateinit var underTest: FavouritesViewModel

    private val stringUtilWrapper =
        mock<StringUtilWrapper> { on { getFolderInfo(any(), any()) }.thenReturn("info") }
    private val favouriteMapper = mock<FavouriteMapper> {
        on { invoke(any(), any(), any(), any(), any(), any()) }
            .thenAnswer {
                val typedFileNode = it.arguments[1] as TypedFileNode
                mock<FavouriteFile> {
                    on { typedNode }.thenReturn(typedFileNode)
                }
            }
    }
    private val getSortOrder = mock<GetFavouriteSortOrder> {
        onBlocking { invoke() }.thenReturn(FavouriteSortOrder.ModifiedDate(false))
    }

    private val megaNode = mock<MegaNode>()

    private val fetchNodeWrapper =
        mock<FetchNodeWrapper> { onBlocking { invoke(any()) }.thenReturn(megaNode) }

    private val mapFavouriteSortOrder = mock<MapFavouriteSortOrder>()

    private val isAvailableOffline = mock<IsAvailableOffline> {
        onBlocking { invoke(any()) }.thenReturn(false)
    }

    private val evenString = "Even"
    private val oddString = "Odd"
    private val timeDescending = 10L downTo 1L
    private val descendingTimeNodes = timeDescending.map { time ->
        val nameString = if (time.mod(2) == 0) "$evenString $time" else "$oddString $time"
        mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(time))
            on { modificationTime }.thenReturn(time)
            on { name }.thenReturn(nameString)
        }
    }
    private val getAllFavorites =
        mock<GetAllFavorites> { on { invoke() }.thenReturn(flowOf(descendingTimeNodes)) }

    private val scheduler = TestCoroutineScheduler()

    private val connectedFlow = MutableStateFlow(false)

    private val monitorConnectivity = mock<MonitorConnectivity> {
        on { invoke() }.thenReturn(connectedFlow)
    }


    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = FavouritesViewModel(
            getAllFavorites = getAllFavorites,
            stringUtilWrapper = stringUtilWrapper,
            getSortOrder = getSortOrder,
            removeFavourites = mock(),
            favouriteMapper = favouriteMapper,
            fetchNode = fetchNodeWrapper,
            mapOrder = mapFavouriteSortOrder,
            headerMapper = ::toHeader,
            monitorConnectivity = monitorConnectivity,
            isAvailableOffline = isAvailableOffline
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test default state`() = runTest {
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
        }
    }

    @Test
    fun `test empty state`() = runTest {
        whenever(getAllFavorites()).thenReturn(
            flowOf(emptyList())
        )
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Empty::class.java)
        }
    }

    @Test
    fun `test load favourites success`() = runTest {
        whenever(getAllFavorites()).thenReturn(
            flowOf(descendingTimeNodes)
        )
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
        }
    }

    @Test
    fun `test that favourites are mapped according to returned order`() = runTest {
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            val items = awaitItem()
            verifyDefaultSortOrder(items)
        }
    }

    @Test
    fun `test that sort order is changed when set from event`() = runTest {
        val sortOrder = FavouriteSortOrder.ModifiedDate(false)
        whenever(getSortOrder()).thenReturn(sortOrder)
        whenever(mapFavouriteSortOrder(any())).thenReturn(sortOrder.copy(sortDescending = !sortOrder.sortDescending))

        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            val items = awaitItem()
            verifyDefaultSortOrder(items)
            underTest.onOrderChange(SortOrder.ORDER_CREATION_DESC)
            scheduler.advanceUntilIdle()
            val sortedItems = awaitItem()
            verifyInOrder(sortedItems, timeDescending)
        }
    }


    @Test
    fun `test that show search is false by default`() = runTest {
        underTest.favouritesState.test {
            assertThat(awaitItem().showSearch).isFalse()
        }
    }

    @Test
    fun `test that showSearch value is set to true if viewmodel is set to search mode`() = runTest {
        underTest.favouritesState.test {
            assertThat(awaitItem().showSearch).isFalse()
            underTest.searchQuery("")
            assertThat(awaitItem().showSearch).isTrue()
        }
    }

    @Test
    fun `test that list is filtered according to query string`() = runTest {
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            val items = awaitItem()
            verifyDefaultSortOrder(items)

            underTest.searchQuery(evenString)

            val filteredItems = awaitItem()
            assertThat(filteredItems).isInstanceOf(FavouriteLoadState.Success::class.java)
            assertThat((filteredItems as FavouriteLoadState.Success).favourites.drop(1)
                .mapNotNull { it.favourite?.typedNode?.name }
                .all { it.startsWith(evenString) }).isTrue()
        }
    }

    @Test
    fun `test that s filtered list retains its sort order`() = runTest {
        val timeDescendingOddOnly = timeDescending.filter { it.mod(2) != 0 }
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            val items = awaitItem()
            verifyDefaultSortOrder(items)

            underTest.searchQuery(oddString)

            val filteredItems = awaitItem()
            val expected = timeDescendingOddOnly.reversed()
            verifyInOrder(filteredItems, expected)
        }
    }

    @Test
    fun `test that sorted list retain their filter`() = runTest {
        val timeDescendingOddOnly = timeDescending.filter { it.mod(2) != 0 }
        whenever(mapFavouriteSortOrder(any())).thenReturn(FavouriteSortOrder.ModifiedDate(true))
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            val items = awaitItem()
            verifyDefaultSortOrder(items)

            underTest.searchQuery(oddString)

            val filteredItems = awaitItem()
            val expected = timeDescendingOddOnly.reversed()
            verifyInOrder(filteredItems,
                expected,
                "Items contain odd values in ascending time order")

            underTest.onOrderChange(SortOrder.ORDER_CREATION_DESC)

            val sortedItems = awaitItem()
            verifyInOrder(sortedItems,
                timeDescendingOddOnly,
                "Items contain odd values in descending time order")
            assertThat((sortedItems as FavouriteLoadState.Success).favourites.drop(1)
                .mapNotNull { it.favourite?.typedNode?.name }
                .all { it.startsWith(oddString) }).isTrue()
        }
    }

    @Test
    fun `test that selecting a node adds the id to selected node list`() = runTest {
        val expected = timeDescending
            .map { longValue ->
                mock<TypedFileNode> {
                    on { id }.thenReturn(NodeId(longValue))
                }
            }.first()
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
            underTest.itemSelected(mock { on { typedNode }.thenReturn(expected) })
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).containsExactly(
                expected.id)
        }
    }

    @Test
    fun `test that selecting items again removes them from the list `() = runTest {
        val expected = timeDescending
            .map { longValue ->
                mock<TypedFileNode> {
                    on { id }.thenReturn(NodeId(longValue))
                }
            }.first()
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
            val selected = mock<Favourite> { on { typedNode }.thenReturn(expected) }
            underTest.itemSelected(selected)
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).containsExactly(
                expected.id)
            underTest.itemSelected(selected)
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).isEmpty()
        }
    }

    @Test
    fun `test that select all selects the whole list`() = runTest {
        val expected = timeDescending
            .map { NodeId(it) }
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
            underTest.selectAll()
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).containsExactlyElementsIn(
                expected)
        }
    }

    @Test
    fun `test that clear selection returns an empty selected set`() = runTest {
        val expected = timeDescending
            .map { NodeId(it) }
        underTest.favouritesState.test {
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(FavouriteLoadState.Success::class.java)
            underTest.selectAll()
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).containsExactlyElementsIn(
                expected)
            underTest.clearSelections()
            assertThat((awaitItem() as FavouriteLoadState.Success).selectedItems).isEmpty()
        }
    }


    private fun verifyDefaultSortOrder(items: FavouriteLoadState) {
        val expected = timeDescending.reversed()
        verifyInOrder(items, expected)
    }

    private fun verifyInOrder(
        items: FavouriteLoadState,
        expected: Iterable<Long>,
        message: String? = "Verify in order failed",
    ) {
        assertThat(items).isInstanceOf(FavouriteLoadState.Success::class.java)
        assertWithMessage(message).that((items as FavouriteLoadState.Success).favourites.drop(
            1)
            .map { (it.favourite?.typedNode as? TypedFileNode)?.modificationTime })
            .containsExactlyElementsIn(
                expected).inOrder()
    }

    @Test
    fun `test that initial connected state matches current state`() = runTest {
        val currentConnectedState = connectedFlow.value
        underTest.favouritesState.filterNot { it is FavouriteLoadState.Loading }.test {
            assertThat(awaitItem().isConnected).isEqualTo(currentConnectedState)
        }
    }

    @Test
    fun `test that subsequent connected state matches latest value`() = runTest {
        connectedFlow.emit(true)

        val initial = connectedFlow.value
        connectedFlow.emit(!initial)
        underTest.favouritesState.test {
            assertThat(awaitItem().isConnected).isEqualTo(initial)
            assertThat(awaitItem().isConnected).isEqualTo(!initial)
        }
    }

}