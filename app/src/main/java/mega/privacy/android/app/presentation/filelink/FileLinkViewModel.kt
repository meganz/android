package mega.privacy.android.app.presentation.filelink

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import mega.privacy.android.app.presentation.fileinfo.model.getNodeIcon
import mega.privacy.android.app.presentation.filelink.model.FileLinkJobInProgressState
import mega.privacy.android.app.presentation.filelink.model.FileLinkState
import mega.privacy.android.app.presentation.folderlink.model.LinkErrorState
import mega.privacy.android.app.presentation.mapper.UrlDownloadException
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.PublicNodeException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.advertisements.QueryAdsUseCase
import mega.privacy.android.domain.usecase.filelink.GetFileUrlByPublicLinkUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFileLinkNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.publiclink.CheckPublicNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.publiclink.CopyPublicNodeUseCase
import mega.privacy.android.domain.usecase.node.publiclink.MapNodeToPublicLinkUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.MegaNavigator
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View Model class for [mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity]
 *
 * @param monitorMiscLoadedUseCase Use case to monitor when misc data is loaded
 */
@HiltViewModel
class FileLinkViewModel @Inject constructor(
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
    private val checkPublicNodesNameCollisionUseCase: CheckPublicNodesNameCollisionUseCase,
    private val copyPublicNodeUseCase: CopyPublicNodeUseCase,
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val getFileUrlByPublicLinkUseCase: GetFileUrlByPublicLinkUseCase,
    private val mapNodeToPublicLinkUseCase: MapNodeToPublicLinkUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val getFileLinkNodeContentUriUseCase: GetFileLinkNodeContentUriUseCase,
    private val megaNavigator: MegaNavigator,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    val monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    private val queryAdsUseCase: QueryAdsUseCase,
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
        get() = isConnectedToInternetUseCase()

    /**
     * Check if login is required
     */
    fun checkLoginRequired() {
        viewModelScope.launch {
            val hasCredentials = hasCredentialsUseCase()
            val shouldLogin = hasCredentials && !rootNodeExistsUseCase()
            _state.update {
                it.copy(
                    showLoginScreenEvent = if (shouldLogin) triggered else consumed,
                    hasDbCredentials = hasCredentials
                )
            }
        }
    }

    /**
     * Consume show login screen event
     */
    fun onShowLoginScreenEventConsumed() {
        _state.update {
            it.copy(showLoginScreenEvent = consumed)
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
                val iconResource = getNodeIcon(
                    typedNode = node,
                    originShares = false,
                    fileTypeIconMapper = fileTypeIconMapper
                )
                _state.update {
                    it.copyWithTypedNode(node, iconResource)
                }
                queryAds(node.id.longValue)
                resetJobInProgressState()
            }
            .onFailure { exception ->
                resetJobInProgressState()
                when (exception) {
                    is PublicNodeException.InvalidDecryptionKey -> {
                        if (decryptionIntroduced) {
                            Timber.w("Incorrect key, ask again!")
                            _state.update { it.copy(askForDecryptionKeyDialogEvent = triggered) }
                        } else {
                            _state.update {
                                it.copy(errorState = LinkErrorState.Unavailable)
                            }
                        }
                    }

                    is PublicNodeException.DecryptionKeyRequired -> {
                        _state.update { it.copy(askForDecryptionKeyDialogEvent = triggered) }
                    }

                    else -> {
                        _state.update {
                            it.copy(
                                errorState = if (exception is PublicNodeException.Expired) {
                                    LinkErrorState.Expired
                                } else if (exception is PublicNodeException) {
                                    LinkErrorState.Unavailable
                                } else {
                                    LinkErrorState.NoError
                                }
                            )
                        }
                    }
                }
            }
    }

    private fun queryAds(handle: Long) {
        viewModelScope.launch {
            runCatching {
                queryAdsUseCase(handle)
            }.onSuccess { value ->
                _state.update { it.copy(shouldShowAdsForLink = value) }
            }.onFailure {
                Timber.e(it)
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
                _state.update {
                    it.copy(
                        collisionsEvent = triggered(result.conflictNodes.first()),
                        jobInProgressState = null
                    )
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
        _state.update {
            it.copy(
                jobInProgressState = FileLinkJobInProgressState.Importing,
            )
        }
        runCatching { copyPublicNodeUseCase(fileNode, NodeId(targetHandle), null) }
            .onSuccess {
                _state.update {
                    it.copy(
                        copySuccessEvent = triggered,
                        jobInProgressState = null
                    )
                }
            }
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
            val linkNodes = listOfNotNull(
                (_state.value.fileNode as? UnTypedNode)?.let {
                    runCatching {
                        mapNodeToPublicLinkUseCase(it, null)
                    }.onFailure {
                        Timber.e(it)
                    }.getOrNull()
                })
            _state.update {
                it.copy(
                    downloadEvent = triggered(
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = linkNodes,
                            withStartMessage = false
                        )
                    )
                )
            }
        }
    }

    /**
     * Reset collision
     */
    fun resetCollision() {
        _state.update { it.copy(collisionsEvent = consumed()) }
    }

    /**
     * Reset the askForDecryptionKeyDialog boolean
     */
    fun resetAskForDecryptionKeyDialog() {
        _state.update { it.copy(askForDecryptionKeyDialogEvent = consumed) }
    }

    /**
     * Reset the copySuccessEvent when consumed
     */
    fun resetCopySuccessEvent() {
        _state.update { it.copy(copySuccessEvent = consumed) }
    }

    /**
     * Reset the job in progress state value
     */
    private fun resetJobInProgressState() {
        _state.update { it.copy(jobInProgressState = null) }
    }

    /**
     * update intent values for image
     */
    fun updateImageIntent(intent: Intent) {
        _state.update { it.copy(openFile = triggered(intent)) }
    }

    /**
     * Update intent values for pdf
     */
    fun updatePdfIntent(pdfIntent: Intent, mimeType: String) {
        viewModelScope.launch {
            runCatching {
                with(state.value) {
                    pdfIntent.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK

                        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, handle)
                        putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, title)
                        putExtra(Constants.URL_FILE_LINK, url)
                        putExtra(Constants.EXTRA_SERIALIZE_STRING, serializedData)
                        putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
                        putExtra(
                            Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                            Constants.FILE_LINK_ADAPTER
                        )
                    }
                    startHttpServer(pdfIntent)
                    val path = getFileUrlByPublicLinkUseCase(url) ?: throw UrlDownloadException()
                    pdfIntent.setDataAndType(Uri.parse(path), mimeType)
                }
            }.onSuccess { intent ->
                intent.let { _state.update { it.copy(openFile = triggered(intent)) } }
            }.onFailure {
                Timber.e("itemClick:ERROR:httpServerGetLocalLink")
            }
        }
    }

    /**
     * Update intent value for text editor
     */
    fun updateTextEditorIntent(intent: Intent) {
        with(state.value) {
            intent.apply {
                putExtra(Constants.URL_FILE_LINK, url)
                putExtra(Constants.EXTRA_SERIALIZE_STRING, serializedData)
                putExtra(
                    Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                    Constants.FILE_LINK_ADAPTER
                )
            }
            _state.update { it.copy(openFile = triggered(intent)) }
        }
    }

    /**
     * Start the server if not started
     * also setMax buffer size based on available buffer size
     * @param intent [Intent]
     *
     * @return intent
     */
    private suspend fun startHttpServer(intent: Intent): Intent {
        if (httpServerIsRunning() == 0) {
            httpServerStart()
            intent.putExtra(ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        return intent
    }

    /**
     * Reset and notify that openFile event is consumed
     */
    fun resetOpenFile() = _state.update { it.copy(openFile = consumed()) }

    /**
     * Reset and notify that downloadFile event is consumed
     */
    fun resetDownloadFile() = _state.update {
        it.copy(
            downloadEvent = consumed(),
        )
    }

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

    internal suspend fun getNodeContentUri() = getFileLinkNodeContentUriUseCase(_state.value.url)


    internal fun openOtherTypeFile(
        context: Context,
        fileNode: TypedNode,
        showSnackBar: (Int) -> Unit,
    ) {
        viewModelScope.launch {
            val typedFileNode = fileNode as? TypedFileNode ?: return@launch
            getNodePreviewFileUseCase(fileNode)?.let { localFile ->
                if (fileNode.type is ZipFileTypeInfo) {
                    openZipFile(
                        context = context,
                        localFile = localFile,
                        fileNode = typedFileNode,
                        showSnackBar = showSnackBar
                    )
                } else {
                    handleOtherFiles(
                        context = context,
                        localFile = localFile,
                        currentFileNode = fileNode,
                        showSnackBar = showSnackBar
                    )
                }
            } ?: updateNodeToPreview(fileNode)
        }
    }

    private fun openZipFile(
        context: Context,
        localFile: File,
        fileNode: TypedFileNode,
        showSnackBar: (Int) -> Unit,
    ) {
        Timber.d("The file is zip, open in-app.")
        megaNavigator.openZipBrowserActivity(
            context = context,
            zipFilePath = localFile.absolutePath,
            nodeHandle = fileNode.id.longValue,
        ) {
            showSnackBar(R.string.message_zip_format_error)
        }
    }

    private fun handleOtherFiles(
        context: Context,
        localFile: File,
        currentFileNode: TypedFileNode,
        showSnackBar: (Int) -> Unit,
    ) {
        Intent(Intent.ACTION_VIEW).apply {
            nodeContentUriIntentMapper(
                intent = this,
                content = NodeContentUri.LocalContentUri(localFile),
                mimeType = currentFileNode.type.mimeType,
                isSupported = false
            )
            runCatching {
                context.startActivity(this)
            }.onFailure { error ->
                Timber.e(error)
                showSnackBar(R.string.intent_not_available)
            }
        }
    }

    /**
     * Update nodes to download
     */
    private suspend fun updateNodeToPreview(node: TypedNode) =
        runCatching {
            mapNodeToPublicLinkUseCase(node as UnTypedNode, null)
        }.onSuccess { linkNode ->
            _state.update {
                it.copy(
                    downloadEvent = triggered(
                        TransferTriggerEvent.StartDownloadForPreview(
                            node = linkNode,
                            isOpenWith = false
                        )
                    )
                )
            }
        }.onFailure {
            Timber.e(it)
        }
}
