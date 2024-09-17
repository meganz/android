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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.FavouriteListItem
import mega.privacy.android.app.presentation.favourites.model.FavouritesEventState
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.GetFavouriteFolderInfoUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The view model regarding favourite folder
 * @property context Context
 * @property ioDispatcher CoroutineDispatcher
 * @property getFavouriteFolderInfoUseCase GetFavouriteFolderInfoUseCase
 * @property favouriteMapper FavouriteMapper
 * @property stringUtilWrapper StringUtilWrapper
 * @property megaUtilWrapper MegaUtilWrapper
 * @property fetchNodeWrapper FetchNodeWrapper
 */
@HiltViewModel
class FavouriteFolderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getFavouriteFolderInfoUseCase: GetFavouriteFolderInfoUseCase,
    private val favouriteMapper: FavouriteMapper,
    private val stringUtilWrapper: StringUtilWrapper,
    private val megaUtilWrapper: MegaUtilWrapper,
    private val fetchNodeWrapper: FetchNodeWrapper,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
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

    private suspend fun handleHiddenNodes(parentHandle: Long) {
        combine(
            getFavouriteFolderInfoUseCase(parentHandle),
            monitorAccountDetailUseCase(),
            monitorShowHiddenItemsUseCase(),
        ) { folderInfo, accountDetail, showHiddenItems ->
            currentFavouriteFolderInfo = folderInfo
            Triple(folderInfo, accountDetail.levelDetail?.accountType, showHiddenItems)
        }.catch { Timber.e(it) }
            .collectLatest { (folderInfo, accountType, showHiddenItems) ->
                val filteredChildren = filterNonSensitiveItems(
                    folderInfo.children,
                    showHiddenItems,
                    accountType?.isPaid
                )
                _childrenNodesState.update {
                    folderInfo.run {
                        when {
                            filteredChildren.isEmpty() -> {
                                ChildrenNodesLoadState.Empty(folderInfo.name)
                            }

                            else -> getData(
                                name = name,
                                children = filteredChildren,
                                currentHandle = folderInfo.currentHandle,
                                accountType = accountType,
                            )
                        }
                    }
                }
            }
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
            if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
                handleHiddenNodes(parentHandle = parentHandle)
            } else {
                getFavouriteFolderInfoUseCase(parentHandle).collectLatest { folderInfo ->
                    currentFavouriteFolderInfo = folderInfo
                    _childrenNodesState.update {
                        folderInfo.run {
                            when {
                                children.isEmpty() -> {
                                    ChildrenNodesLoadState.Empty(folderInfo.name)
                                }

                                else -> getData(
                                    name = name,
                                    children = children,
                                    currentHandle = folderInfo.currentHandle
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getData(
        name: String,
        children: List<TypedNode>,
        currentHandle: Long,
        accountType: AccountType? = null,
    ): ChildrenNodesLoadState {
        return withContext(ioDispatcher) {
            ChildrenNodesLoadState.Success(
                title = name,
                children = children.mapNotNull { favouriteInfo ->
                    val node =
                        fetchNodeWrapper(favouriteInfo.id.longValue) ?: return@mapNotNull null
                    runCatching {
                        FavouriteListItem(
                            favourite = favouriteMapper(
                                node,
                                favouriteInfo,
                                favouriteInfo.isAvailableOffline,
                                stringUtilWrapper,
                                false
                            ) { name ->
                                MimeTypeList.typeForName(name).iconResourceId
                            }
                        )
                    }.onFailure {
                        Timber.e(it)
                    }.getOrNull()
                },
                // If current handle is not current root handle, enable onBackPressedCallback
                isBackPressedEnable = currentHandle != currentRootHandle,
                accountType = accountType,
            )
        }
    }

    private suspend fun filterNonSensitiveItems(
        items: List<TypedNode>,
        showHiddenItems: Boolean?,
        isPaid: Boolean?,
    ) = withContext(defaultDispatcher) {
        showHiddenItems ?: return@withContext items
        isPaid ?: return@withContext items

        return@withContext if (showHiddenItems || !isPaid) {
            items
        } else {
            items.filter {
                !it.isMarkedSensitive && !it.isSensitiveInherited
            }
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
        if (favourite is FavouriteFolder) {
            getChildrenNodes(favourite.typedNode.id.longValue)
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

    internal fun getFileTypeInfo(name: String) = runCatching {
        getFileTypeInfoByNameUseCase(name)
    }.recover {
        Timber.e(it)
        null
    }.getOrNull()

    internal suspend fun getNodeContentUri(fileNode: TypedFileNode) =
        getNodeContentUriUseCase(fileNode)

    companion object {

        /**
         * The key of parent handle argument
         */
        const val KEY_ARGUMENT_PARENT_HANDLE = "parentHandle"
    }
}