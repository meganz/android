package mega.privacy.android.app.presentation.clouddrive

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.fileinfo.model.getNodeIcon
import mega.privacy.android.app.presentation.filelink.model.FileLinkState
import mega.privacy.android.app.presentation.mapper.GetIntentFromFileLinkToOpenFileMapper
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.publiclink.CheckPublicNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.publiclink.CopyPublicNodeUseCase
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
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val getIntentFromFileLinkToOpenFileMapper: GetIntentFromFileLinkToOpenFileMapper,
    private val checkPublicNodesNameCollisionUseCase: CheckPublicNodesNameCollisionUseCase,
    private val copyPublicNodeUseCase: CopyPublicNodeUseCase,
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
            val hasCredentials = hasCredentials()
            val shouldLogin = hasCredentials && !rootNodeExistsUseCase()
            _state.update { it.copy(shouldLogin = shouldLogin, hasDbCredentials = hasCredentials) }
        }
    }

    /**
     * Handle intent
     */
    fun handleIntent(intent: Intent) {
        intent.dataString?.let { link ->
            _state.update { it.copy(url = link) }
            getPublicNode(link)
        } ?: Timber.w("url NULL")
    }

    /**
     * Get node from public link
     */
    fun getPublicNode(link: String, decryptionIntroduced: Boolean = false) = viewModelScope.launch {
        runCatching { getPublicNodeUseCase(link) }
            .onSuccess { node ->
                val iconResource = getNodeIcon(node, false)
                _state.update {
                    it.copyWithTypedNode(node, iconResource)
                }
                resetJobInProgressState()
            }
            .onFailure { exception ->
                resetJobInProgressState()
                when (exception) {
                    is PublicNodeException.InvalidDecryptionKey -> {
                        if (decryptionIntroduced) {
                            Timber.w("Incorrect key, ask again!")
                            _state.update { it.copy(askForDecryptionDialog = true) }
                        } else {
                            _state.update {
                                it.copy(fetchPublicNodeError = exception)
                            }
                        }
                    }

                    is PublicNodeException.DecryptionKeyRequired -> {
                        _state.update { it.copy(askForDecryptionDialog = true) }
                    }

                    else -> {
                        _state.update {
                            it.copy(fetchPublicNodeError = exception as? PublicNodeException)
                        }
                    }
                }
            }
    }

    /**
     * Get combined url with key for fetching link content
     */
    fun decrypt(mKey: String?) {
        val url = state.value.url
        mKey?.let { key ->
            if (key.isEmpty()) return
            var urlWithKey = ""
            if (url.contains("#!")) {
                // old folder link format
                urlWithKey = if (key.startsWith("!")) {
                    Timber.d("Decryption key with exclamation!")
                    url + key
                } else {
                    "$url!$key"
                }
            } else if (url.contains(Constants.SEPARATOR + "file" + Constants.SEPARATOR)) {
                // new folder link format
                urlWithKey = if (key.startsWith("#")) {
                    Timber.d("Decryption key with hash!")
                    url + key
                } else {
                    "$url#$key"
                }
            }
            Timber.d("File link to import: $urlWithKey")
            getPublicNode(urlWithKey, true)
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
                        _state.update { it.copy(collisionCheckThrowable = throwable) }
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
                _state.update { it.copy(copyThrowable = copyThrowable) }
            }
    }

    /**
     * Handle select import folder result
     */
    fun handleSelectImportFolderResult(result: ActivityResult) {
        val resultCode = result.resultCode
        val intent = result.data

        if (resultCode != AppCompatActivity.RESULT_OK || intent == null) {
            return
        }

        if (!isConnected) {
            resetJobInProgressState()
            setErrorMessage(R.string.error_server_connection_problem)
            return
        }

        val toHandle = intent.getLongExtra("IMPORT_TO", 0)
        handleImportNode(toHandle)
    }

    /**
     * Handle import node
     *
     * @param targetHandle
     */
    fun handleImportNode(targetHandle: Long) {
        checkNameCollision(targetHandle)
    }

    private fun checkNameCollision(targetHandle: Long) = viewModelScope.launch {
        val fileNode = state.value.fileNode ?: run {
            Timber.e("Invalid File node")
            resetJobInProgressState()
            return@launch
        }
        runCatching {
            checkPublicNodesNameCollisionUseCase(
                listOf(fileNode),
                targetHandle,
                NodeNameCollisionType.COPY
            )
        }.onSuccess { result ->
            if (result.noConflictNodes.isNotEmpty()) {
                copy(targetHandle)
            } else if (result.conflictNodes.isNotEmpty()) {
                val collision = NameCollision.Copy.getCopyCollision(result.conflictNodes[0])
                _state.update {
                    it.copy(collision = collision, jobInProgressState = null)
                }
            }
        }.onFailure { throwable ->
            resetJobInProgressState()
            setErrorMessage(R.string.general_error)
            Timber.e(throwable)
        }
    }

    private fun copy(targetHandle: Long) = viewModelScope.launch {
        val fileNode = state.value.fileNode ?: run {
            Timber.e("Invalid File node")
            resetJobInProgressState()
            return@launch
        }
        runCatching { copyPublicNodeUseCase(fileNode, NodeId(targetHandle), null) }
            .onSuccess { _state.update { it.copy(copySuccess = true, jobInProgressState = null) } }
            .onFailure { copyThrowable ->
                resetJobInProgressState()
                handleCopyError(copyThrowable)
                Timber.e(copyThrowable)
            }
    }

    private fun handleCopyError(throwable: Throwable) {
        when (throwable) {
            is QuotaExceededMegaException -> {
                _state.update { it.copy(overQuotaError = triggered(StorageState.Red)) }
            }

            is NotEnoughQuotaMegaException -> {
                _state.update { it.copy(overQuotaError = triggered(StorageState.Orange)) }
            }

            is ForeignNodeException -> {
                _state.update { it.copy(foreignNodeError = triggered) }
            }

            else -> {
                setErrorMessage(R.string.context_no_copied)
            }
        }
    }

    /**
     * Handle save to device
     */
    fun handleSaveFile() {
        viewModelScope.launch {
            val publicNode = MegaNode.unserialize(state.value.serializedData)
            _state.update { it.copy(downloadFile = triggered(publicNode)) }
        }
    }

    /**
     * Reset copy node values
     */
    fun resetCopyError() {
        _state.update { it.copy(copyThrowable = null) }
    }

    /**
     * Reset collision error
     */
    fun resetCollisionError() {
        _state.update { it.copy(collisionCheckThrowable = null) }
    }

    /**
     * Reset collision
     */
    fun resetCollision() {
        _state.update { it.copy(collision = null) }
    }

    /**
     * Reset the askForDecryptionKeyDialog boolean
     */
    fun resetAskForDecryptionKeyDialog() {
        _state.update { it.copy(askForDecryptionDialog = false) }
    }

    /**
     * Reset the job in progress state value
     */
    private fun resetJobInProgressState() {
        _state.update { it.copy(jobInProgressState = null) }
    }

    /**
     * Handle preview content click
     */
    fun onPreviewClick(activity: Activity) = viewModelScope.launch {
        runCatching {
            with(state.value) {
                getIntentFromFileLinkToOpenFileMapper(
                    activity,
                    handle,
                    title,
                    sizeInBytes,
                    serializedData,
                    url
                )
            }
        }.onSuccess { intent ->
            intent?.let { _state.update { it.copy(openFile = triggered(intent)) } }
        }.onFailure {
            Timber.e("itemClick:ERROR:httpServerGetLocalLink")
        }
    }

    /**
     * Reset and notify that openFile event is consumed
     */
    fun resetOpenFile() = _state.update { it.copy(openFile = consumed()) }

    /**
     * Reset and notify that downloadFile event is consumed
     */
    fun resetDownloadFile() = _state.update { it.copy(downloadFile = consumed()) }

    /**
     * Set and notify that errorMessage event is triggered
     */
    private fun setErrorMessage(message: Int) =
        _state.update { it.copy(errorMessage = triggered(message)) }

    /**
     * Reset and notify that errorMessage event is consumed
     */
    fun resetErrorMessage() = _state.update { it.copy(errorMessage = consumed()) }

    /**
     * Reset and notify that overQuotaError event is consumed
     */
    fun resetOverQuotaError() = _state.update { it.copy(overQuotaError = consumed()) }

    /**
     * Reset and notify that foreignNodeError event is consumed
     */
    fun resetForeignNodeError() = _state.update { it.copy(foreignNodeError = consumed) }
}