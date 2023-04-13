package mega.privacy.android.app.presentation.mapper

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandle
import mega.privacy.android.domain.usecase.GetLocalFileForNode
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerSetMaxBufferSizeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Mapper to get intent to open file
 *
 * @property getLocalFileForNode [GetLocalFileForNode] to get local file if present
 * @property getFileUrlByNodeHandle [GetFileUrlByNodeHandle] to get file url if present
 * @property httpServerStart [MegaApiHttpServerStartUseCase] to start MegaApi Server
 * @property httpServerIsRunning [MegaApiHttpServerIsRunningUseCase] to check Mega Api Http server is running
 * @property httpServerSetMaxBufferSize [MegaApiHttpServerSetMaxBufferSizeUseCase] to get Api buffer size
 * @property getNodeByHandle [GetNodeByHandle]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class GetIntentToOpenFileMapper @Inject constructor(
    private val getLocalFileForNode: GetLocalFileForNode,
    private val getFileUrlByNodeHandle: GetFileUrlByNodeHandle,
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val httpServerSetMaxBufferSize: MegaApiHttpServerSetMaxBufferSizeUseCase,
    private val getNodeByHandle: GetNodeByHandle,
    private val getCloudSortOrder: GetCloudSortOrder,
) {

    /**
     * Invoke
     * @param activity Instance of activity
     * @param fileNode [FileNode]
     *
     * @return Nullable Intent
     * @throws [UrlDownloadException] if failed to get Url from server
     */
    @Throws(UrlDownloadException::class)
    suspend operator fun invoke(
        activity: Activity,
        fileNode: FileNode,
    ): Intent? {
        return if (MimeTypeList.typeForName(fileNode.name).isPdf) {
            val mimeType = MimeTypeList.typeForName(fileNode.name).type
            val pdfIntent = Intent(activity, PdfViewerActivity::class.java)
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            pdfIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK

            pdfIntent.putExtra("adapterType", Constants.RUBBISH_BIN_ADAPTER)
            pdfIntent.putExtra("inside", true)
            pdfIntent.putExtra("APP", true)

            getLocalFileForNode(fileNode)?.let {
                val path = it.path
                if (path.contains(Environment.getExternalStorageDirectory().path)) {
                    pdfIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            activity,
                            "mega.privacy.android.app.providers.fileprovider",
                            it
                        ),
                        MimeTypeList.typeForName(fileNode.name).type
                    )
                } else {
                    pdfIntent.setDataAndType(
                        Uri.fromFile(it),
                        MimeTypeList.typeForName(fileNode.name).type
                    )
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } ?: run {
                startHttpServer(pdfIntent, activity)
                val path =
                    getFileUrlByNodeHandle(fileNode.id.longValue) ?: throw UrlDownloadException()
                pdfIntent.setDataAndType(Uri.parse(path), mimeType)
            }
            pdfIntent.putExtra("HANDLE", fileNode.id.longValue)
            pdfIntent
        } else if (MimeTypeList.typeForName(fileNode.name).isURL) {
            val intent = Intent(Intent.ACTION_VIEW)
            val br = getLocalFileForNode(fileNode)?.let {
                val urlFile = File(it.path)
                BufferedReader(FileReader(urlFile))
            } ?: run {
                startHttpServer(intent, activity)
                val path =
                    getFileUrlByNodeHandle(fileNode.id.longValue) ?: throw UrlDownloadException()
                val connection = URL(path).openConnection() as HttpURLConnection
                BufferedReader(InputStreamReader(connection.inputStream))
            }
            var line = br.readLine()
            if (line != null) {
                line = br.readLine()
                val url = line.replace(Constants.URL_INDICATOR, "")
                intent.data = Uri.parse(url)
            }
            intent
        } else if (MimeTypeList.typeForName(fileNode.name)
                .isOpenableTextFile(fileNode.size)
        ) {
            val textFileIntent = Intent(activity, TextEditorActivity::class.java)
            textFileIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
                .putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
            textFileIntent
        } else if (MimeTypeList.typeForName(fileNode.name).isVideoMimeType ||
            MimeTypeList.typeForName(fileNode.name).isAudio
        ) {
            val mimeType = MimeTypeList.typeForName(fileNode.name).type
            var opusFile = false
            val intentInternalIntentPair =
                if (MimeTypeList.typeForName(fileNode.name).isVideoNotSupported ||
                    MimeTypeList.typeForName(fileNode.name).isAudioNotSupported
                ) {
                    val s = fileNode.name.split("\\.".toRegex())
                    if (s.size > 1 && s[s.size - 1] == "opus") {
                        opusFile = true
                    }
                    Pair(Intent(Intent.ACTION_VIEW), false)
                } else {
                    Pair(Util.getMediaIntent(activity, fileNode.name), true)
                }

            intentInternalIntentPair.first.putExtra(
                "placeholder",
                0
            )
            intentInternalIntentPair.first.apply {
                putExtra("FILENAME", fileNode.name)
                putExtra("adapterType", Constants.RUBBISH_BIN_ADAPTER)
                putExtra("parentNodeHandle", -1L)
            }
            getLocalFileForNode(fileNode)?.let {
                val path = it.path
                if (path.contains(Environment.getExternalStorageDirectory().path)) {
                    intentInternalIntentPair.first.setDataAndType(
                        FileProvider.getUriForFile(
                            activity,
                            "mega.privacy.android.app.providers.fileprovider",
                            it
                        ),
                        MimeTypeList.typeForName(fileNode.name).type
                    )
                } else {
                    intentInternalIntentPair.first.setDataAndType(
                        Uri.fromFile(it),
                        MimeTypeList.typeForName(fileNode.name).type
                    )
                }
                intentInternalIntentPair.first.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } ?: run {
                startHttpServer(intentInternalIntentPair.first, activity)
                val path =
                    getFileUrlByNodeHandle(fileNode.id.longValue) ?: throw UrlDownloadException()
                intentInternalIntentPair.first.setDataAndType(Uri.parse(path), mimeType)
            }
            intentInternalIntentPair.first.putExtra("HANDLE", fileNode.id.longValue)
            if (opusFile) {
                intentInternalIntentPair.first.setDataAndType(
                    intentInternalIntentPair.first.data,
                    "audio/*"
                )
            }
            intentInternalIntentPair.first
        } else if (MimeTypeList.typeForName(fileNode.name).isImage) {
            ImageViewerActivity.getIntentForParentNode(
                activity,
                fileNode.parentId.longValue,
                getCloudSortOrder(),
                fileNode.id.longValue
            )
        } else {
            getNodeByHandle(fileNode.id.longValue)?.let {
                MegaNodeUtil.onNodeTapped(
                    activity,
                    it,
                    (activity as ManagerActivity)::saveNodeByTap,
                    activity,
                    activity
                )
            }
            null
        }
    }

    /**
     * Start the server if not started
     * also setMax buffer size based on available buffer size
     * @param intent [Intent]
     * @param context [Context]
     *
     * @return intent
     */
    private suspend fun startHttpServer(intent: Intent, context: Context): Intent {
        if (httpServerIsRunning() == 0) {
            httpServerStart()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        val memoryInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getMemoryInfo(memoryInfo)

        httpServerSetMaxBufferSize(
            if (memoryInfo.totalMem > Constants.BUFFER_COMP) {
                Constants.MAX_BUFFER_32MB
            } else {
                Constants.MAX_BUFFER_16MB
            }
        )
        return intent
    }
}
