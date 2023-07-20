package mega.privacy.android.app.presentation.clouddrive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.filelink.model.FileLinkState
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model class for [mega.privacy.android.app.presentation.filelink.FileLinkActivity]
 */
@HiltViewModel
class FileLinkViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val hasCredentials: HasCredentials,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val legacyCopyNodeUseCase: LegacyCopyNodeUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val getNodeByHandle: GetNodeByHandle,
) : ViewModel() {


    private val _state = MutableStateFlow(FileLinkState())

    /**
     * The FileLink UI State accessible outside the ViewModel
     */
    val state: StateFlow<FileLinkState> = _state.asStateFlow()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

    /**
     * Check if login is required
     */
    fun checkLoginRequired() {
        viewModelScope.launch {
            val shouldLogin = hasCredentials() && !rootNodeExistsUseCase()
            _state.update { it.copy(shouldLogin = shouldLogin) }
        }
    }

    /**
     * Handle import node
     *
     * @param node
     * @param targetHandle
     */
    fun handleImportNode(node: MegaNode?, targetHandle: Long) {
        checkNameCollision(node, targetHandle)
    }

    private fun checkNameCollision(node: MegaNode?, targetHandle: Long) = viewModelScope.launch {
        runCatching { checkNameCollisionUseCase.check(node, targetHandle, NameCollisionType.COPY) }
            .onSuccess { collision -> _state.update { it.copy(collision = collision) } }
            .onFailure { throwable ->
                when (throwable) {
                    is MegaNodeException.ChildDoesNotExistsException -> copy(node, targetHandle)

                    is MegaNodeException.ParentDoesNotExistException -> {
                        _state.update { it.copy(snackBarMessageId = R.string.general_error) }
                    }

                    else -> Timber.e(throwable)
                }
            }
    }

    private fun copy(node: MegaNode?, targetHandle: Long) = viewModelScope.launch {
        val targetNode = getNodeByHandle(targetHandle)
        runCatching { legacyCopyNodeUseCase.copyAsync(node, targetNode) }
            .onSuccess { _state.update { it.copy(copySuccess = true) } }
            .onFailure { copyThrowable ->
                _state.update {
                    it.copy(
                        copyThrowable = copyThrowable,
                        snackBarMessageId = R.string.context_no_copied
                    )
                }
            }
    }

    /**
     * Reset copy node values
     */
    fun resetCopy() {
        _state.update { it.copy(copyThrowable = null, snackBarMessageId = -1) }
    }
}