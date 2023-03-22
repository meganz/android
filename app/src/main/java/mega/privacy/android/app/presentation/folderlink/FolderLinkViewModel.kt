package mega.privacy.android.app.presentation.folderlink

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.extensions.errorDialogContentId
import mega.privacy.android.app.presentation.extensions.errorDialogTitleId
import mega.privacy.android.app.presentation.extensions.snackBarMessageId
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.folderlink.LoginToFolder
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * View Model class for [FolderLinkActivity]
 */
@HiltViewModel
class FolderLinkViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorViewType: MonitorViewType,
    private val loginToFolder: LoginToFolder,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    private val hasCredentials: HasCredentials,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val setViewType: SetViewType,
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
        get() = monitorConnectivityUseCase().value

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

    /**
     * Checks the list of nodes to copy in order to know which names already exist
     *
     * @param nodes         List of node handles to copy.
     * @param toHandle      Handle of destination node
     */
    @SuppressLint("CheckResult")
    fun checkNameCollision(nodes: List<MegaNode>, toHandle: Long) {
        checkNameCollisionUseCase.checkNodeList(nodes, toHandle, NameCollisionType.COPY)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { result: Pair<ArrayList<NameCollision>, List<MegaNode>>, throwable: Throwable? ->
                if (throwable == null) {
                    val collisions: ArrayList<NameCollision> = result.first
                    if (collisions.isNotEmpty()) {
                        _state.update {
                            it.copy(collisions = collisions)
                        }
                    }
                    val nodesWithoutCollisions: List<MegaNode> = result.second
                    if (nodesWithoutCollisions.isNotEmpty()) {
                        copyNodeUseCase.copy(nodesWithoutCollisions, toHandle)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { copyRequestResult: CopyRequestResult?, copyThrowable: Throwable? ->
                                _state.update {
                                    it.copy(
                                        copyResultText = copyRequestMessageMapper(copyRequestResult),
                                        copyThrowable = copyThrowable
                                    )
                                }
                            }
                    }
                }
            }
    }

    /**
     * Reset values once collision activity is launched
     */
    fun resetLaunchCollisionActivity() {
        _state.update {
            it.copy(collisions = null)
        }
    }

    /**
     * Reset values once show copy result is processed
     */
    fun resetShowCopyResult() {
        _state.update {
            it.copy(copyResultText = null, copyThrowable = null)
        }
    }

    /**
     * Check if login is required
     */
    fun checkLoginRequired() {
        viewModelScope.launch {
            val hasCredentials = hasCredentials()
            _state.update {
                it.copy(
                    shouldLogin = (hasCredentials && !rootNodeExistsUseCase()),
                    hasDbCredentials = hasCredentials
                )
            }
        }
    }

    /**
     * Update the preferred view type
     *
     * @param isList    Whether the updated view type is list or grid
     */
    fun updateViewType(isList: Boolean) {
        val viewType = if (isList) ViewType.LIST else ViewType.GRID
        viewModelScope.launch {
            setViewType(viewType)
        }
    }
}