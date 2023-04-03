package mega.privacy.android.app.textEditor

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.listeners.ExportListener
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.MoveNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.AlertsAndWarnings.showConfirmRemoveLinkDialog
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.ChatUtil.authorizeNodeIfPreview
import mega.privacy.android.app.utils.Constants.BUFFER_COMP
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.FROM_HOME_PAGE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.MAX_BUFFER_16MB
import mega.privacy.android.app.utils.Constants.MAX_BUFFER_32MB
import mega.privacy.android.app.utils.Constants.MESSAGE_ID
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
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
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.node.ViewerNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.DownloadBackgroundFile
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
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
 * @property checkNameCollisionUseCase  UseCase required to check name collisions.
 * @property moveNodeUseCase            UseCase required to move nodes.
 * @property copyNodeUseCase            UseCase required to copy nodes.
 * @property downloadBackgroundFile     Use case for downloading the file in background if required.
 * @property ioDispatcher
 */
@HiltViewModel
class TextEditorViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val downloadBackgroundFile: DownloadBackgroundFile,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseRxViewModel() {

    companion object {
        const val MODE = "MODE"
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_MODE = "VIEW_MODE"
        const val EDIT_MODE = "EDIT_MODE"
        const val NON_UPDATE_FINISH_ACTION = 0
        const val SUCCESS_FINISH_ACTION = 1
        const val ERROR_FINISH_ACTION = 2
        const val SHOW_LINE_NUMBERS = "SHOW_LINE_NUMBERS"
    }

    private val textEditorData: MutableLiveData<TextEditorData> = MutableLiveData(TextEditorData())
    private val mode: MutableLiveData<String> = MutableLiveData()
    private val fileName: MutableLiveData<String> = MutableLiveData()
    private val pagination: MutableLiveData<Pagination> = MutableLiveData()
    private val snackbarMessage = SingleLiveEvent<String>()
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

    fun onTextFileEditorDataUpdate(): LiveData<TextEditorData> = textEditorData

    fun getFileName(): LiveData<String> = fileName

    fun onContentTextRead(): LiveData<Pagination> = pagination

    fun onSnackbarMessage(): LiveData<String> = snackbarMessage

    fun getCollision(): LiveData<NameCollision> = collision

    fun onExceptionThrown(): LiveData<Throwable> = throwable

    /**
     * Notifies about a fatal error not recoverable.
     */
    fun onFatalError(): LiveData<Unit> = fatalError

    fun getPagination(): Pagination? = pagination.value

    fun getNode(): MegaNode? = textEditorData.value?.node

    /**
     * Checks whether the [MegaNode] is a Backup Node or not
     */
    fun isNodeInInbox(): Boolean = getNode()?.let {
        megaApi.isInInbox(it)
    } ?: false

    fun getNodeAccess(): Int = megaApi.getAccess(getNode())

    fun updateNode() {
        val node = textEditorData.value?.node ?: return

        textEditorData.value?.node = megaApi.getNodeByHandle(node.handle)
        textEditorData.notifyObserver()
    }

    private fun getFileUri(): Uri? = textEditorData.value?.fileUri

    private fun getFileSize(): Long? = textEditorData.value?.fileSize

    fun getAdapterType(): Int = textEditorData.value?.adapterType ?: INVALID_VALUE

    fun isEditableAdapter(): Boolean = textEditorData.value?.editableAdapter ?: false

    fun getMsgChat(): MegaChatMessage? = textEditorData.value?.msgChat

    fun getChatRoom(): MegaChatRoom? = textEditorData.value?.chatRoom

    fun getMode(): LiveData<String> = mode

    fun isViewMode(): Boolean = mode.value == VIEW_MODE

    private fun isEditMode(): Boolean = mode.value == EDIT_MODE

    fun isCreateMode(): Boolean = mode.value == CREATE_MODE

    private fun setViewMode() {
        mode.value = VIEW_MODE
    }

    fun setEditMode() {
        mode.value = EDIT_MODE
    }

    fun getCurrentText(): String? = pagination.value?.getCurrentPageText()

    fun setEditedText(text: String?) {
        pagination.value?.updatePage(text)
    }

    fun getNameOfFile(): String = fileName.value ?: ""

    fun needsReadContent(): Boolean = needsReadContent

    fun isReadingContent(): Boolean = isReadingContent

    fun needsReadOrIsReadingContent(): Boolean = needsReadContent || isReadingContent

    fun errorSettingContent() {
        errorSettingContent = true
    }

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
                && thereIsNoErrorSettingContent() && !isNodeInInbox()

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
     * @param mi          Current phone memory info in case is needed to read the file on streaming.
     * @param preferences Preference data.
     */
    fun setInitialValues(
        intent: Intent,
        mi: ActivityManager.MemoryInfo,
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
                            it.viewerNode = ViewerNode.ChatNode(node.handle, chatId, msgId)
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
                        it.viewerNode = ViewerNode.FileLinkNode(node.handle, serializedNode)
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
                        it.viewerNode = ViewerNode.FolderLinkNode(authorizedNode.handle)
                    }
                }
            }
            else -> {
                val node = megaApi.getNodeByHandle(
                    intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
                )
                textEditorData.value?.let {
                    it.node = node
                    it.viewerNode = ViewerNode.GeneralNode(node.handle)
                }
            }
        }

        textEditorData.value?.api =
            if (adapterType == FOLDER_LINK_ADAPTER) megaApiFolder else megaApi

        mode.value = intent.getStringExtra(MODE) ?: VIEW_MODE

        if (isViewMode() || isEditMode()) {
            needsReadContent = true
            initializeReadParams(mi)
        } else {
            pagination.value = Pagination()
        }

        setEditableAdapter()

        fileName.value = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: getNode()?.name ?: ""

        this.preferences = preferences
        showLineNumbers = preferences.getBoolean(SHOW_LINE_NUMBERS, false)
    }

    /**
     * Initializes the necessary params to read the file content.
     * If file is available locally, the local uri. If not, the streaming URL.
     *
     * @param mi Current phone memory info in case is needed to read the file on streaming.
     */
    private fun initializeReadParams(mi: ActivityManager.MemoryInfo) {
        localFileUri =
            if (getAdapterType() == OFFLINE_ADAPTER || getAdapterType() == ZIP_ADAPTER) getFileUri().toString()
            else getLocalFile(getNode())

        if (isTextEmpty(localFileUri)) {
            val api = textEditorData.value?.api ?: return

            if (api.httpServerIsRunning() == 0) {
                api.httpServerStart()
                textEditorData.value?.needStopHttpServer = true
            }

            api.httpServerSetMaxBufferSize(
                if (mi.totalMem > BUFFER_COMP) MAX_BUFFER_32MB
                else MAX_BUFFER_16MB
            )

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
    private suspend fun downloadFileForReading() {
        downloadBackgroundFileJob = viewModelScope.launch(ioDispatcher) {
            localFileUri = downloadBackgroundFile(textEditorData.value?.viewerNode ?: return@launch)

            if (!readLocalFile()) {
                fatalError.value = Unit
            }
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
     * @param activity Current activity.
     * @param fromHome True if is creating file from Home page, false otherwise.
     */
    fun saveFile(activity: Activity, fromHome: Boolean) {
        if (!isFileEdited() && !isCreateMode()) {
            setViewMode()
            return
        }

        val tempFile = CacheFolderManager.buildTempFile(activity, fileName.value)
        if (tempFile == null) {
            Timber.e("Cannot get temporal file.")
            return
        }

        val fileWriter = FileWriter(tempFile.absolutePath)
        val out = BufferedWriter(fileWriter)
        out.write(pagination.value?.getEditedText() ?: "")
        out.close()

        if (!isFileAvailable(tempFile)) {
            Timber.e("Cannot manage temporal file.")
            return
        }

        val parentHandle = if (mode.value == CREATE_MODE && getNode() == null) {
            megaApi.rootNode?.handle
        } else if (mode.value == CREATE_MODE) {
            getNode()?.handle
        } else {
            getNode()?.parentHandle
        }

        if (parentHandle == null) {
            Timber.e("Parent handle not valid.")
            return
        }

        if (mode.value == EDIT_MODE) {
            uploadFile(activity, fromHome, tempFile, parentHandle)
            return
        }

        checkNameCollisionUseCase.check(tempFile.name, parentHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { handle ->
                    collision.value =
                        NameCollision.Upload.getUploadCollision(
                            handle,
                            tempFile,
                            parentHandle,
                            activity
                        )
                },
                onError = { error ->
                    when (error) {
                        is MegaNodeException.ParentDoesNotExistException -> {
                            Timber.e(error)
                        }
                        is MegaNodeException.ChildDoesNotExistsException -> {
                            uploadFile(activity, fromHome, tempFile, parentHandle)
                        }
                    }
                }
            )
            .addTo(composite)
    }

    /**
     * Uploads the file.
     *
     * @param activity Current activity.
     * @param fromHome True if is creating file from Home page, false otherwise.
     * @param tempFile  The file to upload.
     * @param parentHandle  The handle of the folder in which the file will be uploaded.
     */
    private fun uploadFile(
        activity: Activity,
        fromHome: Boolean,
        tempFile: File,
        parentHandle: Long,
    ) {
        PermissionUtils.checkNotificationsPermission(activity)

        val uploadServiceIntent = Intent(activity, UploadService::class.java)
            .putExtra(UploadService.EXTRA_UPLOAD_TXT, mode.value)
            .putExtra(FROM_HOME_PAGE, fromHome)
            .putExtra(UploadService.EXTRA_FILE_PATH, tempFile.absolutePath)
            .putExtra(UploadService.EXTRA_NAME, fileName.value)
            .putExtra(UploadService.EXTRA_PARENT_HASH, parentHandle)
        ContextCompat.startForegroundService(activity, uploadServiceIntent)

        activity.finish()
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
     * Copies a node if there is no name collision.
     *
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(newParentHandle: Long, context: Context) {
        checkNameCollision(
            newParentHandle = newParentHandle,
            type = NameCollisionType.COPY,
            context = context,
        ) {
            copyNodeUseCase.copy(
                node = getNode(),
                parentHandle = newParentHandle
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        snackbarMessage.value =
                            context.getString(R.string.context_correctly_copied)
                    },
                    onError = { error ->
                        throwable.value = error
                        Timber.e(error, "Not copied: ")
                    }
                )
                .addTo(composite)
        }
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(newParentHandle: Long, context: Context) {
        checkNameCollision(
            newParentHandle = newParentHandle,
            type = NameCollisionType.MOVE,
            context = context
        ) {
            moveNodeUseCase.move(
                handle = getNode()?.handle ?: return@checkNameCollision,
                parentHandle = newParentHandle
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        snackbarMessage.value =
                            context.getString(R.string.context_correctly_moved)
                    },
                    onError = { error ->
                        throwable.value = error
                        Timber.e(error, "Not moved: ")
                    }
                )
                .addTo(composite)
        }
    }

    /**
     * Checks if there is a name collision before proceeding with the action.
     *
     * @param newParentHandle   Handle of the parent folder in which the action will be performed.
     * @param type              [NameCollisionType]
     * @param completeAction    Action to complete after checking the name collision.
     */
    private fun checkNameCollision(
        newParentHandle: Long,
        type: NameCollisionType,
        context: Context,
        completeAction: (() -> Unit),
    ) {
        checkNameCollisionUseCase.check(
            handle = getNode()?.handle ?: return,
            parentHandle = newParentHandle,
            type = type,
            context = context,
        ).observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { collisionResult -> collision.value = collisionResult },
                onError = { error ->
                    when (error) {
                        is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()
                        else -> Timber.e(error)
                    }
                }
            )
            .addTo(composite)
    }

    /**
     * Checks if the content of the file has been modified.
     *
     * @return True if the content has been modified, false otherwise.
     */
    fun isFileEdited(): Boolean = pagination.value?.isEdited() == true

    /**
     * Manages the download action.
     *
     * @param nodeSaver Required object to save nodes.
     */
    fun downloadFile(nodeSaver: NodeSaver) {
        when (getAdapterType()) {
            OFFLINE_ADAPTER -> nodeSaver.saveOfflineNode(getNode()!!.handle, true)
            ZIP_ADAPTER -> nodeSaver.saveUri(
                getFileUri()!!,
                getNameOfFile(),
                getFileSize()!!,
                true
            )
            FROM_CHAT -> nodeSaver.saveNode(
                getNode()!!,
                highPriority = true,
                isFolderLink = true,
                fromMediaViewer = true
            )
            else -> nodeSaver.saveHandle(
                getNode()!!.handle,
                isFolderLink = getAdapterType() == FOLDER_LINK_ADAPTER,
                fromMediaViewer = true
            )
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
                    ExportListener(context) { runDelay(100L) { updateNode() } })
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
            FILE_LINK_ADAPTER -> shareLink(context, urlFileLink)
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
}