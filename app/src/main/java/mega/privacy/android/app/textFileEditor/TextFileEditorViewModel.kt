package mega.privacy.android.app.textFileEditor

import android.app.ActivityManager
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.CacheFolderManager.buildTempFile
import mega.privacy.android.app.utils.ChatUtil.authorizeNodeIfPreview
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaNode
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class TextFileEditorViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) : BaseRxViewModel() {

    companion object {
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_MODE = "VIEW_MODE"
        const val EDIT_MODE = "EDIT_MODE"
    }

    private var fileName = ""
    private var node: MegaNode? = null
    private var mode = VIEW_MODE
    private var contentText = ""
    private var adapterType: Int = INVALID_VALUE
    private var msgChat: MegaChatMessage? = null

    fun isViewMode(): Boolean = mode == VIEW_MODE

    fun getFileName(): String = fileName

    fun getNode(): MegaNode? = node

    fun getNodeAccess(): Int = megaApi.getAccess(node)

    fun getAdapterType(): Int = adapterType

    fun getMsgChat(): MegaChatMessage? = msgChat

    fun setEditMode() {
        mode = EDIT_MODE
    }

    fun setValuesFromIntent(intent: Intent) {
        adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)

        if (adapterType == FROM_CHAT) {
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
        } else {
            val handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
            node = megaApi.getNodeByHandle(handle)
        }

        val name = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME)
        mode = if (node == null || node?.isFolder == true) CREATE_MODE else VIEW_MODE
        fileName = if (name != null) name + FileUtil.TXT_EXTENSION else node?.name!!
    }

    fun readFile(mi: ActivityManager.MemoryInfo): String {
        val localFileUri = getLocalFile(null, node?.name, node?.size!!)

        if (!isTextEmpty(localFileUri)) {
            val localFile = File(localFileUri)

            if (isFileAvailable(localFile)) {
                contentText = readFile(BufferedReader(FileReader(localFile)))
                return contentText
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
            return contentText
        }

        val url = URL(uri)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        val inputS = connection.inputStream
        contentText = readFile(BufferedReader(InputStreamReader(inputS)))
        return contentText
    }

    private fun readFile(br: BufferedReader): String {
        val sb = StringBuilder()

        try {
            var line: String?

            while (br.readLine().also { line = it } != null) {
                sb.append(line)
                sb.append('\n')
            }

            br.close()
            return sb.toString()
        } catch (e: IOException) {
            logError("Exception while reading text file.", e)
        }

        return sb.toString()
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
                } else {
                    node?.handle
                }
            )

        app.startService(uploadIntent)

        return true
    }
}