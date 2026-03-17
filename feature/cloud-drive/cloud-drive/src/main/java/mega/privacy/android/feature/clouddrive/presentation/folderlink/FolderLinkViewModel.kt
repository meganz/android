package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.mobile.analytics.event.ViewModeGridMenuItemEvent
import mega.privacy.mobile.analytics.event.ViewModeListMenuItemEvent
import timber.log.Timber

@HiltViewModel(assistedFactory = FolderLinkViewModel.Factory::class)
internal class FolderLinkViewModel @AssistedInject constructor(
    private val loginToFolderUseCase: LoginToFolderUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val fetchFolderNodesUseCase: FetchFolderNodesUseCase,
    private val getFolderLinkChildrenNodesUseCase: GetFolderLinkChildrenNodesUseCase,
    private val getFolderParentNodeUseCase: GetFolderParentNodeUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val setViewTypeUseCase: SetViewType,
    @Assisted private val args: Args,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderLinkUiState())
    val uiState: StateFlow<FolderLinkUiState> = _uiState.asStateFlow()
    private var browseFolderJob: Job? = null

    init {
        monitorViewType()
        monitorSortOrder()
        viewModelScope.launch {
            checkCredentials()
            if (args.uriString != null) loginToFolder(args.uriString)
        }
    }

    fun processAction(action: FolderLinkAction) {
        when (action) {
            is FolderLinkAction.DecryptionKeyEntered -> onDecryptionKeyEntered(action.key)
            FolderLinkAction.DecryptionKeyDialogDismissed -> onDecryptionKeyDialogDismissed()
            is FolderLinkAction.ItemClicked -> onItemClicked(action)
            FolderLinkAction.BackPressed -> handleBackPress()
            FolderLinkAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
            FolderLinkAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
            is FolderLinkAction.SortOrderChanged -> setSortOrder(action.sortConfiguration)
            FolderLinkAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
        }
    }

    private fun onNavigateBackEventConsumed() {
        _uiState.update {
            it.copy(navigateBackEvent = consumed)
        }
    }

    private fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            runCatching {
                val toggledViewType = when (_uiState.value.currentViewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setViewTypeUseCase(toggledViewType)
                val event = when (toggledViewType) {
                    ViewType.LIST -> ViewModeListMenuItemEvent
                    ViewType.GRID -> ViewModeGridMenuItemEvent
                }
                Analytics.tracker.trackEvent(event)
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }
        }
    }

    private fun monitorViewType() {
        viewModelScope.launch {
            monitorViewTypeUseCase()
                .catch { Timber.e(it) }
                .collect { viewType ->
                    _uiState.update { it.copy(currentViewType = viewType) }
                }
        }
    }

    private fun monitorSortOrder() {
        monitorSortCloudOrderUseCase()
            .catch { Timber.e(it) }
            .filterNotNull()
            .onEach { order ->
                updateSortOrder(order)
                refreshCurrentFolder()
            }
            .launchIn(viewModelScope)
    }

    private fun updateSortOrder(sortOrder: SortOrder) {
        val sortConfiguration = nodeSortConfigurationUiMapper(sortOrder)
        _uiState.update {
            it.copy(
                selectedSortOrder = sortOrder,
                selectedSortConfiguration = sortConfiguration,
            )
        }
    }

    fun setSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set sort order")
            }
        }
    }

    private suspend fun refreshCurrentFolder() {
        val currentFolder = _uiState.value.currentFolderNode ?: return

        runCatching {
            val children = getFolderLinkChildrenNodesUseCase(
                parentHandle = currentFolder.id.longValue,
                order = _uiState.value.selectedSortOrder,
            )
            nodeUiItemMapper(children)
        }.onSuccess { items ->
            _uiState.update {
                it.copy(
                    contentState = FolderLinkContentState.Loaded(
                        items = items,
                    )
                )
            }
        }.onFailure { error ->
            Timber.e(error)
            _uiState.update { it.copy(contentState = FolderLinkContentState.Unavailable) }
        }
    }

    private suspend fun checkCredentials() {
        val hasCredentials = hasCredentialsUseCase()
        _uiState.update {
            it.copy(hasCredentials = hasCredentials)
        }
    }

    private suspend fun loginToFolder(url: String) {
        _uiState.update { it.copy(contentState = FolderLinkContentState.Loading) }
        runCatching { loginToFolderUseCase(url) }
            .onSuccess { status ->
                when (status) {
                    FolderLoginStatus.SUCCESS -> {
                        _uiState.update {
                            it.copy(isFolderLoggedIn = true)
                        }
                        fetchNodes(parseFolderSubHandle(url))
                    }

                    FolderLoginStatus.API_INCOMPLETE ->
                        _uiState.update {
                            it.copy(
                                contentState = FolderLinkContentState.DecryptionKeyRequired(
                                    url = url
                                )
                            )
                        }

                    FolderLoginStatus.INCORRECT_KEY ->
                        _uiState.update {
                            it.copy(
                                contentState = FolderLinkContentState.DecryptionKeyRequired(
                                    url = url,
                                    isKeyIncorrect = true
                                )
                            )
                        }

                    FolderLoginStatus.ERROR ->
                        _uiState.update { it.copy(contentState = FolderLinkContentState.Unavailable) }
                }
            }
            .onFailure { error ->
                Timber.e(error)
                _uiState.update { it.copy(contentState = FolderLinkContentState.Unavailable) }
            }
    }

    private fun onDecryptionKeyEntered(key: String) {
        val currentState = _uiState.value.contentState
        if (currentState !is FolderLinkContentState.DecryptionKeyRequired) return
        val trimmedKey = key.trim()
        val url = currentState.url
        val urlWithKey = when {
            url.contains("#F!") -> {
                // Old folder link format: append key with "!" separator
                if (trimmedKey.startsWith("!")) "$url$trimmedKey" else "$url!$trimmedKey"
            }

            else -> {
                // New folder link format: append key with "#" separator
                if (trimmedKey.startsWith("#")) "$url$trimmedKey" else "$url#$trimmedKey"
            }
        }
        viewModelScope.launch {
            loginToFolder(urlWithKey)
        }
    }

    private fun onDecryptionKeyDialogDismissed() {
        _uiState.update { it.copy(contentState = FolderLinkContentState.Unavailable) }
    }

    private fun fetchNodes(folderSubHandle: String?) {
        viewModelScope.launch {
            runCatching {
                val result =
                    fetchFolderNodesUseCase(folderSubHandle, _uiState.value.selectedSortOrder)
                val nodeUiItems = nodeUiItemMapper(result.childrenNodes)
                _uiState.update {
                    it.copy(
                        contentState = FolderLinkContentState.Loaded(
                            items = nodeUiItems,
                        ),
                        rootNode = result.rootNode,
                        currentFolderNode = result.parentNode,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is FetchFolderNodesException.Expired) {
                    _uiState.update { it.copy(contentState = FolderLinkContentState.Expired) }
                } else {
                    _uiState.update { it.copy(contentState = FolderLinkContentState.Unavailable) }
                }
            }
        }
    }

    private fun onItemClicked(action: FolderLinkAction.ItemClicked) {
        when (val node = action.nodeUiItem.node) {
            is TypedFolderNode -> openFolder(node)
            is TypedFileNode -> onFileClicked(node)
            else -> Unit
        }
    }

    private fun onFileClicked(fileNode: TypedFileNode) {
        _uiState.update { it.copy(openedFileNode = fileNode) }
    }

    private fun onOpenedFileNodeHandled() {
        _uiState.update { it.copy(openedFileNode = null) }
    }

    private fun openFolder(folder: TypedFolderNode) {
        browseFolderJob?.cancel()
        browseFolderJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentFolderNode = folder,
                    contentState = FolderLinkContentState.Loading,
                )
            }
            refreshCurrentFolder()
        }
    }

    private fun handleBackPress() {
        browseFolderJob?.cancel()
        browseFolderJob = viewModelScope.launch {
            val currentFolderNode = _uiState.value.currentFolderNode
            if (currentFolderNode == null) {
                // Pressed back before loading link folder, navigate back
                _uiState.update { it.copy(navigateBackEvent = triggered) }
                return@launch
            }
            _uiState.update { it.copy(contentState = FolderLinkContentState.Loading) }

            val newParentNode = runCatching {
                getFolderParentNodeUseCase(currentFolderNode.id)
            }.getOrElse {
                // In the root folder, navigate back
                _uiState.update { it.copy(navigateBackEvent = triggered) }
                return@launch
            }
            _uiState.update { it.copy(currentFolderNode = newParentNode) }

            runCatching {
                getFolderLinkChildrenNodesUseCase(
                    parentHandle = newParentNode.id.longValue,
                    order = _uiState.value.selectedSortOrder,
                )
            }.onSuccess { children ->
                _uiState.update {
                    it.copy(
                        contentState = FolderLinkContentState.Loaded(
                            items = nodeUiItemMapper(children),
                        )
                    )
                }
            }.onFailure { throwable ->
                Timber.e(throwable)
                _uiState.update { it.copy(contentState = FolderLinkContentState.Unavailable) }
            }
        }
    }

    private fun parseFolderSubHandle(url: String): String? {
        val parts = url.split("!")
        return when {
            parts.size > 3 -> parts[3] // Old format: #F!handle!key!subhandle
            parts.size == 2 -> parts[1] // New format: /folder/handle#key!subhandle
            else -> null
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): FolderLinkViewModel
    }

    data class Args(
        val uriString: String?,
    )
}
