package mega.privacy.android.app.presentation.favourites

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.usecase.GetFavouriteFolderInfo
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.*
import mega.privacy.android.app.presentation.mapper.FavouriteMapper
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
    savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    private val _childrenNodesState =
        MutableStateFlow<ChildrenNodesLoadState>(ChildrenNodesLoadState.Loading)
    val childrenNodesState = _childrenNodesState.asStateFlow()

    private val _favouritesEventState = MutableSharedFlow<FavouritesEventState>(replay = 0)
    val favouritesEventState = _favouritesEventState.asSharedFlow()

    private var currentFavouriteFolderInfo: FavouriteFolderInfo? = null

    private var currentRootHandle: Long = -1

    private var currentNodeJob : Job? = null

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
                            children.isNotEmpty() -> {
                                withContext(ioDispatcher) {
                                    ChildrenNodesLoadState.Success(
                                        title = name,
                                        children = children.map { favouriteInfo ->
                                            FavouriteListItem(
                                                favourite = favouriteMapper(
                                                    favouriteInfo,
                                                    { node ->
                                                        megaUtilWrapper.availableOffline(
                                                            context,
                                                            node
                                                        )
                                                    },
                                                    stringUtilWrapper,
                                                    { name ->
                                                        MimeTypeList.typeForName(name).iconResourceId
                                                    }
                                                )
                                            )
                                        })
                                }
                            }
                            else -> {
                                ChildrenNodesLoadState.Loading
                            }
                        }
                    } ?: ChildrenNodesLoadState.Empty(null)
                }
            }
        }
    }

    /**
     * Whether handle back press
     * @return true is handle back pressed
     */
    fun shouldHandleBackPressed(): Boolean {
        currentFavouriteFolderInfo?.run {
            if (currentHandle == currentRootHandle) {
                return true
            } else {
                getChildrenNodes(parentHandle)
            }
        } ?: run {
            return true
        }
        return false
    }

    /**
     * Open file
     * @param favourite Favourite
     */
    fun openFile(favourite: Favourite) {
        if (favourite.isFolder) {
            getChildrenNodes(favourite.handle)
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
        const val KEY_ARGUMENT_PARENT_HANDLE = "parentHandle"
    }
}