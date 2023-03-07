package mega.privacy.android.app.presentation.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteListItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.HeaderMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetFavouriteSortOrder
import mega.privacy.android.domain.usecase.IsAvailableOffline
import mega.privacy.android.domain.usecase.MapFavouriteSortOrder
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.RemoveFavourites
import javax.inject.Inject

/**
 * Favourites view model
 *
 * @property getAllFavorites
 * @property favouriteMapper
 * @property stringUtilWrapper
 * @property removeFavourites
 * @property getSortOrder
 * @property fetchNode
 * @property mapOrder
 * @property headerMapper
 * @property monitorConnectivity
 * @property isAvailableOffline
 */
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val getAllFavorites: GetAllFavorites,
    private val favouriteMapper: FavouriteMapper,
    private val stringUtilWrapper: StringUtilWrapper,
    private val removeFavourites: RemoveFavourites,
    private val getSortOrder: GetFavouriteSortOrder,
    private val fetchNode: FetchNodeWrapper,
    private val mapOrder: MapFavouriteSortOrder,
    private val headerMapper: HeaderMapper,
    private val monitorConnectivity: MonitorConnectivity,
    private val isAvailableOffline: IsAvailableOffline,
) : ViewModel() {

    private val query = MutableStateFlow<String?>(null)
    private lateinit var order: MutableStateFlow<FavouriteSortOrder>
    private val selected = MutableStateFlow<Set<NodeId>>(emptySet())
    private val _state =
        MutableStateFlow<FavouriteLoadState>(FavouriteLoadState.Loading(false, isConnected = true))

    /**
     * The favouritesState for observing the favourites state.
     */
    val favouritesState: StateFlow<FavouriteLoadState> = _state

    private val itemsSelected = mutableMapOf<Long, Favourite>()

    init {
        viewModelScope.launch {
            order = MutableStateFlow(getSortOrder())
            combine(order,
                query,
                getAllFavorites(),
                selected,
                monitorConnectivity(),
                ::mapToFavourite).collectLatest { newState ->
                _state.update { newState }
            }
        }
    }

    private suspend fun mapToFavourite(
        order: FavouriteSortOrder,
        search: String?,
        nodes: List<TypedNode>,
        selectedNodes: Set<NodeId>,
        isConnected: Boolean,
    ): FavouriteLoadState {
        return if (nodes.isEmpty()) FavouriteLoadState.Empty(search != null, isConnected)
        else FavouriteLoadState.Success(
            favourites = createFavouriteItemsList(order, nodes, search, selectedNodes),
            showSearch = search != null,
            selectedItems = selectedNodes,
            isConnected = isConnected,
        )
    }

    private suspend fun createFavouriteItemsList(
        order: FavouriteSortOrder,
        nodes: List<TypedNode>,
        search: String?,
        selectedNodes: Set<NodeId>,
    ) = listOf(
        headerMapper(order)
    ) + nodes.filter {
        search == null || it.name.contains(search, ignoreCase = true)
    }.mapTypedNodesListToFavourites(selectedNodes)
        .sortBy(order)
        .map {
            FavouriteListItem(
                favourite = it,
            )
        }


    /**
     * Determine that search menu whether is shown.
     * @return true is shown.
     */
    fun shouldShowSearchMenu() = _state.value.showSearch

    /**
     * Filter the items that matches the query
     * @param query search query
     */
    fun searchQuery(queryString: String) {
        query.update { queryString }
    }

    /**
     * Exit search mode
     */
    fun exitSearch() {
        query.update { null }
    }

    /**
     * The logic that item is clicked
     * @param item the item that is clicked
     */
    fun itemSelected(item: Favourite) {
        val nodeId = item.typedNode.id
        selected.update {
            if (it.contains(nodeId)) it - nodeId else it + nodeId
        }
    }

    /**
     * Select all items
     */
    fun selectAll() {
        (_state.value as? FavouriteLoadState.Success)?.favourites?.mapNotNull { it.favourite?.typedNode?.id }
            ?.toSet()?.let { allNodes ->
                selected.update { allNodes }
            }
    }

    /**
     * Removing favourites
     * @param nodeHandles the handle of items that are removed favourites
     */
    fun favouritesRemoved(nodeHandles: List<Long>) {
        viewModelScope.launch {
            removeFavourites(nodeHandles)
        }
    }

    /**
     * Clear the items that are selected
     */
    fun clearSelections() {
        selected.update { emptySet() }
    }

    /**
     * Get the selected item map
     */
    fun getItemsSelected(): Map<Long, Favourite> = itemsSelected

    private suspend fun List<TypedNode>.mapTypedNodesListToFavourites(selectedNodes: Set<NodeId>): List<Favourite> =
        mapNotNull { favouriteInfo ->
            val nodeId = favouriteInfo.id
            val node = this@FavouritesViewModel.fetchNode(nodeId.longValue) ?: return@mapNotNull null
            this@FavouritesViewModel.favouriteMapper(
                node,
                favouriteInfo,
                isAvailableOffline(favouriteInfo),
                stringUtilWrapper,
                selectedNodes.contains(nodeId),
            ) { name ->
                MimeTypeList.typeForName(name).iconResourceId
            }
        }

    /**
     * Sort order for FavouriteInfo list and place the folders at the top
     * @param this@sortOrder favouriteInfo list
     * @param order sort order
     * @return List<FavouriteInfo>
     */
    private fun List<Favourite>.sortBy(order: FavouriteSortOrder): List<Favourite> =
        this.sortedWith { item1, item2 ->
            if (order.sortDescending) {
                item2.typedNode.compareTo(item1.typedNode, order)
            } else {
                item1.typedNode.compareTo(item2.typedNode, order)
            }
        }

    private fun TypedNode.compareTo(other: TypedNode, order: FavouriteSortOrder): Int {
        return when (this) {
            is TypedFileNode -> this.compareToFile(other, order)
            is TypedFolderNode -> this.compareToFolder(other, order)
        }
    }


    private fun TypedFileNode.compareToFile(
        other: TypedNode,
        order: FavouriteSortOrder,
    ): Int {
        val otherFile = other as? TypedFileNode ?: return compareFileToFolder(order)
        return when (order) {
            FavouriteSortOrder.Label -> label.compareTo(otherFile.label)
            is FavouriteSortOrder.ModifiedDate -> modificationTime.compareTo(otherFile.modificationTime)
            is FavouriteSortOrder.Name -> name.compareTo(otherFile.name)
            is FavouriteSortOrder.Size -> size.compareTo(otherFile.size)
        }
    }

    private fun compareFileToFolder(order: FavouriteSortOrder) = if (order.sortDescending) -1 else 1

    private fun TypedFolderNode.compareToFolder(
        other: TypedNode,
        order: FavouriteSortOrder,
    ): Int {
        val otherFolder = other as? TypedFolderNode ?: return compareFolderToFile(order)
        return if (order is FavouriteSortOrder.Label) label.compareTo(otherFolder.label) else name.compareTo(
            otherFolder.name)
    }

    private fun compareFolderToFile(order: FavouriteSortOrder) = if (order.sortDescending) 1 else -1

    /**
     * On order change
     *
     * @param order
     */
    fun onOrderChange(sortOrder: SortOrder) {
        if (this::order.isInitialized) order.update { mapOrder(sortOrder) }
    }

}