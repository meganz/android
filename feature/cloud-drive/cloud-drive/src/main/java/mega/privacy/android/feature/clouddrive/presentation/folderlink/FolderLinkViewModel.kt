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
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolderUseCase
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import timber.log.Timber

@HiltViewModel(assistedFactory = FolderLinkViewModel.Factory::class)
internal class FolderLinkViewModel @AssistedInject constructor(
    private val loginToFolderUseCase: LoginToFolderUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    @Assisted private val args: Args,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderLinkUiState())
    val uiState: StateFlow<FolderLinkUiState> = _uiState.asStateFlow()

    init {
        when {
            args.uriString != null -> loginToFolder(args.uriString)
            // args.nodeHandle != null → future MR: fetch sub-folder nodes
            // both null              → future MR: fetch root nodes
        }
    }

    fun processAction(action: FolderLinkAction) {
        when (action) {
            is FolderLinkAction.DecryptionKeyEntered -> onDecryptionKeyEntered(action.key)
            FolderLinkAction.DecryptionKeyDialogDismissed -> onDecryptionKeyDialogDismissed()
        }
    }

    private fun loginToFolder(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(contentState = FolderLinkContentState.Loading) }
            runCatching { loginToFolderUseCase(url) }
                .onSuccess { status ->
                    when (status) {
                        FolderLoginStatus.SUCCESS ->
                            _uiState.update {
                                it.copy(
                                    contentState = FolderLinkContentState.Loaded(
                                        hasDbCredentials = runCatching { hasCredentialsUseCase() }
                                            .getOrDefault(false)
                                    )
                                )
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
        loginToFolder(urlWithKey)
    }

    private fun onDecryptionKeyDialogDismissed() {
        _uiState.update { it.copy(contentState = FolderLinkContentState.Unavailable) }
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
