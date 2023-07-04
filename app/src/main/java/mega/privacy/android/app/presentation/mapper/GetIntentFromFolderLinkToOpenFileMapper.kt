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
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_APP
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_FOLDER_LINK
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PLACEHOLDER
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.GetLocalFileForNode
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerSetMaxBufferSizeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import javax.inject.Inject

/**
 * Mapper to get intent to open file
 *
 * @property getLocalFileForNode [GetLocalFileForNode] to get local file if present
 * @property getLocalFolderLinkFromMegaApiFolderUseCase [GetLocalFolderLinkFromMegaApiFolderUseCase] to get file url if present
 * @property megaApiFolderHttpServerStartUseCase [MegaApiFolderHttpServerStartUseCase] to start MegaApi Server
 * @property megaApiFolderHttpServerIsRunningUseCase [MegaApiFolderHttpServerIsRunningUseCase] to check Mega Api Http server is running
 * @property megaApiFolderHttpServerSetMaxBufferSizeUseCase [MegaApiFolderHttpServerSetMaxBufferSizeUseCase] to get Api buffer size
 * @property getNodeByHandle [GetNodeByHandle]
 */
class GetIntentFromFolderLinkToOpenFileMapper @Inject constructor(
    private val getLocalFileForNode: GetLocalFileForNode,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val megaApiFolderHttpServerSetMaxBufferSizeUseCase: MegaApiFolderHttpServerSetMaxBufferSizeUseCase,
    private val getNodeByHandle: GetNodeByHandle,
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
        viewType: Int,
        childrenNodeIds: List<Long> = listOf(),
    ): Intent? {
        return if (MimeTypeList.typeForName(fileNode.name).isPdf) {
            val mimeType = MimeTypeList.typeForName(fileNode.name).type
            val pdfIntent = Intent(activity, PdfViewerActivity::class.java)

            pdfIntent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK

                putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
                putExtra(INTENT_EXTRA_KEY_IS_FOLDER_LINK, true)
                putExtra(INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
                putExtra(INTENT_EXTRA_KEY_INSIDE, true)
                putExtra(INTENT_EXTRA_KEY_APP, true)
            }

            getLocalFileForNode(fileNode)?.let {
                val path = it.path
                if (path.contains(Environment.getExternalStorageDirectory().path)) {
                    pdfIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            activity,
                            AUTHORITY_STRING_FILE_PROVIDER,
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
                val path = getLocalFolderLinkFromMegaApiFolderUseCase(fileNode.id.longValue)
                    ?: throw UrlDownloadException()
                pdfIntent.setDataAndType(Uri.parse(path), mimeType)
            }
            pdfIntent

        } else if (MimeTypeList.typeForName(fileNode.name).isOpenableTextFile(fileNode.size)) {
            val textFileIntent = Intent(activity, TextEditorActivity::class.java)
            textFileIntent.apply {
                putExtra(INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
                putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
                putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
            }
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

            intentInternalIntentPair.first.putExtra(INTENT_EXTRA_KEY_PLACEHOLDER, 0)
            intentInternalIntentPair.first.apply {
                putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
                putExtra(INTENT_EXTRA_KEY_IS_FOLDER_LINK, true)
                putExtra(INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
                putExtra(INTENT_EXTRA_KEY_FILE_NAME, fileNode.name)
                putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, -1L)
            }
            getLocalFileForNode(fileNode)?.let {
                val path = it.path
                if (path.contains(Environment.getExternalStorageDirectory().path)) {
                    intentInternalIntentPair.first.setDataAndType(
                        FileProvider.getUriForFile(
                            activity,
                            AUTHORITY_STRING_FILE_PROVIDER,
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
                    getLocalFolderLinkFromMegaApiFolderUseCase(fileNode.id.longValue)
                        ?: throw UrlDownloadException()
                intentInternalIntentPair.first.setDataAndType(Uri.parse(path), mimeType)
            }
            if (opusFile) {
                intentInternalIntentPair.first.setDataAndType(
                    intentInternalIntentPair.first.data,
                    "audio/*"
                )
            }
            intentInternalIntentPair.first

        } else if (MimeTypeList.typeForName(fileNode.name).isImage) {
            ImageViewerActivity.getIntentForChildren(
                activity,
                childrenNodeIds.toLongArray(),
                fileNode.id.longValue,
                fromFolderLink = true
            )

        } else {
            getNodeByHandle(fileNode.id.longValue)?.let { node ->
                (activity as FolderLinkComposeActivity).downloadNodes(listOf(node))
            }
            null
        }
    }

    private suspend fun startHttpServer(intent: Intent, context: Context): Intent {
        if (megaApiFolderHttpServerIsRunningUseCase() == 0) {
            megaApiFolderHttpServerStartUseCase()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        val memoryInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getMemoryInfo(memoryInfo)

        megaApiFolderHttpServerSetMaxBufferSizeUseCase(
            if (memoryInfo.totalMem > Constants.BUFFER_COMP) {
                Constants.MAX_BUFFER_32MB
            } else {
                Constants.MAX_BUFFER_16MB
            }
        )
        return intent
    }
}
