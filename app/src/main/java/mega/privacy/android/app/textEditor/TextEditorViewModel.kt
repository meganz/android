package mega.privacy.android.app.textEditor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.webkit.WebView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.ExportListener
import mega.privacy.android.app.utils.AlertsAndWarnings.showConfirmRemoveLinkDialog
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.ChatUtil.authorizeNodeIfPreview
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.MESSAGE_ID
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.FileUtil.shareUri
import mega.privacy.android.app.utils.LinksUtil.showGetLinkActivity
import mega.privacy.android.app.utils.MegaNodeUtil.shareLink
import mega.privacy.android.app.utils.MegaNodeUtil.shareNode
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.RUBBISH_BIN_ADAPTER
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.extensions.getFileName
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Main ViewModel to handle all logic related to the [TextEditorActivity].
 *
 * @property megaApi                    Needed to manage nodes.
 * @property megaApiFolder              Needed to manage folder link nodes.
 * @property megaChatApi                Needed to get text file info from chats.
 * @property ioDispatcher
 * @property getNodeByIdUseCase
 * @property getChatFileUseCase
 * @property getPublicChildNodeFromIdUseCase
 * @property getPublicNodeFromSerializedDataUseCase
 */
@HiltViewModel
class TextEditorViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase,
    private val checkNodesNameCollisionWithActionUseCase: CheckNodesNameCollisionWithActionUseCase,
    private val checkChatNodesNameCollisionAndCopyUseCase: CheckChatNodesNameCollisionAndCopyUseCase,
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val downloadNodeUseCase: DownloadNodeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getPublicChildNodeFromIdUseCase: GetPublicChildNodeFromIdUseCase,
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val crashReporter: CrashReporter,
    // Use existing use case for smart filename generation
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase,
) : ViewModel() {

    companion object {
        const val MODE = "MODE"
        const val SHOW_LINE_NUMBERS = "SHOW_LINE_NUMBERS"

        private val BLOCKQUOTE_STYLE = """
            blockquote {
                margin: 16px 0;
                padding: 10px 20px;
                background-color: #f9f9f9;
                border-left: 5px solid #ccc;
                font-style: italic;
                color: #555;
            }
        """.trimIndent()

        internal const val CONVERTED_FILE_NAME = "markdown_converted_content.html"
        internal const val CHUCK_SIZE = 100 * 1024 //100KB
    }

    private val handle: Long
        get() = savedStateHandle[INTENT_EXTRA_KEY_HANDLE] ?: INVALID_HANDLE

    private val textEditorData: MutableLiveData<TextEditorData> = MutableLiveData(TextEditorData())
    private val mode: MutableLiveData<String> = MutableLiveData()
    private val fileName: MutableLiveData<String> = MutableLiveData()
    private val pagination: MutableLiveData<Pagination> = MutableLiveData()
    private val snackBarMessage = SingleLiveEvent<Int>()
    private val fatalError = SingleLiveEvent<Unit>()
    private val collision = SingleLiveEvent<NameCollision>()
    private val throwable = SingleLiveEvent<Throwable>()

    private var needsReadContent = false
    private var isReadingContent = false
    private var errorSettingContent = false
    private var localFileUri: String? = null
    private var streamingFileURL: URL? = null
    private var showLineNumbers = false

    private lateinit var preferences: SharedPreferences

    private var downloadBackgroundFileJob: Job? = null

    private val _uiState = MutableStateFlow(TextEditorViewState())

    val uiState = _uiState.asStateFlow()

    init {
        monitorAccountDetail()
        monitorIsHiddenNodesOnboarded()
        monitorNodeUpdates()
        checkIsNodeInBackups()
    }

    private fun checkIsNodeInBackups() {
        viewModelScope.launch {
            val isNodeInBackups = isNodeInBackupsUseCase(handle)
            _uiState.update { it.copy(isNodeInBackups = isNodeInBackups) }
        }
    }

    private fun monitorNodeUpdates() {
        monitorNodeUpdatesUseCase()
            .onEach { updateNode() }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    fun onTextFileEditorDataUpdate(): LiveData<TextEditorData> = textEditorData

    fun getFileName(): LiveData<String> = fileName

    fun onContentTextRead(): LiveData<Pagination> = pagination

    fun onSnackBarMessage(): LiveData<Int> = snackBarMessage

    fun getCollision(): LiveData<NameCollision> = collision

    fun onExceptionThrown(): LiveData<Throwable> = throwable

    /**
     * Notifies about a fatal error not recoverable.
     */
    fun onFatalError(): LiveData<Unit> = fatalError

    fun getPagination(): Pagination? = pagination.value

    fun getNode(): MegaNode? = textEditorData.value?.node

    /**
     * Checks whether the [MegaNode] exists in Backups or not
     *
     * @return true if the [MegaNode] exists in Backups, and false if otherwise
     */
    private fun isNodeInBackups(): Boolean = getNode()?.let {
        megaApi.isInVault(it)
    } == true

    fun getNodeAccess(): Int = megaApi.getAccess(getNode())

    fun updateNode() {
        if (textEditorData.value?.adapterType in listOf(
                FOLDER_LINK_ADAPTER,
                FILE_LINK_ADAPTER,
                OFFLINE_ADAPTER,
                ZIP_ADAPTER,
                FROM_CHAT
            )
        ) return
        val node = textEditorData.value?.node ?: return

        textEditorData.value?.node = megaApi.getNodeByHandle(node.handle)
        textEditorData.notifyObserver()
    }

    private fun getFileUri(): Uri? = textEditorData.value?.fileUri

    fun getAdapterType(): Int = textEditorData.value?.adapterType ?: INVALID_VALUE

    fun isEditableAdapter(): Boolean = textEditorData.value?.editableAdapter == true

    fun getMsgChat(): MegaChatMessage? = textEditorData.value?.msgChat

    fun getChatRoom(): MegaChatRoom? = textEditorData.value?.chatRoom

    fun getMode(): LiveData<String> = mode

    fun isViewMode(): Boolean = mode.value == TextEditorMode.View.value

    internal fun isEditMode(): Boolean = mode.value == TextEditorMode.Edit.value

    fun isCreateMode(): Boolean = mode.value == TextEditorMode.Create.value

    internal fun setViewMode() {
        mode.value = TextEditorMode.View.value
    }

    fun setEditMode() {
        mode.value = TextEditorMode.Edit.value
    }

    fun getCurrentText(): String? = pagination.value?.getCurrentPageText()

    fun setEditedText(text: String?) {
        pagination.value?.updatePage(text)
    }

    fun getNameOfFile(): String = fileName.value ?: ""

    fun needsReadContent(): Boolean = needsReadContent

    fun isReadingContent(): Boolean = isReadingContent

    fun needsReadOrIsReadingContent(): Boolean = needsReadContent || isReadingContent

    fun thereIsErrorSettingContent(): Boolean = errorSettingContent

    fun thereIsNoErrorSettingContent(): Boolean = !errorSettingContent

    fun setShowLineNumbers(): Boolean {
        showLineNumbers = !showLineNumbers
        preferences.edit().putBoolean(SHOW_LINE_NUMBERS, showLineNumbers).apply()

        return shouldShowLineNumbers()
    }

    fun shouldShowLineNumbers(): Boolean = showLineNumbers

    fun canShowEditFab(): Boolean =
        isViewMode() && isEditableAdapter() && !needsReadOrIsReadingContent()
                && thereIsNoErrorSettingContent() && !isNodeInBackups()

    /**
     * Checks if the file can be editable depending on the current adapter.
     */
    private fun setEditableAdapter() {
        textEditorData.value?.editableAdapter =
            if (isCreateMode()) true
            else getAdapterType() != OFFLINE_ADAPTER
                    && getAdapterType() != RUBBISH_BIN_ADAPTER && !megaApi.isInRubbish(getNode())
                    && getAdapterType() != FILE_LINK_ADAPTER
                    && getAdapterType() != FOLDER_LINK_ADAPTER
                    && getAdapterType() != ZIP_ADAPTER
                    && getAdapterType() != FROM_CHAT
                    && getAdapterType() != VERSIONS_ADAPTER
                    && getAdapterType() != INVALID_VALUE
                    && getNodeAccess() >= MegaShare.ACCESS_READWRITE
    }

    /**
     * Sets all necessary values from params if available.
     *
     * @param intent      Received intent.
     * @param preferences Preference data.
     */
    fun setInitialValues(
        intent: Intent,
        preferences: SharedPreferences,
    ) {
        val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        textEditorData.value?.adapterType = adapterType

        when (adapterType) {
            FROM_CHAT -> {
                val msgId = intent.getLongExtra(MESSAGE_ID, MEGACHAT_INVALID_HANDLE)
                val chatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE)

                if (msgId != MEGACHAT_INVALID_HANDLE && chatId != MEGACHAT_INVALID_HANDLE) {
                    textEditorData.value?.chatRoom = megaChatApi.getChatRoom(chatId)
                    var msgChat = megaChatApi.getMessage(chatId, msgId)

                    if (msgChat == null) {
                        msgChat = megaChatApi.getMessageFromNodeHistory(chatId, msgId)
                    }

                    if (msgChat != null) {
                        textEditorData.value?.msgChat = msgChat

                        val node = authorizeNodeIfPreview(
                            msgChat.megaNodeList.get(0),
                            megaChatApi,
                            megaApi,
                            chatId
                        )
                        textEditorData.value?.let {
                            it.node = node
                        }
                    }
                }
            }

            OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                val filePath = intent.getStringExtra(INTENT_EXTRA_KEY_PATH)

                if (filePath != null) {
                    textEditorData.value?.fileUri = filePath.toUri()
                    textEditorData.value?.fileSize = File(filePath).length()
                }
            }

            FILE_LINK_ADAPTER -> {
                intent.getStringExtra(EXTRA_SERIALIZE_STRING)?.let { serializedNode ->
                    val node = MegaNode.unserialize(serializedNode)
                    textEditorData.value?.let {
                        it.node = node
                    }
                }
            }

            FOLDER_LINK_ADAPTER -> {
                megaApiFolder.getNodeByHandle(
                    intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
                )?.let { node ->
                    val authorizedNode = megaApiFolder.authorizeNode(node)
                    textEditorData.value?.let {
                        it.node = authorizedNode
                    }
                }
            }

            else -> {
                val node = megaApi.getNodeByHandle(
                    intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
                )
                textEditorData.value?.let {
                    it.node = node
                }
            }
        }

        textEditorData.value?.api =
            if (adapterType == FOLDER_LINK_ADAPTER) megaApiFolder else megaApi

        mode.value = intent.getStringExtra(MODE) ?: TextEditorMode.View.value

        if (isViewMode() || isEditMode()) {
            needsReadContent = true
            initializeReadParams()
        } else {
            pagination.value = Pagination()
        }

        setEditableAdapter()

        fileName.value =
            intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: getNode()?.name ?: ""
        val isMarkDownFile = fileName.value?.endsWith(".md", ignoreCase = true) == true
        _uiState.update { it.copy(isMarkDownFile = isMarkDownFile) }

        this.preferences = preferences
        showLineNumbers = preferences.getBoolean(SHOW_LINE_NUMBERS, false)
    }

    /**
     * Initializes the necessary params to read the file content.
     * If file is available locally, the local uri. If not, the streaming URL.
     *
     */
    private fun initializeReadParams() {
        localFileUri =
            if (getAdapterType() == OFFLINE_ADAPTER || getAdapterType() == ZIP_ADAPTER) getFileUri().toString()
            else getLocalFile(getNode())

        if (isTextEmpty(localFileUri)) {
            val api = textEditorData.value?.api ?: return

            if (api.httpServerIsRunning() == 0) {
                api.httpServerStart()
                textEditorData.value?.needStopHttpServer = true
            }

            val uri = api.httpServerGetLocalLink(getNode())

            if (!isTextEmpty(uri)) {
                streamingFileURL = URL(uri)
            }
        }
    }

    /**
     * Starts the read action to get the content of the file.
     */
    fun readFileContent() {
        viewModelScope.launch { readFile() }
    }

    /**
     * Continues the read action to get the content of the file.
     * Checks if the file is available to read locally. If not, it's read by streaming.
     */
    private suspend fun readFile() {
        withContext(ioDispatcher) {
            isReadingContent = true
            needsReadContent = false

            if (readLocalFile()) {
                return@withContext
            }

            if (streamingFileURL == null) {
                Timber.e("Error getting the file URL.")
                downloadFileForReading()
                return@withContext
            }

            val deferred = viewModelScope.async { createConnectionAndRead() }

            try {
                deferred.await()
            } catch (e: Exception) {
                Timber.e(e, "Creating connection for reading by streaming.")
                downloadFileForReading()
            }
        }
    }

    /**
     * Reads the file from local storage.
     *
     * @return True if the read from local file started successfully, false otherwise.
     */
    private suspend fun readLocalFile(): Boolean {
        val localFile: File = localFileUri?.let { File(it) } ?: return false

        return if (localFile.exists()) {
            kotlin.runCatching {
                readFile(BufferedReader(FileReader(localFile)))
            }.onFailure {
                Timber.e(it, "Exception while reading text file.")
            }

            true
        } else {
            false
        }
    }

    /**
     * Downloads the file in background in case some error happened trying to read it with streaming,
     * and tries to read it from local if the download finishes with success.
     */
    private fun downloadFileForReading() {
        downloadBackgroundFileJob = viewModelScope.launch(ioDispatcher) {
            runCatching {
                val nodeFileName = textEditorData.value?.node?.getFileName() ?: return@launch
                val typedNode = getTypedNode() ?: return@launch
                getCacheFileUseCase(
                    CacheFolderConstant.TEMPORARY_FOLDER,
                    nodeFileName,
                )?.absolutePath?.let { destination ->
                    localFileUri = destination
                    downloadNodeUseCase(
                        node = typedNode,
                        destinationPath = destination,
                        appData = listOf(TransferAppData.BackgroundTransfer),
                        isHighPriority = true,
                    )
                        .catch {
                            Timber.e(it)
                            showFatalError()
                        }
                        .collect()
                } ?: return@launch

                if (!readLocalFile()) {
                    showFatalError()
                }
            }.onFailure {
                Timber.e(it)
                showFatalError()
            }
        }
    }

    private suspend fun showFatalError() {
        withContext(Dispatchers.Main) {
            fatalError.value = Unit
        }
    }

    /**
     * Cancels the download in background.
     */
    private fun cancelDownload() {
        downloadBackgroundFileJob?.cancel()
    }

    /**
     * Creates a connection for reading the file by streaming.
     */
    private suspend fun createConnectionAndRead() {
        withContext(ioDispatcher) {
            kotlin.runCatching {
                val connection: HttpURLConnection =
                    streamingFileURL?.openConnection() as HttpURLConnection

                readFile(BufferedReader(InputStreamReader(connection.inputStream)))
            }.onFailure {
                Timber.e(it, "Exception while reading text file through streaming.")
            }
        }
    }

    /**
     * Finishes the read action after get all necessary params to do it.
     *
     * @param br Necessary BufferReader to read the file.
     */
    private suspend fun readFile(br: BufferedReader) {
        withContext(ioDispatcher) {
            val sb = StringBuilder()

            kotlin.runCatching {
                var line: String?

                while (br.readLine().also { line = it } != null) {
                    sb.appendLine(line)
                }

                br.close()
            }.onFailure {
                Timber.e(it, "Exception while reading text file.")
            }

            checkIfNeedsStopHttpServer()
            isReadingContent = false

            //Remove latest line break since it's not part of the file content
            val latestBreak = sb.lastIndexOf("\n")
            if (sb.isNotEmpty() && latestBreak != -1 && sb.length - latestBreak == 1) {
                sb.deleteRange(latestBreak, sb.length)
            }


            pagination.postValue(Pagination(sb.toString()))
            sb.clear()
        }
    }

    /**
     * Starts the save file content action by creating a temp file, setting the new or modified text,
     * and then uploading it to the Cloud.
     *
     * @param fromHome True if is creating file from Home page, false otherwise.
     */
    fun saveFile(fromHome: Boolean) {
        if (!isFileEdited() && !isCreateMode()) {
            setViewMode()
            return
        }

        viewModelScope.launch {
            try {
                // Generate unique filename using existing collision handling
                val originalFileName = fileName.value ?: "untitled.txt"
                val parentHandle = getParentHandle() ?: return@launch

                // Create a FileNameCollision object to use with the existing use case
                val nameCollision = FileNameCollision(
                    collisionHandle = 0L, // Not relevant for our use case
                    name = originalFileName,
                    size = 0L, // Not relevant for our use case
                    lastModified = System.currentTimeMillis(),
                    parentHandle = parentHandle,
                    path = UriPath("") // Not relevant for our use case
                )

                val uniqueFileName = getNodeNameCollisionRenameNameUseCase(nameCollision)

                // Create temp file with unique name
                val tempFile = CacheFolderManager.buildTempFile(uniqueFileName)
                if (tempFile == null) {
                    Timber.e("Cannot get temporal file.")
                    return@launch
                }

                // Write content to temp file
                writeContentToTempFile(tempFile)

                if (!isFileAvailable(tempFile)) {
                    Timber.e("Cannot manage temporal file.")
                    return@launch
                }

                // Handle upload based on mode
                if (mode.value == TextEditorMode.Edit.value) {
                    uploadFile(fromHome, tempFile, parentHandle)
                } else {
                    // For create mode, check collisions before upload
                    checkCollisionsAndUpload(fromHome, tempFile, parentHandle)
                }

            } catch (e: Exception) {
                Timber.e(e, "Error in saveFile")
                throwable.value = e
            }
        }
    }

    /**
     * Uploads the file.
     *
     * @param fromHome True if is creating file from Home page, false otherwise.
     * @param tempFile  The file to upload.
     * @param parentHandle  The handle of the folder in which the file will be uploaded.
     */
    private fun uploadFile(
        fromHome: Boolean,
        tempFile: File,
        parentHandle: Long,
    ) {
        _uiState.update { state ->
            state.copy(
                transferEvent = triggered(
                    TransferTriggerEvent.StartUpload.TextFile(
                        path = tempFile.absolutePath,
                        destinationId = NodeId(parentHandle),
                        isEditMode = isEditMode(),
                        fromHomePage = fromHome
                    )
                )
            )
        }
    }

    /**
     * Gets the parent handle for the file operation
     */
    private fun getParentHandle(): Long? {
        return if (mode.value == TextEditorMode.Create.value && getNode() == null) {
            megaApi.rootNode?.handle
        } else if (mode.value == TextEditorMode.Create.value) {
            getNode()?.handle
        } else {
            getNode()?.parentHandle
        }
    }

    /**
     * Writes content to a temporary file
     */
    private suspend fun writeContentToTempFile(tempFile: File) {
        withContext(ioDispatcher) {
            val fileWriter = FileWriter(tempFile.absolutePath)
            val out = BufferedWriter(fileWriter)
            out.write(pagination.value?.getEditedText() ?: "")
            out.close()
        }
    }

    /**
     * Checks for collisions and uploads the file if no conflicts exist
     */
    private fun checkCollisionsAndUpload(fromHome: Boolean, tempFile: File, parentHandle: Long) {
        viewModelScope.launch {
            runCatching {
                checkFileNameCollisionsUseCase(
                    files = listOf(
                        DocumentEntity(
                            name = tempFile.name,
                            size = tempFile.length(),
                            lastModified = tempFile.lastModified(),
                            uri = UriPath(tempFile.toUri().toString()),
                        )
                    ),
                    parentNodeId = NodeId(parentHandle)
                )
            }.onSuccess { fileCollisions ->
                fileCollisions.firstOrNull()?.let {
                    collision.value = it
                } ?: uploadFile(fromHome, tempFile, parentHandle)
            }.onFailure {
                Timber.e(it, "Cannot check name collisions")
            }
        }
    }

    /**
     * Finishes all pending works before closing the activity.
     */
    fun finishBeforeClosing() {
        checkIfNeedsStopHttpServer()
        cancelDownload()
    }

    /**
     * Stops the http server if has been started before.
     */
    private fun checkIfNeedsStopHttpServer() {
        if (textEditorData.value?.needStopHttpServer == true) {
            textEditorData.value?.api?.httpServerStop()
            textEditorData.value?.needStopHttpServer = false
        }
    }

    /**
     * Imports a node if there is no name collision.
     *
     * @param newParentHandle
     */
    fun importNode(newParentHandle: Long) {
        viewModelScope.launch {
            runCatching {
                val chatFile = getTypedNode()
                if (chatFile !is ChatFile) throw IllegalStateException("TypedNode must be a ChatFile type")
                importChatNode(
                    chatId = chatFile.chatId,
                    messageId = chatFile.messageId,
                    newParentNode = NodeId(newParentHandle)
                )
            }.onFailure {
                throwable.value = it
                Timber.e(it)
            }
        }
    }

    /**
     * Imports a chat node if there is no name collision.
     * @param chatId            Chat id where the node is.
     * @param messageId         Message id of the node to import.
     * @param newParentNode     Parent node in which the node will be copied.
     */
    fun importChatNode(chatId: Long, messageId: Long, newParentNode: NodeId) {
        viewModelScope.launch {
            runCatching {
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            }.onSuccess { result ->
                result.firstChatNodeCollisionOrNull?.let { item ->
                    collision.value = item
                }

                result.moveRequestResult?.let {
                    snackBarMessage.value = if (it.isSuccess) {
                        R.string.context_correctly_copied
                    } else {
                        R.string.context_no_copied
                    }
                }
            }.onFailure {
                throwable.value = it
                Timber.e(it)
            }
        }
    }

    /**
     * Copies a node if there is no name collision.
     *
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
                    collision.value = item
                }
                result.moveRequestResult?.let {
                    snackBarMessage.value = if (it.isSuccess) {
                        R.string.context_correctly_copied
                    } else {
                        R.string.context_no_copied
                    }
                }
            }.onFailure {
                Timber.e(it)
                if (it is NodeDoesNotExistsException) {
                    snackBarMessage.value = R.string.general_error
                } else {
                    throwable.value = it
                }
            }
        }
    }

    /**
     * Moves a node if there is no name collision.
     *
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
                    collision.value = item
                }
                result.moveRequestResult?.let {
                    snackBarMessage.value = if (it.isSuccess) {
                        R.string.context_correctly_moved
                    } else {
                        R.string.context_no_moved
                    }
                }
            }.onFailure {
                Timber.e(it)
                if (it is NodeDoesNotExistsException) {
                    snackBarMessage.value = R.string.general_error
                } else {
                    throwable.value = it
                }
            }
        }
    }

    fun hideOrUnhideNode(nodeId: NodeId, hide: Boolean) {
        viewModelScope.launch {
            updateNodeSensitiveUseCase(nodeId = nodeId, isSensitive = hide)
        }
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
                _uiState.update {
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
            _uiState.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }
    }

    fun setHiddenNodesOnboarded() {
        _uiState.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    /**
     * Checks if the content of the file has been modified.
     *
     * @return True if the content has been modified, false otherwise.
     */
    fun isFileEdited(): Boolean = pagination.value?.isEdited() == true

    /**
     * Manages the download action.
     */
    fun downloadFile() {
        when (getAdapterType()) {
            ZIP_ADAPTER -> _uiState.update { state ->
                state.copy(
                    transferEvent = triggered(
                        TransferTriggerEvent.CopyUri(
                            name = getNameOfFile(),
                            uriPath = UriPath(getFileUri().toString())
                        )
                    )
                )
            }

            else -> {
                viewModelScope.launch {
                    val typedNode = getTypedNode() ?: return@launch
                    updateTransferEvent(
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = listOfNotNull(typedNode),
                            withStartMessage = true,
                        )
                    )
                }
            }
        }
    }

    private suspend fun getTypedNode(): TypedNode? = when (getAdapterType()) {
        OFFLINE_ADAPTER, ZIP_ADAPTER -> null
        FROM_CHAT -> {
            val chatId =
                textEditorData.value?.chatRoom?.chatId ?: INVALID_HANDLE
            val msgId = textEditorData.value?.msgChat?.msgId ?: INVALID_HANDLE
            getChatFileUseCase(chatId, msgId)
        }

        FOLDER_LINK_ADAPTER -> {
            val nodeId = NodeId(getNode()?.handle ?: INVALID_HANDLE)
            getPublicChildNodeFromIdUseCase(nodeId)
        }

        FILE_LINK_ADAPTER -> {
            getNode()?.serialize()?.let {
                getPublicNodeFromSerializedDataUseCase(it)
            }
        }

        else -> {
            getNode()?.handle?.let {
                getNodeByIdUseCase(NodeId(it))
            }
        }
    }

    private fun updateTransferEvent(event: TransferTriggerEvent) {
        _uiState.update {
            it.copy(transferEvent = triggered(event))
        }
    }

    /**
     * Consume transfer event
     */
    fun consumeTransferEvent() {
        _uiState.update {
            it.copy(transferEvent = consumed())
        }
    }

    /**
     * Manages the get or remove link action depending on if the node is already exported or not.
     *
     * @param context Current context.
     */
    fun manageLink(context: Context) {
        if (showTakenDownNodeActionNotAvailableDialog(getNode(), context)) {
            return
        }

        if (getNode()?.isExported == true) {
            showConfirmRemoveLinkDialog(context) {
                megaApi.disableExport(
                    getNode(),
                    ExportListener(context) { runDelay(500L) { updateNode() } })
            }
        } else {
            showGetLinkActivity(context as Activity, getNode()!!.handle)
        }
    }

    /**
     * Manages the share action.
     *
     * @param context     Current context.
     * @param urlFileLink Link if FILE_LINK_ADAPTER, empty otherwise.
     */
    fun share(context: Context, urlFileLink: String) {
        when (getAdapterType()) {
            OFFLINE_ADAPTER, ZIP_ADAPTER -> shareUri(
                context,
                getNameOfFile(),
                getFileUri()
            )

            FILE_LINK_ADAPTER -> shareLink(context, urlFileLink, getNode()?.name)
            else -> shareNode(context, getNode()!!) { updateNode() }
        }
    }

    fun previousClicked() {
        pagination.value?.previousPage()
        pagination.notifyObserver()
    }

    fun nextClicked() {
        pagination.value?.nextPage()
        pagination.notifyObserver()
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
                    snackBarMessage.value = R.string.file_already_exists
                } else {
                    _uiState.update {
                        it.copy(
                            transferEvent = triggered(
                                TransferTriggerEvent.StartDownloadForOffline(
                                    node = chatFile,
                                    withStartMessage = true,
                                )
                            )
                        )
                    }
                }
            }.onFailure {
                Timber.e(it)
                throwable.value = it
            }
        }
    }

    internal fun convertMarkDownToHtml(context: Context, webView: WebView) {
        viewModelScope.launch(ioDispatcher) {
            if (_uiState.value.markDownFileLoaded) {
                _uiState.update { it.copy(markDownFileLoaded = false) }
            }
            getPagination()?.getEditedText()?.let { content ->
                crashReporter.log("The MD file size is ${content.length}")
                val tempHtmlFile = File(context.cacheDir, CONVERTED_FILE_NAME)

                writeMarkdownAsHtmlToFile(content, tempHtmlFile)

                withContext(Dispatchers.Main) {
                    val uri: Uri =
                        FileProvider.getUriForFile(
                            context,
                            AUTHORITY_STRING_FILE_PROVIDER,
                            tempHtmlFile
                        )

                    webView.settings.allowFileAccess = true
                    webView.settings.allowContentAccess = true
                    webView.loadUrl(uri.toString())
                    _uiState.update { it.copy(markDownFileLoaded = true) }
                }
            }
        }
    }

    private suspend fun writeMarkdownAsHtmlToFile(content: String, file: File) {
        withContext(Dispatchers.IO) {
            val chunkSize = CHUCK_SIZE
            val safeChunks = splitContentSafely(content, chunkSize)

            file.bufferedWriter().use { writer ->
                writer.write("<html><head><style>$BLOCKQUOTE_STYLE</style></head><body>")

                val parser = Parser.builder().build()
                val renderer = HtmlRenderer.builder().build()

                safeChunks.forEach { chunk ->
                    val document = parser.parse(chunk)
                    val htmlChunk = renderer.render(document)
                    writer.write(htmlChunk)
                }

                writer.write("</body></html>")
            }
        }
    }

    private fun splitContentSafely(content: String, chunkSize: Int): List<String> {
        val result = mutableListOf<String>()
        var currentChunk = StringBuilder()

        content.split("\n").forEach { line ->
            if (currentChunk.length + line.length > chunkSize) {
                result.add(currentChunk.toString())
                currentChunk = StringBuilder(line)
            } else {
                currentChunk.append(line).append("\n")
            }
        }

        if (currentChunk.isNotEmpty()) {
            result.add(currentChunk.toString())
        }

        return result
    }
}