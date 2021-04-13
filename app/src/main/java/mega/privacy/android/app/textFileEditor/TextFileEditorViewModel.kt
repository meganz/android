package mega.privacy.android.app.textFileEditor

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.CacheFolderManager.buildTempFile
import mega.privacy.android.app.utils.ChatUtil.authorizeNodeIfPreview
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaNodeUtil.handleSelectFolderToCopyResult
import mega.privacy.android.app.utils.MegaNodeUtil.handleSelectFolderToImportResult
import mega.privacy.android.app.utils.MegaNodeUtil.handleSelectFolderToMoveResult
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
    private val megaChatApi: MegaChatApiAndroid
) : BaseRxViewModel() {

    companion object {
        const val MODE = "MODE"
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_MODE = "VIEW_MODE"
        const val EDIT_MODE = "EDIT_MODE"

        const val CONTENT_TEXT = "CONTENT_TEXT"
    }

    private lateinit var api: MegaApiAndroid

    private var fileName = ""
    private var node: MegaNode? = null
    private var fileUri: Uri? = null
    private var fileSize: Long? = null
    private var mode = VIEW_MODE
    private var adapterType: Int = INVALID_VALUE
    private var msgChat: MegaChatMessage? = null
    private var chatRoom: MegaChatRoom? = null

    private var needStopHttpServer = false

    private val contentText: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    fun onContentTextRead(): LiveData<String> = contentText

    fun getContentText(): String? = contentText.value


    fun setContentText(editedContentText: String) {
        contentText.value = editedContentText
    }

    fun getFileName(): String = fileName

    fun getNode(): MegaNode? = node

    fun updateNode() {
        if (node == null) return

        node = megaApi.getNodeByHandle(node!!.handle)
    }

    fun updateNode(handle: Long) {
        node = megaApi.getNodeByHandle(handle)
    }

    fun getFileUri(): Uri = fileUri!!

    fun getFileSize(): Long = fileSize!!

    fun getNodeAccess(): Int = megaApi.getAccess(node)

    fun getMode(): String = mode

    fun isViewMode(): Boolean = mode == VIEW_MODE

    fun setViewMode() {
        mode = VIEW_MODE
    }

    fun setEditMode() {
        mode = EDIT_MODE
    }

    fun getAdapterType(): Int = adapterType

    fun getMsgChat(): MegaChatMessage? = msgChat

    fun getChatRoom(): MegaChatRoom? = chatRoom

    /**
     * Checks if the file can be editable depending on the current adapter.
     *
     * @return True if the file can be editable, false otherwise.
     */
    fun isEditableAdapter(): Boolean = adapterType != OFFLINE_ADAPTER
            && adapterType != RUBBISH_BIN_ADAPTER && !megaApi.isInRubbish(node)
            && adapterType != FILE_LINK_ADAPTER
            && adapterType != FOLDER_LINK_ADAPTER
            && adapterType != ZIP_ADAPTER
            && adapterType != FROM_CHAT
            && (getNodeAccess() == MegaShare.ACCESS_OWNER || getNodeAccess() == MegaShare.ACCESS_READWRITE)

    /**
     * Gets all necessary values from intent and savedInstanceState if available.
     *
     * @param intent             Received intent.
     * @param savedInstanceState Saved state.
     */
    fun setValuesFromIntent(intent: Intent, savedInstanceState: Bundle?) {
        adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        when (adapterType) {
            FROM_CHAT -> {
                val msgId = intent.getLongExtra(MESSAGE_ID, MEGACHAT_INVALID_HANDLE)
                val chatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE)

                if (msgId != MEGACHAT_INVALID_HANDLE && chatId != MEGACHAT_INVALID_HANDLE) {
                    chatRoom = megaChatApi.getChatRoom(chatId)
                    msgChat = megaChatApi.getMessage(chatId, msgId)

                    if (msgChat == null) {
                        msgChat = megaChatApi.getMessageFromNodeHistory(chatId, msgId)
                    }

                    if (msgChat != null) {
                        node = authorizeNodeIfPreview(
                            msgChat!!.megaNodeList.get(0),
                            megaChatApi,
                            megaApi,
                            chatId
                        )
                    }
                }
            }
            OFFLINE_ADAPTER, ZIP_ADAPTER -> {
                val filePath = intent.getStringExtra(INTENT_EXTRA_KEY_PATH)
                fileUri = filePath!!.toUri()
                fileSize = File(filePath).length()
            }
            FILE_LINK_ADAPTER -> {
                node = MegaNode.unserialize(intent.getStringExtra(EXTRA_SERIALIZE_STRING))
            }
            FOLDER_LINK_ADAPTER -> {
                node = megaApiFolder.getNodeByHandle(
                    intent.getLongExtra(
                        INTENT_EXTRA_KEY_HANDLE,
                        INVALID_HANDLE
                    )
                )

                node = megaApiFolder.authorizeNode(node)
            }
            else -> {
                node = megaApi.getNodeByHandle(
                    intent.getLongExtra(
                        INTENT_EXTRA_KEY_HANDLE,
                        INVALID_HANDLE
                    )
                )
            }
        }

        api = if (adapterType == FOLDER_LINK_ADAPTER) megaApiFolder else megaApi

        if (savedInstanceState != null) {
            mode = savedInstanceState.getString(MODE) ?: VIEW_MODE
            contentText.value = savedInstanceState.getString(CONTENT_TEXT)
        } else {
            mode = intent.getStringExtra(MODE) ?: VIEW_MODE
        }

        fileName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: node?.name!!
    }

    /**
     * Starts the read action to get the content of the file.
     *
     * @param mi Current phone memory info in case is needed to read the file on streaming.
     */
    fun readFileContent(mi: ActivityManager.MemoryInfo) {
        viewModelScope.launch { readFile(mi) }
    }

    /**
     * Continues the read action to get the content of the file.
     * Checks if the file is available to read locally. If not, it's read by streaming.
     *
     * @param mi Current phone memory info in case is needed to read the file on streaming.
     */
    private suspend fun readFile(mi: ActivityManager.MemoryInfo) {
        withContext(Dispatchers.IO) {
            val localFileUri =
                if (adapterType == OFFLINE_ADAPTER || adapterType == ZIP_ADAPTER) fileUri.toString()
                else getLocalFile(null, node?.name, node?.size!!)

            if (!isTextEmpty(localFileUri)) {
                val localFile = File(localFileUri)

                if (isFileAvailable(localFile)) {
                    readFile(BufferedReader(FileReader(localFile)))
                    return@withContext
                }
            }

            if (api.httpServerIsRunning() == 0) {
                api.httpServerStart()
                needStopHttpServer = true
            }

            api.httpServerSetMaxBufferSize(
                if (mi.totalMem > BUFFER_COMP) MAX_BUFFER_32MB
                else MAX_BUFFER_16MB
            )

            val uri = api.httpServerGetLocalLink(node)
            if (uri == null) {
                logError("Error getting the file uri.")
                contentText.value = ""
            }

            val url = URL(uri)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
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

            contentText.postValue(sb.toString())
        }
    }

    /**
     * Starts the save file content action by creating a temp file, setting the new or modified text,
     * and then uploading it to the Cloud.
     *
     * @param contentText The new or modified content text.
     */
    fun saveFile(contentText: String): Boolean {
        val app = MegaApplication.getInstance()
        val tempFile = buildTempFile(app, fileName)
        if (tempFile == null) {
            logError("Cannot get temporal file.")
            return false
        }

        val fileWriter = FileWriter(tempFile.absolutePath)
        val out = BufferedWriter(fileWriter)
        out.write(contentText)
        out.close()

        if (!isFileAvailable(tempFile)) {
            logError("Cannot manage temporal file.")
            return false
        }

        val uploadIntent = Intent(app, UploadService::class.java)
            .putExtra(UploadService.EXTRA_UPLOAD_TXT, true)
            .putExtra(UploadService.EXTRA_FILEPATH, tempFile.absolutePath)
            .putExtra(UploadService.EXTRA_NAME, fileName)
            .putExtra(UploadService.EXTRA_SIZE, tempFile.length())
            .putExtra(
                UploadService.EXTRA_PARENT_HASH,
                if (mode == CREATE_MODE && node == null) {
                    megaApi.rootNode.handle
                } else if (mode == CREATE_MODE && node != null) {
                    node?.handle
                } else {
                    node?.parentHandle
                }
            )

        app.startService(uploadIntent)

        return true
    }

    /**
     * Stops the http server if it has been started before.
     */
    fun checkIfNeedsStopHttpServer() {
        if (needStopHttpServer) {
            api.httpServerStop()
        }
    }

    /**
     * Checks if the completed transfer refers to the same node of current view.
     *
     * @param completedTransfer Completed transfer to check.
     * @return True if the completed transfer refers to the same node, false otherwise.
     */
    fun isSameNode(completedTransfer: AndroidCompletedTransfer): Boolean {
        val fileParentHandle = when {
            node == null -> megaApi.rootNode.handle
            node!!.isFolder -> node!!.handle
            else -> node!!.parentHandle
        }

        return completedTransfer.fileName == fileName
                && completedTransfer.parentHandle == fileParentHandle
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
                    node!!,
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
}