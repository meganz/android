package mega.privacy.android.app.presentation.pdfviewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.pdf.GetLastPageViewedInPdfUseCase
import mega.privacy.android.domain.usecase.pdf.SetOrUpdateLastPageViewedInPdfUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * View model for [PdfViewerActivity]
 */
@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
    private val checkChatNodesNameCollisionAndCopyUseCase: CheckChatNodesNameCollisionAndCopyUseCase,
    private val checkNodesNameCollisionWithActionUseCase: CheckNodesNameCollisionWithActionUseCase,
    private val getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val broadcastTransferOverQuotaUseCase: BroadcastTransferOverQuotaUseCase,
    private val getLastPageViewedInPdfUseCase: GetLastPageViewedInPdfUseCase,
    private val setOrUpdateLastPageViewedInPdfUseCase: SetOrUpdateLastPageViewedInPdfUseCase,
) : ViewModel() {

    private val handle: Long
        get() = savedStateHandle["HANDLE"] ?: INVALID_HANDLE

    private val _state = MutableStateFlow(PdfViewerState())

    /**
     * UI State PdfViewer
     * Flow of [PdfViewerState]
     */
    val uiState = _state.asStateFlow()

    init {
        checkLastPageViewed()
        monitorAccountDetail()
        monitorIsHiddenNodesOnboarded()
        checkIsNodeInBackups()
    }

    private fun checkLastPageViewed() {
        if (handle != INVALID_HANDLE) {
            viewModelScope.launch {
                runCatching {
                    getLastPageViewedInPdfUseCase(handle)
                }.onSuccess { lastPageViewed ->
                    _state.update { it.copy(lastPageViewed = lastPageViewed ?: 1) }
                }.onFailure { Timber.e(it) }
            }
        } else {
            _state.update { it.copy(lastPageViewed = 1) }
        }
    }

    /**
     * Sets or updates the last page viewed in the PDF.
     */
    fun setOrUpdateLastPageViewed(lastPageViewed: Long) {
        _state.update { it.copy(lastPageViewed = lastPageViewed) }

        if (handle != INVALID_HANDLE) {
            appScope.launch {
                runCatching {
                    setOrUpdateLastPageViewedInPdfUseCase(
                        LastPageViewedInPdf(
                            nodeHandle = handle,
                            lastPageViewed = lastPageViewed
                        )
                    )
                }.onFailure { Timber.e(it) }
            }
        }
    }

    /**
     * Sets the PDF URI data to the state.
     */
    fun setPdfUriData(pdfUriData: Uri) {
        _state.update { it.copy(pdfUriData = pdfUriData) }
    }

    private fun checkIsNodeInBackups() {
        viewModelScope.launch {
            val isNodeInBackups = isNodeInBackupsUseCase(handle)
            _state.update { it.copy(isNodeInBackups = isNodeInBackups) }
        }
    }

    fun broadcastTransferOverQuota() {
        viewModelScope.launch {
            broadcastTransferOverQuotaUseCase(true)
        }
    }

    /**
     * Imports a chat node if there is no name collision.
     *
     * @param chatId            Chat ID where the node is.
     * @param messageId         Message ID where the node is.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun importChatNode(
        chatId: Long,
        messageId: Long,
        newParentHandle: NodeId,
    ) = viewModelScope.launch {
        runCatching {
            checkChatNodesNameCollisionAndCopyUseCase(
                chatId = chatId,
                messageIds = listOf(messageId),
                newNodeParent = newParentHandle,
            )
        }.onSuccess { result ->
            result.firstChatNodeCollisionOrNull?.let { item ->
                _state.update {
                    it.copy(nameCollision = item)
                }
            }
            result.moveRequestResult?.let { movementResult ->
                _state.update {
                    it.copy(
                        snackBarMessage = if (movementResult.isSuccess)
                            R.string.context_correctly_copied
                        else
                            R.string.context_no_copied
                    )
                }
            }
        }.onFailure { throwable ->
            Timber.e(throwable, "The chat node is not copied")
            _state.update {
                it.copy(nodeCopyError = throwable)
            }
        }
    }

    /**
     * Copies a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(nodeHandle to newParentHandle),
                    type = NodeNameCollisionType.COPY,
                )
            }.onSuccess { result ->
                result.firstNodeCollisionOrNull?.let { item ->
                    _state.update { it.copy(nameCollision = item) }
                }
                result.moveRequestResult?.let { movementResult ->
                    _state.update {
                        it.copy(
                            snackBarMessage = if (movementResult.isSuccess)
                                R.string.context_correctly_copied
                            else
                                R.string.context_no_copied
                        )
                    }
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(nodeCopyError = throwable)
                }
                Timber.e("Error while copying", throwable)
            }
        }
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(nodeHandle to newParentHandle),
                    type = NodeNameCollisionType.MOVE,
                )
            }.onSuccess { result ->
                result.firstNodeCollisionOrNull?.let { item ->
                    _state.update { it.copy(nameCollision = item) }
                }
                result.moveRequestResult?.let { movementResult ->
                    _state.update {
                        it.copy(
                            snackBarMessage = if (movementResult.isSuccess)
                                sharedResR.string.context_correctly_moved
                            else
                                R.string.context_no_moved,
                            shouldFinishActivity = movementResult.isSuccess
                        )
                    }
                }
            }.onFailure { throwable ->
                Timber.e("Error while moving", throwable)
                _state.update {
                    it.copy(nodeMoveError = throwable)
                }
            }
        }
    }

    /**
     * Load pdf stream data from url
     */
    fun loadPdfStream(uri: String) {
        viewModelScope.launch {
            runCatching {
                getDataBytesFromUrlUseCase(URL(uri))
            }.onSuccess { data ->
                _state.update { it.copy(pdfStreamData = data) }
            }.onFailure { Timber.e("Exception loading PDF as stream", it) }
        }
    }

    /**
     * onConsumeSnackBarMessage
     *
     * resets SnackBar state to null once SnackBar is shown
     */
    fun onConsumeSnackBarMessage() {
        _state.update { it.copy(snackBarMessage = null) }
    }

    /**
     * onConsume Copy Error
     *
     * resets throwable to null once error is displayed to user
     */
    fun onConsumeNodeMoveError() {
        _state.update { it.copy(nodeMoveError = null) }
    }

    /**
     * onConsume Copy Error
     *
     * resets throwable to null once error is displayed to user
     */
    fun onConsumeNodeCopyError() {
        _state.update { it.copy(nodeCopyError = null) }
    }

    /**
     * Reset pdf stream data
     */
    fun resetPdfStreamData() {
        _state.update { it.copy(pdfStreamData = null) }
    }

    /**
     * Hide or unhide the node by modifying the sensitive attribute
     */
    fun hideOrUnhideNode(nodeId: NodeId, hide: Boolean) = viewModelScope.launch {
        updateNodeSensitiveUseCase(nodeId = nodeId, isSensitive = hide)
    }

    private fun monitorAccountDetail() {
        monitorAccountDetailUseCase()
            .onEach { accountDetail ->
                val accountType = accountDetail.levelDetail?.accountType
                val businessStatus =
                    if (accountType?.isBusinessAccount == true) {
                        getBusinessStatusUseCase()
                    } else null

                val isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired
                _state.update {
                    it.copy(
                        accountType = accountDetail.levelDetail?.accountType,
                        isBusinessAccountExpired = isBusinessAccountExpired,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun monitorIsHiddenNodesOnboarded() {
        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            _state.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }
    }

    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    /**
     * Save chat node to offline
     *
     * @param chatId    Chat ID where the node is.
     * @param messageId Message ID where the node is.
     */
    fun saveChatNodeToOffline(chatId: Long, messageId: Long) {
        viewModelScope.launch {
            runCatching {
                val chatFile = getChatFileUseCase(chatId = chatId, messageId = messageId)
                    ?: throw IllegalStateException("Chat file not found")
                val isAvailableOffline = isAvailableOfflineUseCase(chatFile)
                if (isAvailableOffline) {
                    _state.update {
                        it.copy(snackBarMessage = R.string.file_already_exists)
                    }
                } else {
                    _state.update {
                        it.copy(startChatOfflineDownloadEvent = triggered(chatFile))
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Reset state event once consumed
     */
    fun onConsumeStartChatOfflineDownloadEvent() {
        _state.update {
            it.copy(startChatOfflineDownloadEvent = consumed())
        }
    }
}