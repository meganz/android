package mega.privacy.android.app.presentation.favourites

import android.content.Context
import androidx.lifecycle.SavedStateHandle
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
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteListItem
import mega.privacy.android.app.presentation.favourites.model.FavouritesEventState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetFavouriteFolderInfo
import javax.inject.Inject

/**
 * The view model regarding favourite folder
 * @param context Context
 * @param getFavouriteFolderInfo DefaultGetChildrenByNode
 * @param favouriteMapper FavouriteMapper
 * @param stringUtilWrapper StringUtilWrapper
 * @param savedStateHandle SavedStateHandle
 */
@HiltViewModel
class FavouriteFolderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getFavouriteFolderInfo: GetFavouriteFolderInfo,
    private val favouriteMapper: FavouriteMapper,
    private val stringUtilWrapper: StringUtilWrapper,
    private val megaUtilWrapper: MegaUtilWrapper,
    private val fetchNode: FetchNodeWrapper,
    savedStateHandle: SavedStateHandle,
) :
    ViewModel() {

    private val _childrenNodesState =
        MutableStateFlow<ChildrenNodesLoadState>(ChildrenNodesLoadState.Loading)

    /**
     * The state regarding children nodes
     */
    val childrenNodesState = _childrenNodesState.asStateFlow()

    private val _favouritesEventState = MutableSharedFlow<FavouritesEventState>(replay = 0)

    /**
     * The state regarding events
     */
    val favouritesEventState = _favouritesEventState.asSharedFlow()

    private var currentFavouriteFolderInfo: FavouriteFolderInfo? = null

    private var currentRootHandle: Long = -1

    private var currentNodeJob: Job? = null

    init {
        currentRootHandle = savedStateHandle[KEY_ARGUMENT_PARENT_HANDLE] ?: -1
        getChildrenNodes(currentRootHandle)
    }

    /**
     * Get children nodes
     * @param parentHandle the parent node handle
     */
    private fun getChildrenNodes(parentHandle: Long) {
        _childrenNodesState.update {
            ChildrenNodesLoadState.Loading
        }
        // Cancel the previous job avoid to repeatedly observed
        currentNodeJob?.cancel()
        currentNodeJob = viewModelScope.launch {
            getFavouriteFolderInfo(parentHandle).collectLatest { folderInfo ->
                currentFavouriteFolderInfo = folderInfo
                _childrenNodesState.update {
                    folderInfo?.run {
                        when {
                            children.isEmpty() -> {
                                ChildrenNodesLoadState.Empty(folderInfo.name)
                            }
                            children.isNotEmpty() -> getData(name, children, folderInfo)
                            else -> {
                                ChildrenNodesLoadState.Loading
                            }
                        }
                    } ?: ChildrenNodesLoadState.Empty(null)
                }
            }
        }
    }

    private suspend fun getData(
        name: String,
        children: List<TypedNode>,
        folderInfo: FavouriteFolderInfo
    ): ChildrenNodesLoadState {
        return withContext(ioDispatcher) {
            ChildrenNodesLoadState.Success(
                title = name,
                children = children.mapNotNull { favouriteInfo ->
                    val node = fetchNode(favouriteInfo.id.id) ?: return@mapNotNull null
                    FavouriteListItem(
                        favourite = favouriteMapper(
                            node,
                            favouriteInfo,
                            megaUtilWrapper.availableOffline(
                                context,
                                favouriteInfo.id.id
                            ),
                            stringUtilWrapper
                        ) { name ->
                            MimeTypeList.typeForName(name).iconResourceId
                        }
                    )
                },
                // If current handle is not current root handle, enable onBackPressedCallback
                isBackPressedEnable = folderInfo.currentHandle != currentRootHandle
            )
        }
    }


    /**
     * Back to previous level page
     */
    fun backToPreviousPage() {
        currentFavouriteFolderInfo?.run {
            getChildrenNodes(parentHandle)
        }
    }

    /**
     * Open file
     * @param favourite Favourite
     */
    fun openFile(favourite: Favourite) {
        if (favourite.isFolder) {
            getChildrenNodes(favourite.nodeId.id)
        } else {
            viewModelScope.launch {
                _favouritesEventState.emit(FavouritesEventState.OpenFile(favourite as FavouriteFile))
            }
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

    companion object {

        /**
         * The key of parent handle argument
         */
        const val KEY_ARGUMENT_PARENT_HANDLE = "parentHandle"
    }
}