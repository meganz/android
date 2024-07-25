package mega.privacy.android.app.presentation.mapper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.CloudDriveImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.RubbishBinImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.SharedItemsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import mega.privacy.android.app.utils.Constants.FILE_BROWSER_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_APP
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_FOLDER_LINK
import mega.privacy.android.app.utils.Constants.LINKS_ADAPTER
import mega.privacy.android.app.utils.Constants.OUTGOING_SHARES_ADAPTER
import mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFileForNodeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerSetMaxBufferSizeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetFolderLinkNodeContentUriUseCase
import mega.privacy.android.navigation.MegaNavigator
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Mapper to get intent to open file
 *
 * @property getLocalFileForNodeUseCase [GetLocalFileForNodeUseCase] to get local file if present
 * @property getFileUrlByNodeHandleUseCase [GetFileUrlByNodeHandleUseCase] to get file url if present
 * @property httpServerStart [MegaApiHttpServerStartUseCase] to start MegaApi Server
 * @property httpServerIsRunning [MegaApiHttpServerIsRunningUseCase] to check Mega Api Http server is running
 * @property httpServerSetMaxBufferSize [MegaApiHttpServerSetMaxBufferSizeUseCase] to get Api buffer size
 * @property getNodeByHandle [GetNodeByHandle]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
@Deprecated(
    message = "This class is deprecated",
    replaceWith = ReplaceWith(expression = "NodeActionsViewModel -> handleFileNodeClicked()")
)
class GetIntentToOpenFileMapper @Inject constructor(
    private val getLocalFileForNodeUseCase: GetLocalFileForNodeUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val httpServerSetMaxBufferSize: MegaApiHttpServerSetMaxBufferSizeUseCase,
    private val getNodeByHandle: GetNodeByHandle,
    private val addNodeType: AddNodeType,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getFolderLinkNodeContentUriUseCase: GetFolderLinkNodeContentUriUseCase,
    private val megaNavigator: MegaNavigator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
    ): Intent? {
        return if (MimeTypeList.typeForName(fileNode.name).isPdf) {
            val mimeType = MimeTypeList.typeForName(fileNode.name).type
            val pdfIntent = Intent(activity, PdfViewerActivity::class.java)

            pdfIntent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK

                putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
                putExtra(
                    INTENT_EXTRA_KEY_IS_FOLDER_LINK,
                    viewType == FOLDER_LINK_ADAPTER
                )
                putExtra(INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
                putExtra(INTENT_EXTRA_KEY_INSIDE, true)
                putExtra(INTENT_EXTRA_KEY_APP, true)
            }

            getLocalFileForNodeUseCase(fileNode)?.let {
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
                startHttpServer(pdfIntent)
                val path =
                    getFileUrlByNodeHandleUseCase(fileNode.id.longValue)
                        ?: throw UrlDownloadException()
                pdfIntent.setDataAndType(Uri.parse(path), mimeType)
            }
            pdfIntent

        } else if (MimeTypeList.typeForName(fileNode.name).isURL) {
            val intent = Intent(Intent.ACTION_VIEW)
            try {
                val br = getLocalFileForNodeUseCase(fileNode)?.let {
                    val urlFile = File(it.path)
                    FileReader(urlFile).buffered()
                } ?: run {
                    startHttpServer(intent)
                    val path =
                        getFileUrlByNodeHandleUseCase(fileNode.id.longValue)
                    withContext(ioDispatcher) {
                        val nodeURL = URL(path)
                        val connection = nodeURL.openConnection() as HttpURLConnection
                        connection.inputStream.bufferedReader()
                    }
                }
                var line = withContext(ioDispatcher) {
                    br.readLine()
                }
                if (line != null) {
                    line = withContext(ioDispatcher) {
                        br.readLine()
                    }
                    val url = line.replace(Constants.URL_INDICATOR, "")
                    intent.data = Uri.parse(url)
                }
            } catch (ex: Exception) {
                Timber.e(ex)
                throw UrlDownloadException()
            }
            intent

        } else if (MimeTypeList.typeForName(fileNode.name)
                .isOpenableTextFile(fileNode.size)
        ) {
            val textFileIntent = Intent(activity, TextEditorActivity::class.java)
            textFileIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, fileNode.id.longValue)
                .putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
                .putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
            textFileIntent

        } else if (MimeTypeList.typeForName(fileNode.name).isVideoMimeType ||
            MimeTypeList.typeForName(fileNode.name).isAudio
        ) {
            val typedNode = addNodeType(fileNode) as? TypedFileNode
            typedNode?.let { node ->
                runCatching {
                    val contentUri = getFolderLinkNodeContentUriUseCase(node)
                    megaNavigator.openMediaPlayerActivityByFileNode(
                        context = activity,
                        contentUri = contentUri,
                        fileNode = node,
                        viewType = viewType,
                        isFolderLink = viewType == FOLDER_LINK_ADAPTER,
                        sortOrder = getCloudSortOrder()
                    )
                }.onFailure {
                    Timber.e(it)
                }
                null
            }
        } else if (MimeTypeList.typeForName(fileNode.name).isImage) {
            when (viewType) {
                FILE_BROWSER_ADAPTER -> {
                    val parentNodeHandle = fileNode.parentId.longValue
                    ImagePreviewActivity.createIntent(
                        context = activity,
                        imageSource = ImagePreviewFetcherSource.CLOUD_DRIVE,
                        menuOptionsSource = ImagePreviewMenuSource.CLOUD_DRIVE,
                        anchorImageNodeId = fileNode.id,
                        params = mapOf(CloudDriveImageNodeFetcher.PARENT_ID to parentNodeHandle),
                    )
                }

                INCOMING_SHARES_ADAPTER, OUTGOING_SHARES_ADAPTER -> {
                    val parentNodeHandle = fileNode.parentId.longValue
                    ImagePreviewActivity.createIntent(
                        context = activity,
                        imageSource = ImagePreviewFetcherSource.SHARED_ITEMS,
                        menuOptionsSource = ImagePreviewMenuSource.SHARED_ITEMS,
                        anchorImageNodeId = fileNode.id,
                        params = mapOf(SharedItemsImageNodeFetcher.PARENT_ID to parentNodeHandle),
                    )
                }

                LINKS_ADAPTER -> {
                    val parentNodeHandle = fileNode.parentId.longValue
                    ImagePreviewActivity.createIntent(
                        context = activity,
                        imageSource = ImagePreviewFetcherSource.SHARED_ITEMS,
                        menuOptionsSource = ImagePreviewMenuSource.LINKS,
                        anchorImageNodeId = fileNode.id,
                        params = mapOf(SharedItemsImageNodeFetcher.PARENT_ID to parentNodeHandle),
                    )
                }

                RUBBISH_BIN_ADAPTER -> {
                    ImagePreviewActivity.createIntent(
                        context = activity,
                        imageSource = ImagePreviewFetcherSource.RUBBISH_BIN,
                        menuOptionsSource = ImagePreviewMenuSource.RUBBISH_BIN,
                        anchorImageNodeId = fileNode.id,
                        params = mapOf(RubbishBinImageNodeFetcher.PARENT_ID to fileNode.parentId.longValue),
                    )
                }

                else -> {
                    ImagePreviewActivity.createIntent(
                        context = activity,
                        imageSource = ImagePreviewFetcherSource.DEFAULT,
                        menuOptionsSource = ImagePreviewMenuSource.DEFAULT,
                        anchorImageNodeId = fileNode.parentId,
                        params = mapOf(DefaultImageNodeFetcher.NODE_IDS to longArrayOf(fileNode.parentId.longValue)),
                    )
                }
            }
        } else {
            if (viewType == FOLDER_LINK_ADAPTER) {
                (activity as FolderLinkComposeActivity).downloadNodes(listOf(addNodeType(fileNode)))
            } else {
                getNodeByHandle(fileNode.id.longValue)?.let { node ->
                    MegaNodeUtil.onNodeTapped(
                        activity,
                        node,
                        (activity as ManagerActivity)::saveNodeByTap,
                        activity,
                        activity
                    )
                }
            }
            null
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
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        return intent
    }
}
