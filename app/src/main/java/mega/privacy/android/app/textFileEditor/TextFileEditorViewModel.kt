package mega.privacy.android.app.textFileEditor

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.CacheFolderManager.buildTempFile
import mega.privacy.android.app.utils.ChatUtil.authorizeNodeIfPreview
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class TextFileEditorViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) : BaseRxViewModel() {

    companion object {
        const val MODE = "MODE"
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_MODE = "VIEW_MODE"
        const val EDIT_MODE = "EDIT_MODE"

        const val CONTENT_TEXT = "CONTENT_TEXT"
    }

    private var fileName = ""
    private var node: MegaNode? = null
    private var filePath: String? = null
    private var mode = VIEW_MODE
    private var adapterType: Int = INVALID_VALUE
    private var msgChat: MegaChatMessage? = null

    private val contentText: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    fun onContentTextRead(): LiveData<String> = contentText

    fun getContentText(): String? = contentText.value

    fun getMode(): String = mode

    fun isViewMode(): Boolean = mode == VIEW_MODE

    fun setViewMode() {
        mode = VIEW_MODE
    }

    fun getFileName(): String = fileName

    fun getNode(): MegaNode? = node

    fun getNodeAccess(): Int = megaApi.getAccess(node)

    fun getAdapterType(): Int = adapterType

    fun getMsgChat(): MegaChatMessage? = msgChat

    fun setEditMode() {
        mode = EDIT_MODE
    }

    fun isEditableAdapter(): Boolean = adapterType != OFFLINE_ADAPTER
            && adapterType != RUBBISH_BIN_ADAPTER && !megaApi.isInRubbish(node)
            && adapterType != FILE_LINK_ADAPTER
            && adapterType != ZIP_ADAPTER
            && adapterType != FROM_CHAT
            && (getNodeAccess() == MegaShare.ACCESS_OWNER || getNodeAccess() == MegaShare.ACCESS_READWRITE)

    fun readFileContent(mi: ActivityManager.MemoryInfo) {
        viewModelScope.launch { readFile(mi) }
    }

    fun setValuesFromIntent(intent: Intent, savedInstanceState: Bundle?) {
        adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        when (adapterType) {
            FROM_CHAT -> {
                val msgId = intent.getLongExtra(MESSAGE_ID, MEGACHAT_INVALID_HANDLE)
                val chatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE)

                if (msgId != MEGACHAT_INVALID_HANDLE && chatId != MEGACHAT_INVALID_HANDLE) {
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
                filePath = intent.getStringExtra(INTENT_EXTRA_KEY_PATH)
            }
            else -> {
                val handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
                node = megaApi.getNodeByHandle(handle)
            }
        }

        if (savedInstanceState != null) {
            mode = savedInstanceState.getString(MODE) ?: VIEW_MODE
            contentText.value = savedInstanceState.getString(CONTENT_TEXT)
        } else {
            mode = intent.getStringExtra(MODE) ?: VIEW_MODE
        }

        fileName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: node?.name!!
    }

    private suspend fun readFile(mi: ActivityManager.MemoryInfo) {
        withContext(Dispatchers.IO) {
            val localFileUri =
                if (adapterType == OFFLINE_ADAPTER || adapterType == ZIP_ADAPTER) filePath
                else getLocalFile(null, node?.name, node?.size!!)

            if (!isTextEmpty(localFileUri)) {
                val localFile = File(localFileUri)

                if (isFileAvailable(localFile)) {
                    readFile(BufferedReader(FileReader(localFile)))
                    return@withContext
                }
            }

            if (megaApi.httpServerIsRunning() == 0) {
                megaApi.httpServerStart()
            }

            megaApi.httpServerSetMaxBufferSize(
                if (mi.totalMem > BUFFER_COMP) MAX_BUFFER_32MB
                else MAX_BUFFER_16MB
            )

            val uri = megaApi.httpServerGetLocalLink(node)
            if (uri == null) {
                logError("Error getting the file uri.")
                contentText.value = ""
            }

            val url = URL(uri)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            readFile(BufferedReader(InputStreamReader(connection.inputStream)))
        }
    }

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
}