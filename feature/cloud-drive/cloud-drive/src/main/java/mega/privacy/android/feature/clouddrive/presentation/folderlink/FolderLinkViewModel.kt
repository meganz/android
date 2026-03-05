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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderLinkChildrenNodesUseCase
import mega.privacy.android.domain.usecase.folderlink.GetFolderParentNodeUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import timber.log.Timber

@HiltViewModel(assistedFactory = FolderLinkViewModel.Factory::class)
internal class FolderLinkViewModel @AssistedInject constructor(
    private val loginToFolderUseCase: LoginToFolderUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val fetchFolderNodesUseCase: FetchFolderNodesUseCase,
    private val getFolderLinkChildrenNodesUseCase: GetFolderLinkChildrenNodesUseCase,
    private val getFolderParentNodeUseCase: GetFolderParentNodeUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    @Assisted private val args: Args,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderLinkUiState())
    val uiState: StateFlow<FolderLinkUiState> = _uiState.asStateFlow()
    private var backPressJob: Job? = null

    init {
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
            FolderLinkAction.NavigateBackEventConsumed -> _uiState.update {
                it.copy(
                    navigateBackEvent = consumed
                )
            }
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
                val result = fetchFolderNodesUseCase(folderSubHandle)
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
            else -> Unit // TODO file handling: future MR
        }
    }

    private fun openFolder(folder: TypedFolderNode) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentFolderNode = folder,
                    contentState = FolderLinkContentState.Loading
                )
            }

            runCatching {
                getFolderLinkChildrenNodesUseCase(folder.id.longValue, null)
            }.onSuccess { children ->
                _uiState.update {
                    it.copy(
                        contentState = FolderLinkContentState.Loaded(
                            items = nodeUiItemMapper(children),
                        )
                    )
                }
            }.onFailure { error ->
                Timber.e(error)
                _uiState.update { it.copy(contentState = FolderLinkContentState.Unavailable) }
            }
        }
    }

    private fun handleBackPress() {
        backPressJob?.cancel()
        backPressJob = viewModelScope.launch {
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
                    order = null  // TODO Handle sorting
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
