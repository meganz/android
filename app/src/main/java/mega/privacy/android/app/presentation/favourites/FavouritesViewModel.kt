package mega.privacy.android.app.presentation.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteListItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.favourites.model.mapper.HeaderMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import mega.privacy.android.domain.usecase.favourites.GetFavouriteSortOrderUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.favourites.MapFavouriteSortOrderUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Favourites view model
 *
 * @property getAllFavoritesUseCase
 * @property favouriteMapper
 * @property stringUtilWrapper
 * @property removeFavouritesUseCase
 * @property getFavouriteSortOrderUseCase
 * @property fetchNodeWrapper
 * @property mapFavouriteSortOrderUseCase
 * @property headerMapper
 * @property monitorConnectivityUseCase
 * @property isAvailableOfflineUseCase
 */
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val getAllFavoritesUseCase: GetAllFavoritesUseCase,
    private val favouriteMapper: FavouriteMapper,
    private val stringUtilWrapper: StringUtilWrapper,
    private val removeFavouritesUseCase: RemoveFavouritesUseCase,
    private val getFavouriteSortOrderUseCase: GetFavouriteSortOrderUseCase,
    private val fetchNodeWrapper: FetchNodeWrapper,
    private val mapFavouriteSortOrderUseCase: MapFavouriteSortOrderUseCase,
    private val headerMapper: HeaderMapper,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isHidingActionAllowedUseCase: IsHidingActionAllowedUseCase,
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : ViewModel() {

    private var isConnected: Boolean = true
    private lateinit var order: MutableStateFlow<FavouriteSortOrder>
    private val selected = MutableStateFlow<Set<NodeId>>(emptySet())
    private val _state =
        MutableStateFlow<FavouriteLoadState>(FavouriteLoadState.Loading(isConnected = true))

    /**
     * The favouritesState for observing the favourites state.
     */
    val favouritesState: StateFlow<FavouriteLoadState> = _state

    private val itemsSelected = mutableMapOf<Long, Favourite>()

    init {
        viewModelScope.launch {
            getFavouriteLoadStateFlow()
                .catch { Timber.e(it) }
                .collectLatest { newState ->
                    _state.update { newState }
                }
        }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun combineFavouriteLoadStateFlow(): Flow<FavouriteLoadState> {
        order = MutableStateFlow(getFavouriteSortOrderUseCase())
        return combine(
            order,
            order.flatMapLatest {
                getAllFavoritesUseCase()
            },
            selected,
            monitorConnectivityUseCase(),
            ::mapToFavourite
        )
    }

    private suspend fun getFavouriteLoadStateFlow(): Flow<FavouriteLoadState> {
        val favouritesFlow = combineFavouriteLoadStateFlow()
        return if (isHiddenNodesActive()) {
            val accountDetailFlow = monitorAccountDetailUseCase()
            val isHiddenNodesOnboardedFlow = flowOf(isHiddenNodesOnboardedUseCase())
            combine(
                accountDetailFlow,
                isHiddenNodesOnboardedFlow,
                favouritesFlow,
                monitorShowHiddenItemsUseCase(),
            ) { accountDetail, isHiddenNodesOnboarded, favouritesState, showHiddenItems ->
                if (favouritesState is FavouriteLoadState.Success) {
                    val accountType = accountDetail.levelDetail?.accountType
                    val businessStatus =
                        if (accountType?.isBusinessAccount == true) {
                            getBusinessStatusUseCase()
                        } else null

                    val isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired

                    val filteredItems = filterNonSensitiveItems(
                        items = favouritesState.favourites,
                        showHiddenItems = showHiddenItems,
                        isPaid = accountType?.isPaid,
                        isBusinessAccountExpired = isBusinessAccountExpired,
                    )
                    if (filteredItems.any { it is FavouriteListItem }) {
                        favouritesState.copy(
                            favourites = filteredItems,
                            accountType = accountType,
                            isBusinessAccountExpired = isBusinessAccountExpired,
                            isHiddenNodesOnboarded = isHiddenNodesOnboarded,
                            hiddenNodeEnabled = true,
                        )
                    } else {
                        FavouriteLoadState.Empty(isConnected)
                    }
                } else {
                    favouritesState
                }
            }
        } else {
            favouritesFlow
        }
    }

    private suspend fun filterNonSensitiveItems(
        items: List<FavouriteItem>,
        showHiddenItems: Boolean?,
        isPaid: Boolean?,
        isBusinessAccountExpired: Boolean,
    ) = withContext(defaultDispatcher) {
        showHiddenItems ?: return@withContext items
        isPaid ?: return@withContext items

        return@withContext if (showHiddenItems || !isPaid || isBusinessAccountExpired) {
            items
        } else {
            items.filter {
                it.favourite?.typedNode?.let { typedNode ->
                    !typedNode.isMarkedSensitive && !typedNode.isSensitiveInherited
                } ?: true
            }
        }
    }

    private suspend fun mapToFavourite(
        order: FavouriteSortOrder,
        nodes: List<TypedNode>,
        selectedNodes: Set<NodeId>,
        isConnected: Boolean,
    ): FavouriteLoadState {
        this@FavouritesViewModel.isConnected = isConnected
        return if (nodes.isEmpty()) FavouriteLoadState.Empty(isConnected)
        else FavouriteLoadState.Success(
            favourites = createFavouriteItemsList(order, nodes, selectedNodes),
            selectedItems = selectedNodes,
            isConnected = isConnected,
        )
    }

    private suspend fun createFavouriteItemsList(
        order: FavouriteSortOrder,
        nodes: List<TypedNode>,
        selectedNodes: Set<NodeId>,
    ) = listOf(
        headerMapper(order)
    ) + nodes.mapTypedNodesListToFavourites(selectedNodes)
        .map {
            FavouriteListItem(
                favourite = it,
            )
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
        itemsSelected[nodeId.longValue]?.let {
            itemsSelected.remove(nodeId.longValue)
        } ?: run {
            itemsSelected[nodeId.longValue] = item
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
        (_state.value as? FavouriteLoadState.Success)?.favourites?.mapNotNull { it.favourite }
            ?.forEach { favorite ->
                itemsSelected[favorite.typedNode.id.longValue] = favorite
            }
    }

    /**
     * Removing favourites
     * @param nodeHandles the handle of items that are removed favourites
     */
    fun favouritesRemoved(nodeHandles: List<Long>) {
        viewModelScope.launch {
            removeFavouritesUseCase(nodeHandles.map { NodeId(it) })
        }
    }

    /**
     * Clear the items that are selected
     */
    fun clearSelections() {
        selected.update { emptySet() }
        itemsSelected.clear()
    }

    /**
     * Get the selected item map
     */
    fun getItemsSelected(): Map<Long, Favourite> = itemsSelected

    private suspend fun List<TypedNode>.mapTypedNodesListToFavourites(selectedNodes: Set<NodeId>): List<Favourite> =
        mapNotNull { favouriteInfo ->
            val nodeId = favouriteInfo.id
            val node =
                this@FavouritesViewModel.fetchNodeWrapper(nodeId.longValue)
                    ?: return@mapNotNull null
            runCatching {
                this@FavouritesViewModel.favouriteMapper(
                    node,
                    favouriteInfo,
                    isAvailableOfflineUseCase(favouriteInfo),
                    stringUtilWrapper,
                    selectedNodes.contains(nodeId),
                ) { name ->
                    MimeTypeList.typeForName(name).iconResourceId
                }
            }.onFailure {
                Timber.e(it)
            }.getOrNull()
        }

    /**
     * On order change
     *
     * @param sortOrder
     */
    fun onOrderChange(sortOrder: SortOrder) {
        if (this::order.isInitialized) order.update { mapFavouriteSortOrderUseCase(sortOrder) }
    }

    fun hideOrUnhideNodes(nodeIds: List<NodeId>, hide: Boolean) = viewModelScope.launch {
        for (nodeId in nodeIds) {
            async {
                runCatching {
                    updateNodeSensitiveUseCase(nodeId = nodeId, isSensitive = hide)
                }.onFailure { Timber.e("Update sensitivity failed: ", it) }
            }
        }
    }

    fun getIsPaidAccount(): Boolean {
        val state = _state.value
        return (state as? FavouriteLoadState.Success)
            ?.accountType
            ?.isPaid ?: false
    }

    fun getIsBusinessAccountExpired(): Boolean {
        val state = _state.value
        return (state as? FavouriteLoadState.Success)
            ?.isBusinessAccountExpired ?: false
    }

    fun isHiddenNodesOnboarded(): Boolean {
        return (_state.value as? FavouriteLoadState.Success)?.isHiddenNodesOnboarded ?: false
    }

    fun setHiddenNodesOnboarded() {
        _state.value.let {
            if (it is FavouriteLoadState.Success) {
                it.copy(isHiddenNodesOnboarded = true)
            } else {
                it
            }
        }
    }

    /**
     * Check if the current node can be hidden
     */
    suspend fun isHidingActionAllowed(nodeId: NodeId): Boolean =
        isHidingActionAllowedUseCase(nodeId)

    internal fun getFileTypeInfo(name: String) = runCatching {
        getFileTypeInfoByNameUseCase(name)
    }.recover {
        Timber.e(it)
        null
    }.getOrNull()

    internal suspend fun getNodeContentUri(fileNode: TypedFileNode) =
        getNodeContentUriUseCase(fileNode)
}