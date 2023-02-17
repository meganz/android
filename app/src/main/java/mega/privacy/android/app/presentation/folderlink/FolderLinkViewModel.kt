package mega.privacy.android.app.presentation.folderlink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.errorDialogContentId
import mega.privacy.android.app.presentation.extensions.errorDialogTitleId
import mega.privacy.android.app.presentation.extensions.snackBarMessageId
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.folderlink.LoginToFolder
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import javax.inject.Inject

/**
 * View Model class for [FolderLinkActivity]
 */
@HiltViewModel
class FolderLinkViewModel @Inject constructor(
    private val monitorConnectivity: MonitorConnectivity,
    private val monitorViewType: MonitorViewType,
    private val loginToFolder: LoginToFolder,
) : ViewModel() {

    /**
     * The FolderLink UI State
     */
    private val _state = MutableStateFlow(FolderLinkState())

    /**
     * The FolderLink UI State accessible outside the ViewModel
     */
    val state: StateFlow<FolderLinkState> = _state

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Determine whether to show data in list or grid view
     */
    var isList = true

    /**
     * Flow that monitors the View Type
     */
    val onViewTypeChanged: Flow<ViewType>
        get() = monitorViewType()

    /**
     * Performs Login to folder
     *
     * @param folderLink Link of the folder to login
     */
    fun folderLogin(folderLink: String, decryptionIntroduced: Boolean = false) {
        viewModelScope.launch {
            when (val result = loginToFolder(folderLink)) {
                FolderLoginStatus.SUCCESS -> {
                    _state.update { it.copy(isInitialState = false, isLoginComplete = true) }
                }
                FolderLoginStatus.API_INCOMPLETE -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialog = true,
                        )
                    }
                }
                FolderLoginStatus.INCORRECT_KEY -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialog = decryptionIntroduced,
                            errorDialogTitle = result.errorDialogTitleId,
                            errorDialogContent = result.errorDialogContentId,
                            snackBarMessage = result.snackBarMessageId
                        )
                    }
                }
                FolderLoginStatus.ERROR -> {
                    _state.update {
                        it.copy(
                            isInitialState = false,
                            isLoginComplete = false,
                            askForDecryptionKeyDialog = false,
                            errorDialogTitle = result.errorDialogTitleId,
                            errorDialogContent = result.errorDialogContentId,
                            snackBarMessage = result.snackBarMessageId
                        )
                    }
                }
            }
        }
    }

    /**
     * Update whether nodes are fetched or not
     *
     * @param value Whether nodes are fetched
     */
    fun updateIsNodesFetched(value: Boolean) {
        _state.update {
            it.copy(isNodesFetched = value)
        }
    }
}