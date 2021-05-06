package mega.privacy.android.app.textFileEditor

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.ExportListener
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showConfirmRemoveLinkDialog
import mega.privacy.android.app.utils.CacheFolderManager.buildTempFile
import mega.privacy.android.app.utils.ChatUtil.authorizeNodeIfPreview
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.*
import mega.privacy.android.app.utils.LinksUtil.showGetLinkActivity
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaNodeUtil.handleSelectFolderToCopyResult
import mega.privacy.android.app.utils.MegaNodeUtil.handleSelectFolderToImportResult
import mega.privacy.android.app.utils.MegaNodeUtil.handleSelectFolderToMoveResult
import mega.privacy.android.app.utils.MegaNodeUtil.shareLink
import mega.privacy.android.app.utils.MegaNodeUtil.shareNode
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class TextFileEditorViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val dbH: DatabaseHandler
) : BaseRxViewModel() {

    companion object {
        const val MODE = "MODE"
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_MODE = "VIEW_MODE"
        const val EDIT_MODE = "EDIT_MODE"
        const val NON_UPDATE_FINISH_ACTION = 0
        const val SUCCESS_FINISH_ACTION = 1
        const val ERROR_FINISH_ACTION = 2
    }

    private val textFileEditorData: MutableLiveData<TextFileEditorData> =
        MutableLiveData(TextFileEditorData())
    private val mode: MutableLiveData<String> = MutableLiveData()
    private val savingMode: MutableLiveData<String> = MutableLiveData()
    private val fileName: MutableLiveData<String> = MutableLiveData()
    private val contentText: MutableLiveData<String> = MutableLiveData()
    private val editedText: MutableLiveData<String> by lazy { MutableLiveData<String>() }

    private var needsReadContent = false
    private var localFileUri: String? = null
    private var streamingFileURL: URL? = null

    fun onTextFileEditorDataUpdate(): LiveData<TextFileEditorData> = textFileEditorData

    fun onSavingMode(): LiveData<String> = savingMode

    fun getFileName(): LiveData<String> = fileName

    fun onContentTextRead(): LiveData<String> = contentText

    fun getNode(): MegaNode? = textFileEditorData.value?.node

    fun getNodeAccess(): Int = megaApi.getAccess(getNode())

    fun updateNode() {
        val node = textFileEditorData.value?.node ?: return

        textFileEditorData.value?.node = megaApi.getNodeByHandle(node.handle)
        textFileEditorData.notifyObserver()
    }

    private fun getFileUri(): Uri? = textFileEditorData.value?.fileUri

    private fun getFileSize(): Long? = textFileEditorData.value?.fileSize

    fun getAdapterType(): Int = textFileEditorData.value?.adapterType ?: INVALID_VALUE

    fun isEditableAdapter(): Boolean = textFileEditorData.value?.editableAdapter ?: false

    fun getMsgChat(): MegaChatMessage? = textFileEditorData.value?.msgChat

    fun getChatRoom(): MegaChatRoom? = textFileEditorData.value?.chatRoom

    fun getMode(): LiveData<String> = mode

    fun isViewMode(): Boolean = mode.value == VIEW_MODE

    fun isCreateMode(): Boolean = mode.value == CREATE_MODE

    private fun setViewMode() {
        mode.value = VIEW_MODE
    }

    fun setEditMode() {
        mode.value = EDIT_MODE
    }

    private fun isSaving(): Boolean = savingMode.value != null

    fun isSavingModeEdit(): Boolean = savingMode.value == EDIT_MODE

    fun resetSavingMode() {
        savingMode.value = null
    }

    fun getEditedText(): String? = editedText.value

    fun setEditedText(text: String?) {
        editedText.value = text
    }

    fun getNameOfFile(): String = fileName.value ?: ""

    fun needsReadContent(): Boolean = needsReadContent

    fun canShowEditFab(): Boolean = isViewMode() && isEditableAdapter() && !isSaving()

    /**
     * Checks if the file can be editable depending on the current adapter.
     */
    private fun setEditableAdapter() {
        textFileEditorData.value?.editableAdapter =
            if (isCreateMode()) true
            else getAdapterType() != OFFLINE_ADAPTER
                    && getAdapterType() != RUBBISH_BIN_ADAPTER && !megaApi.isInRubbish(getNode())
                    && getAdapterType() != FILE_LINK_ADAPTER
                    && getAdapterType() != FOLDER_LINK_ADAPTER
                    && getAdapterType() != ZIP_ADAPTER
                    && getAdapterType() != FROM_CHAT
                    && getAdapterType() != INVALID_VALUE
                    && getNodeAccess() >= MegaShare.ACCESS_READWRITE
    }

    /**
     * Gets all necessary values from intent if available.
     *
     * @param intent Received intent.
     * @param mi     Current phone memory info in case is needed to read the file on streaming.
     */
    fun setValuesFromIntent(intent: Intent, mi: ActivityManager.MemoryInfo) {
        val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        textFileEditorData.value?.adapterType = adapterType

        when (adapterType) {
            FROM_CHAT -> {
                val msgId = intent.getLongExtra(MESSAGE_ID, MEGACHAT_INVALID_HANDLE)
                val chatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE)

                if (msgId != MEGACHAT_INVALID_HANDLE && chatId != MEGACHAT_INVALID_HANDLE) {
                    textFileEditorData.value?.chatRoom = megaChatApi.getChatRoom(chatId)
                    var msgChat = megaChatApi.getMessage(chatId, msgId)

                    if (msgChat == null) {
                        msgChat = megaChatApi.getMessageFromNodeHistory(chatId, msgId)
                    }

                    if (msgChat != null) {
                        textFileEditorData.value?.msgChat = msgChat

                        textFileEditorData.value?.node = authorizeNodeIfPreview(
                            msgChat.megaNodeList.get(0),
                            megaChatApi,
                            megaApi,
                            chatId
                        )
                    }
                }
            }
            OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                val filePath = intent.getStringExtra(INTENT_EXTRA_KEY_PATH)

                if (filePath != null) {
                    textFileEditorData.value?.fileUri = filePath.toUri()
                    textFileEditorData.value?.fileSize = File(filePath).length()
                }
            }
            FILE_LINK_ADAPTER -> {
                textFileEditorData.value?.node =
                    MegaNode.unserialize(intent.getStringExtra(EXTRA_SERIALIZE_STRING))
            }
            FOLDER_LINK_ADAPTER -> {
                val node = megaApiFolder.getNodeByHandle(
                    intent.getLongExtra(
                        INTENT_EXTRA_KEY_HANDLE,
                        INVALID_HANDLE
                    )
                )

                textFileEditorData.value?.node = megaApiFolder.authorizeNode(node)
            }
            else -> {
                textFileEditorData.value?.node = megaApi.getNodeByHandle(
                    intent.getLongExtra(
                        INTENT_EXTRA_KEY_HANDLE,
                        INVALID_HANDLE
                    )
                )
            }
        }

        textFileEditorData.value?.api =
            if (adapterType == FOLDER_LINK_ADAPTER) megaApiFolder else megaApi

        mode.value = intent.getStringExtra(MODE) ?: VIEW_MODE

        if (isViewMode()) {
            needsReadContent = true
            initializeReadParams(mi)
        }

        setEditableAdapter()

        fileName.value = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: getNode()?.name!!
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
            else getLocalFile(null, getNode()?.name, getNode()?.size!!)

        if (isTextEmpty(localFileUri)) {
            val api = textFileEditorData.value?.api ?: return

            if (api.httpServerIsRunning() == 0) {
                api.httpServerStart()
                textFileEditorData.value?.needStopHttpServer = true
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
        withContext(Dispatchers.IO) {
            if (!isTextEmpty(localFileUri)) {
                val localFile = File(localFileUri!!)

                if (isFileAvailable(localFile)) {
                    readFile(BufferedReader(FileReader(localFile)))
                    return@withContext
                }
            }

            if (streamingFileURL == null) {
                logError("Error getting the file URL.")
                return@withContext
            }

            val connection: HttpURLConnection =
                streamingFileURL?.openConnection() as HttpURLConnection
            readFile(BufferedReader(InputStreamReader(connection.inputStream)))
        }
    }

    /**
     * Finishes the read action after get all necessary params to do it.
     *
     * @param br Necessary BufferReader to read the file.
     */
    private suspend fun readFile(br: BufferedReader) {
        withContext(Dispatchers.IO) {
            val sb = StringBuilder()

            try {
                var line: String?

                while (br.readLine().also { line = it } != null) {
                    sb.append(line)
                    sb.append('\n')
                }

                br.close()
            } catch (e: IOException) {
                logError("Exception while reading text file.", e)
            }

            checkIfNeedsStopHttpServer()
            needsReadContent = false
            contentText.postValue(sb.toString())
            editedText.postValue(sb.toString())
        }
    }

    /**
     * Starts the save file content action by creating a temp file, setting the new or modified text,
     * and then uploading it to the Cloud.
     *
     * @param context Current context.
     */
    fun saveFile(context: Context) {
        if (!isFileEdited() && !isCreateMode()) {
            setViewMode()
            return
        }

        val tempFile = buildTempFile(context, fileName.value)
        if (tempFile == null) {
            logError("Cannot get temporal file.")

            return
        }

        val fileWriter = FileWriter(tempFile.absolutePath)
        val out = BufferedWriter(fileWriter)
        out.write(editedText.value ?: "")
        out.close()

        if (!isFileAvailable(tempFile)) {
            logError("Cannot manage temporal file.")
            return
        }

        val uploadIntent = Intent(context, UploadService::class.java)
            .putExtra(UploadService.EXTRA_UPLOAD_TXT, true)
            .putExtra(UploadService.EXTRA_FILEPATH, tempFile.absolutePath)
            .putExtra(UploadService.EXTRA_NAME, fileName.value)
            .putExtra(UploadService.EXTRA_SIZE, tempFile.length())
            .putExtra(
                UploadService.EXTRA_PARENT_HASH,
                if (mode.value == CREATE_MODE && getNode() == null) {
                    megaApi.rootNode.handle
                } else if (mode.value == CREATE_MODE) {
                    getNode()?.handle
                } else {
                    getNode()?.parentHandle
                }
            )

        context.startService(uploadIntent)
        savingMode.value = mode.value
        setViewMode()
    }

    /**
     * Stops the http server if has been started before.
     */
    private fun checkIfNeedsStopHttpServer() {
        if (textFileEditorData.value?.needStopHttpServer == true) {
            textFileEditorData.value?.api?.httpServerStop()
        }
    }

    /**
     * Handle activity result.
     *
     * @param requestCode      RequestCode of onActivityResult
     * @param resultCode       ResultCode of onActivityResult
     * @param data             Intent of onActivityResult
     * @param snackbarShower   Interface to show snackbar
     * @param activityLauncher Interface to start activity
     */
    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        snackbarShower: SnackbarShower,
        activityLauncher: ActivityLauncher
    ) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_SELECT_IMPORT_FOLDER -> {
                val toHandle = data.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
                if (toHandle == INVALID_HANDLE) {
                    return
                }

                handleSelectFolderToImportResult(
                    resultCode,
                    toHandle,
                    getNode()!!,
                    snackbarShower,
                    activityLauncher
                )
            }
            REQUEST_CODE_SELECT_FOLDER_TO_MOVE -> handleSelectFolderToMoveResult(
                requestCode,
                resultCode,
                data,
                snackbarShower
            )
            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> handleSelectFolderToCopyResult(
                requestCode,
                resultCode,
                data,
                snackbarShower,
                activityLauncher
            )
        }
    }

    /**
     * Checks if the content of the file has been modified.
     *
     * @return True if the content has been modified, false otherwise.
     */
    fun isFileEdited(): Boolean = contentText.value != editedText.value

    /**
     * Checks if the completed transfer refers to the same node of current view.
     *
     * @param completedTransfer Completed transfer to check.
     * @return True if the completed transfer refers to the same getNode(), false otherwise.
     */
    private fun isSameNode(completedTransfer: AndroidCompletedTransfer): Boolean {
        val fileParentHandle = when {
            getNode() == null -> megaApi.rootNode.handle
            getNode()!!.isFolder -> getNode()!!.handle
            else -> getNode()!!.parentHandle
        }

        return completedTransfer.fileName == fileName.value
                && completedTransfer.parentHandle == fileParentHandle
    }

    /**
     * Finishes the create or edit action if the received completed transfer makes reference to
     * the current opened file.
     *
     * @param completedTransferId The completed transfer identifier.
     */
    fun finishCreationOrEdition(completedTransferId: Long): Int {
        if (completedTransferId == INVALID_ID.toLong()) {
            LogUtil.logWarning("Invalid completedTransferId")
            return NON_UPDATE_FINISH_ACTION
        }

        val completedTransfer = dbH.getcompletedTransfer(completedTransferId)
        if (completedTransfer == null) {
            LogUtil.logWarning("Invalid completedTransfer")
            return NON_UPDATE_FINISH_ACTION
        }

        if (!isSameNode(completedTransfer)) {
            LogUtil.logWarning("Not the same file, no update needed.")
            return NON_UPDATE_FINISH_ACTION
        }

        if (completedTransfer.state != MegaTransfer.STATE_COMPLETED) {
            return ERROR_FINISH_ACTION
        }

        if (editedText.value == null) {
            contentText.value = ""
            editedText.value = ""
        } else {
            contentText.value = editedText.value
        }

        textFileEditorData.value?.node =
            megaApi.getNodeByHandle(completedTransfer.nodeHandle.toLong())

        return SUCCESS_FINISH_ACTION
    }

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
}