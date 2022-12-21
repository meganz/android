package mega.privacy.android.app.presentation.favourites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.FavouriteHeaderItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteListItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.FavouritePlaceholderItem
import mega.privacy.android.app.presentation.favourites.model.FavouritesEventState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.utils.Constants.ITEM_PLACEHOLDER_TYPE
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetFavouriteSortOrder
import mega.privacy.android.domain.usecase.MapFavouriteSortOrder
import mega.privacy.android.domain.usecase.RemoveFavourites
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model regarding favourites
 * @param context Context
 * @param getAllFavorites GetAllFavorites
 * @param favouriteMapper FavouriteMapper
 * @param stringUtilWrapper StringUtilWrapper
 * @param removeFavourites RemoveFavourites
 * @param getSortOrder GetCloudSortOrder
 * @param megaUtilWrapper MegaUtilWrapper
 * @param fetchNode FetchNodeWrapper
 */
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getAllFavorites: GetAllFavorites,
    private val favouriteMapper: FavouriteMapper,
    private val stringUtilWrapper: StringUtilWrapper,
    private val removeFavourites: RemoveFavourites,
    private val getSortOrder: GetFavouriteSortOrder,
    private val megaUtilWrapper: MegaUtilWrapper,
    private val fetchNode: FetchNodeWrapper,
    private val mapOrder: MapFavouriteSortOrder,
) :
    ViewModel() {
    private val _favouritesState =
        MutableStateFlow<FavouriteLoadState>(FavouriteLoadState.Loading(false))

    /**
     * The favouritesState for observing the favourites state.
     */
    val favouritesState = _favouritesState.asStateFlow()

    private val _favouritesEventState = MutableSharedFlow<FavouritesEventState>(replay = 0)

    /**
     * The favouritesState for observing the event state.
     */
    val favouritesEventState = _favouritesEventState.asSharedFlow()

    private var currentNodeJob: Job? = null

    private val favouriteSourceList = mutableListOf<Favourite>()

    private val favouriteList = mutableListOf<Favourite>()

    private val itemsSelected = mutableMapOf<Long, Favourite>()

    /**
     * Enable search
     */
    fun enableSearch() {
        searchMode = true
    }

    private var searchMode = false
        set(value) {
            field = value
            _favouritesState.update { state ->
                when (state) {
                    is FavouriteLoadState.Empty -> state.copy(showSearch = value)
                    is FavouriteLoadState.Error -> state.copy(showSearch = value)
                    is FavouriteLoadState.Loading -> state.copy(showSearch = value)
                    is FavouriteLoadState.Success -> state.copy(showSearch = value)
                }
            }
        }

    private var searchQuery: String? = null

    private var isList: Boolean = true

    init {
        getFavourites()
    }

    /**
     * Get favourites
     */
    fun getFavourites() {
        _favouritesState.update {
            FavouriteLoadState.Loading(false)
        }
        // Cancel the previous job avoid to repeatedly observed
        currentNodeJob?.cancel()
        currentNodeJob = viewModelScope.launch {
            runCatching {
                getAllFavorites().collectLatest { favouriteInfoList ->
                    _favouritesState.update {
                        when {
                            favouriteInfoList.isEmpty() -> {
                                FavouriteLoadState.Empty(false)
                            }
                            else -> {
                                withContext(ioDispatcher) {
                                    buildFavouriteSourceList(favouriteInfoList)
                                    FavouriteLoadState.Success(
                                        favouriteListToFavouriteItemList(
                                            favouriteList = reorganizeFavouritesByConditions(
                                                getSortOrder(),
                                                if (searchMode) {
                                                    searchQuery
                                                } else {
                                                    null
                                                }),
                                            isList = isList
                                        ),
                                        showSearch = searchMode
                                    )
                                }
                            }
                        }
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception.message)
            }
        }
    }

    /**
     * Open file
     * @param favourite Favourite
     */
    fun openFile(favourite: Favourite) {
        viewModelScope.launch {
            _favouritesEventState.emit(
                if (favourite.isFolder) {
                    FavouritesEventState.OpenFolder(favourite.nodeId.id)
                } else {
                    FavouritesEventState.OpenFile(favourite as FavouriteFile)
                }
            )
        }
    }

    /**
     * Three dots clicked
     * @param favourite Favourite
     */
    fun threeDotsClicked(favourite: Favourite) {
        viewModelScope.launch {
            _favouritesEventState.emit(
                if (megaUtilWrapper.isOnline(context)) {
                    FavouritesEventState.OpenBottomSheetFragment(favourite)
                } else {
                    FavouritesEventState.Offline
                }
            )
        }
    }

    /**
     * Determine that search menu whether is shown.
     * @return true is shown.
     */
    fun shouldShowSearchMenu() = favouriteList.isNotEmpty()

    /**
     * Filter the items that matches the query
     * @param query search query
     */
    fun searchQuery(query: String) {
        searchQuery = query
        getFavouritesByConditions(query = query)
    }

    /**
     * Exit search mode
     */
    fun exitSearch() {
        searchMode = false
        searchQuery = null
        getFavourites()
    }

    /**
     * The logic that item is clicked
     * @param item the item that is clicked
     */
    fun itemSelected(item: Favourite) {
        val items = favouriteList.map { favourite ->
            if (favourite.nodeId == item.nodeId) {
                if (favourite.isSelected) {
                    itemsSelected.remove(favourite.nodeId.id)
                } else {
                    itemsSelected[favourite.nodeId.id] = favourite
                }
                (favourite as? FavouriteFile)?.copy(isSelected = !favourite.isSelected)
                    ?: (favourite as FavouriteFolder).copy(
                        isSelected = !favourite.isSelected
                    )
            } else {
                favourite
            }
        }
        updateFavouritesStateUnderActionMode(items)
    }

    /**
     * Select all items
     */
    fun selectAll() {
        itemsSelectedClear()
        val items = favouriteList.map {
            (it as? FavouriteFile)?.copy(isSelected = true) ?: (it as FavouriteFolder).copy(
                isSelected = true
            )
        }
        itemsSelected.putAll(
            items.map {
                Pair(it.nodeId.id, it)
            }
        )
        updateFavouritesStateUnderActionMode(items)
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
        itemsSelected.clear()
        val items = favouriteList.map {
            (it as? FavouriteFile)?.copy(isSelected = false) ?: (it as FavouriteFolder).copy(
                isSelected = false
            )
        }
        updateFavouritesStateUnderActionMode(items)
    }

    /**
     * Updated favourites state when items are changed under action mode
     * @param items the new favourite list
     */
    private fun updateFavouritesStateUnderActionMode(items: List<Favourite>) {
        viewModelScope.launch {
            _favouritesEventState.emit(FavouritesEventState.ActionModeState(itemsSelected.size))
            if (favouriteList.isNotEmpty()) {
                favouriteList.clear()
            }
            favouriteList.addAll(items)
            _favouritesState.update {
                FavouriteLoadState.Success(
                    favourites = favouriteListToFavouriteItemList(
                        favouriteList = items,
                        isList = isList
                    ),
                    showSearch = searchMode
                )
            }
        }
    }

    /**
     * Clear the selected item map
     */
    private fun itemsSelectedClear() {
        if (itemsSelected.isNotEmpty()) {
            itemsSelected.clear()
        }
    }

    /**
     * Get the selected item map
     */
    fun getItemsSelected(): Map<Long, Favourite> = itemsSelected

    /**
     * Convert the Favourite list to FavouriteItem list, add the header and placeholder items.
     * @param favouriteList Favourite list
     * @param isList true is list, otherwise is grid
     * @param forceUpdate item whether is force updated
     * @param headerForceUpdate header item whether is force update
     * @return FavouriteItem list
     */
    private suspend fun favouriteListToFavouriteItemList(
        favouriteList: List<Favourite>,
        isList: Boolean,
        forceUpdate: Boolean = false,
        headerForceUpdate: Boolean = false,
        order: FavouriteSortOrder? = null,
    ): List<FavouriteItem> {
        val favouriteItemList = mutableListOf<FavouriteItem>()
        favouriteItemList.add(
            FavouriteHeaderItem(
                favourite = null,
                forceUpdate = headerForceUpdate,
                orderStringId = getFavouriteSortHeaderStringIdentifier(order
                    ?: getSortOrder())
            )
        )
        favouriteList.map { favourite ->
            favouriteItemList.add(
                FavouriteListItem(
                    favourite = favourite,
                    forceUpdate = forceUpdate
                )
            )
        }
        if (isList) {
            if (favouriteItemList.any {
                    it.type == ITEM_PLACEHOLDER_TYPE
                }) {
                favouriteItemList.removeIf {
                    it.type == ITEM_PLACEHOLDER_TYPE
                }
            }
        } else {
            // If the number of favourite folders cannot be divisible by 2,
            // add placeholder view to make sure folder is not displayed with file in same row.
            if (favouriteList.filter { it.isFolder }.size % 2 != 0) {
                favouriteItemList.add(
                    // Get the index of last folder and add placeholder in next position of last folder
                    // The position of last folder is index + 1, so that the next position is index + 2
                    index = favouriteList.indexOfLast { it.isFolder } + 2,
                    element = FavouritePlaceholderItem())
            }
        }

        return favouriteItemList
    }

    private fun getFavouriteSortHeaderStringIdentifier(order: FavouriteSortOrder) = when (order) {
        FavouriteSortOrder.Label -> R.string.title_label
        is FavouriteSortOrder.ModifiedDate -> R.string.sortby_date
        is FavouriteSortOrder.Name -> R.string.sortby_name
        is FavouriteSortOrder.Size -> R.string.sortby_size
    }

    /**
     * Force update data when switch between list and gird
     * @param isList true is list, otherwise is grid
     */
    fun forceUpdateData(isList: Boolean) {
        this.isList = isList
        if (favouriteList.isNotEmpty()) {
            viewModelScope.launch {
                _favouritesState.update {
                    FavouriteLoadState.Success(
                        favouriteListToFavouriteItemList(
                            favouriteList = favouriteList,
                            isList = isList,
                            forceUpdate = true,
                            headerForceUpdate = true
                        ),
                        showSearch = searchMode
                    )
                }
            }
        }
    }

    /**
     * Get favourites by conditions
     * @param order sort order
     * @param query search query
     */
    fun getFavouritesByConditions(
        order: FavouriteSortOrder? = null,
        query: String? = null,
    ) {
        viewModelScope.launch {
            _favouritesState.update {
                FavouriteLoadState.Success(
                    favouriteListToFavouriteItemList(
                        favouriteList = reorganizeFavouritesByConditions(
                            order ?: getSortOrder(),
                            query ?: if (searchMode) {
                                searchQuery
                            } else {
                                null
                            }),
                        isList = isList
                    ),
                    showSearch = searchMode
                )
            }
        }
    }

    /**
     * Reorganize favourites by conditions
     * @param order sort order
     * @param query search query
     * @return List<Favourite>
     */
    private fun reorganizeFavouritesByConditions(
        order: FavouriteSortOrder,
        query: String? = null,
    ): List<Favourite> {
        if (favouriteList.isNotEmpty()) {
            favouriteList.clear()
        }
        favouriteList.addAll(
            getFilteredAndSortedFavourites(order, query)
        )
        return favouriteList
    }

    private fun getFilteredAndSortedFavourites(
        order: FavouriteSortOrder,
        query: String?,
    ): List<Favourite> {
        val sortedList = sortOrder(favouriteSourceList, order)
        return query?.let { getFavouritesByQuery(sortedList, it) } ?: sortedList
    }


    /**
     * Build favourite source list
     * @param list List<FavouriteInfo>
     */
    private suspend fun buildFavouriteSourceList(list: List<TypedNode>) {
        if (favouriteSourceList.isNotEmpty()) {
            favouriteSourceList.clear()
        }
        favouriteSourceList.addAll(
            list.mapNotNull { favouriteInfo ->
                val nodeId = favouriteInfo.id
                val node = fetchNode(nodeId.id) ?: return@mapNotNull null
                favouriteMapper(
                    node,
                    favouriteInfo,
                    megaUtilWrapper.availableOffline(
                        context,
                        nodeId.id
                    ),
                    stringUtilWrapper
                ) { name ->
                    MimeTypeList.typeForName(name).iconResourceId
                }
            }
        )
    }

    /**
     * Sort order for FavouriteInfo list and place the folders at the top
     * @param list favouriteInfo list
     * @param order sort order
     * @return List<FavouriteInfo>
     */
    private fun sortOrder(list: List<Favourite>, order: FavouriteSortOrder): List<Favourite> =
        mutableListOf<Favourite>().apply {
            addAll(list)
            sortWith { item1, item2 ->
                sortOrderByType(
                    sortByType = {
                        when {
                            order is FavouriteSortOrder.Name && !order.sortDescending -> {
                                item1.name.compareTo(item2.name)
                            }
                            order is FavouriteSortOrder.Name && order.sortDescending -> {
                                item2.name.compareTo(item1.name)
                            }
                            order is FavouriteSortOrder.Size && order.sortDescending -> {
                                item2.size.compareTo(item1.size)
                            }
                            order is FavouriteSortOrder.Size && !order.sortDescending -> {
                                item1.size.compareTo(item2.size)
                            }
                            order is FavouriteSortOrder.ModifiedDate && order.sortDescending -> {
                                item2.modificationTime.compareTo(item1.modificationTime)
                            }
                            order is FavouriteSortOrder.ModifiedDate && !order.sortDescending -> {
                                item1.modificationTime.compareTo(item2.modificationTime)
                            }
                            else -> {
                                if (item1.label != 0 && item2.label != 0) {
                                    item1.label.compareTo(item2.label)
                                } else {
                                    item2.label.compareTo(item1.label)
                                }
                            }
                        }
                    },
                    item1 = item1,
                    item2 = item2
                )
            }
        }

    /**
     * Sort order for Favourite list
     * @param list favourite list
     * @param query search query
     * @return List<FavouriteInfo>
     */
    private fun getFavouritesByQuery(
        list: List<Favourite>,
        query: String,
    ): List<Favourite> =
        mutableListOf<Favourite>().apply {
            if (query.isNotEmpty()) {
                addAll(
                    list.filter { info ->
                        info.name.contains(query, true)
                    }
                )
            } else {
                addAll(list)
            }
        }

    /**
     * Sort order for Favourite list by type and place the folders at the top
     * @param sortByType the function for sorting by type
     * @param item1 the first Favourite item for comparing
     * @param item2 the second Favourite item for comparing
     */
    private fun sortOrderByType(
        sortByType: () -> Int,
        item1: Favourite,
        item2: Favourite,
    ): Int {
        return if (item1.isFolder != item2.isFolder) {
            item2.isFolder.compareTo(item1.isFolder)
        } else {
            sortByType()
        }
    }

    /**
     * On order change
     *
     * @param order
     */
    fun onOrderChange(order: SortOrder) {
        getFavouritesByConditions(order = mapOrder(order))
    }
}