package mega.privacy.android.feature.clouddrive.presentation.folderlink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.exception.FetchFolderNodesException
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.folderlink.FetchFolderNodesUseCase
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
    private val nodeUiItemMapper: NodeUiItemMapper,
    @Assisted private val args: Args,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderLinkUiState())
    val uiState: StateFlow<FolderLinkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            checkCredentials()
            when {
                args.uriString != null -> loginToFolder(args.uriString)
                // args.nodeHandle != null → future MR: fetch sub-folder nodes
                // both null              → future MR: fetch root nodes
            }
        }
    }

    fun processAction(action: FolderLinkAction) {
        when (action) {
            is FolderLinkAction.DecryptionKeyEntered -> onDecryptionKeyEntered(action.key)
            FolderLinkAction.DecryptionKeyDialogDismissed -> onDecryptionKeyDialogDismissed()
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
        runCatching {
            val hasCredentials = hasCredentialsUseCase()
            _uiState.update {
                it.copy(hasCredentials = hasCredentials)
            }
        }
        runCatching { loginToFolderUseCase(url) }
            .onSuccess { status ->
                when (status) {
                    FolderLoginStatus.SUCCESS -> {
                        _uiState.update {
                            it.copy(contentState = FolderLinkContentState.FolderLogged)

                        }
                        fetchNodes(null)
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

    /**
     * Fetch the nodes to show
     *
     * @param folderSubHandle   Handle of the folder to fetch the nodes for
     */
    private fun fetchNodes(folderSubHandle: String?) {
        viewModelScope.launch {
            runCatching {
                val result = fetchFolderNodesUseCase(folderSubHandle)
                val title = result.parentNode?.name ?: result.rootNode?.name ?: ""
                val nodeUiItems = nodeUiItemMapper(result.childrenNodes)
                _uiState.update {
                    it.copy(
                        contentState = FolderLinkContentState.Loaded(
                            items = nodeUiItems,
                            rootNode = result.rootNode,
                            parentNode = result.parentNode,
                            title = LocalizedText.Literal(title)
                        )
                    )
                }
            }.onFailure { throwable ->
                if (throwable is FetchFolderNodesException.Expired) {
                    _uiState.update {
                        it.copy(
                            contentState = FolderLinkContentState.Expired
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            contentState = FolderLinkContentState.Unavailable
                        )
                    }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): FolderLinkViewModel
    }

    data class Args(
        val uriString: String?,
        val nodeHandle: Long?,
    )
}
