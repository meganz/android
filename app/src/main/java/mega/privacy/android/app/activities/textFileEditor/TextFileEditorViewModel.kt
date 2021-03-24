package mega.privacy.android.app.activities.textFileEditor

import android.app.ActivityManager
import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class TextFileEditorViewModel @ViewModelInject constructor(private val megaApi: MegaApiAndroid) :
    BaseRxViewModel() {

    companion object {
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_MODE = "VIEW_MODE"
        const val EDIT_MODE = "EDIT_MODE"
    }

    private var fileName = ""
    private var node: MegaNode? = null
    private var mode = VIEW_MODE
    private var contentText = ""

    fun isViewMode(): Boolean = mode == VIEW_MODE

    fun setEditMode() {
        mode = EDIT_MODE
    }

    fun setModeAndName(handle: Long, name: String?) {
        node = megaApi.getNodeByHandle(handle)
        mode = if (node == null || node?.isFolder == true) CREATE_MODE else VIEW_MODE
        fileName = if (name != null) name + FileUtil.TXT_EXTENSION else node?.name!!
    }

    fun getFileName(): String = fileName

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
            if (mi.totalMem > Constants.BUFFER_COMP) Constants.MAX_BUFFER_32MB
            else Constants.MAX_BUFFER_16MB
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

    fun saveFile() {
        mode = VIEW_MODE
    }
}