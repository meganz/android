package mega.privacy.android.app.presentation.favourites

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.entity.FavouriteFolderInfo
import mega.privacy.android.app.domain.usecase.GetFavouriteFolderInfo
import mega.privacy.android.app.presentation.extensions.toFavourite
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.favourites.model.ClickEventState
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The view model regarding favourite folder
 * @param context Context
 * @param getFavouriteFolderInfo DefaultGetChildrenByNode
 * @param stringUtilWrapper StringUtilWrapper
 * @param savedStateHandle SavedStateHandle
 */
@HiltViewModel
class FavouriteFolderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getFavouriteFolderInfo: GetFavouriteFolderInfo,
    private val stringUtilWrapper: StringUtilWrapper,
    private val megaUtilWrapper: MegaUtilWrapper,
    savedStateHandle: SavedStateHandle
) :
    ViewModel() {

    private val _childrenNodesState =
        MutableStateFlow<ChildrenNodesLoadState>(ChildrenNodesLoadState.Loading)
    val childrenNodesState = _childrenNodesState.asStateFlow()

    private val _clickEventState = MutableSharedFlow<ClickEventState>(replay = 0)
    val clickEventState = _clickEventState.asSharedFlow()

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
            getFavouriteFolderInfo(parentHandle).collect { folderInfo ->
                currentFavouriteFolderInfo = folderInfo
                _childrenNodesState.update {
                    when {
                        folderInfo.children.isNullOrEmpty() -> {
                            ChildrenNodesLoadState.Empty(folderInfo.name)
                        }
                        folderInfo.children.isNotEmpty() -> {
                            ChildrenNodesLoadState.Success(
                                folderInfo.name,
                                folderInfo.children.map { favouriteInfo ->
                                    favouriteInfo.toFavourite(
                                        { node: MegaNode ->
                                            megaUtilWrapper.availableOffline(
                                                context,
                                                node
                                            )
                                        },
                                        stringUtilWrapper
                                    )
                                })
                        }
                        else -> {
                            ChildrenNodesLoadState.Loading
                        }
                    }
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
                _clickEventState.emit(ClickEventState.OpenFile(favourite as FavouriteFile))
            }
        }
    }

    /**
     * Three dots clicked
     * @param favourite Favourite
     */
    fun threeDotsClicked(favourite: Favourite) {
        viewModelScope.launch {
            _clickEventState.emit(
                if (megaUtilWrapper.isOnline(context)) {
                    ClickEventState.OpenBottomSheetFragment(favourite)
                } else {
                    ClickEventState.Offline
                }
            )
        }
    }

    companion object {
        const val KEY_ARGUMENT_PARENT_HANDLE = "parentHandle"
    }
}