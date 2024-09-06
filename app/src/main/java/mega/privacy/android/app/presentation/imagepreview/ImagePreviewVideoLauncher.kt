package mega.privacy.android.app.presentation.imagepreview

import android.content.Context
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.VIEWER_FROM_ZIP_BROWSER
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.usecase.GetFileUrlByImageNodeUseCase
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import mega.privacy.android.domain.usecase.node.GetFolderLinkNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * AlbumImport Preview help class
 */
class ImagePreviewVideoLauncher @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val getFileUrlByImageNodeUseCase: GetFileUrlByImageNodeUseCase,
    private val addImageTypeUseCase: AddImageTypeUseCase,
    private val getFolderLinkNodeContentUriUseCase: GetFolderLinkNodeContentUriUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val getFileTypeInfoUseCase: GetFileTypeInfoUseCase,
    private val megaNavigator: MegaNavigator,
) {

    suspend fun launchVideoScreen(
        context: Context,
        imageNode: ImageNode,
        source: ImagePreviewFetcherSource = ImagePreviewFetcherSource.DEFAULT,
        adapterType: Int = Constants.FROM_IMAGE_VIEWER,
    ) {
        runCatching {
            val viewType = if (source == ImagePreviewFetcherSource.ZIP) { //handle zip file
                VIEWER_FROM_ZIP_BROWSER
            } else {
                adapterType
            }
            isLocalFile(imageNode, source)?.let { localPath ->
                val file = File(localPath)
                val fileTypeInfo = getFileTypeInfoUseCase(file)
                megaNavigator.openMediaPlayerActivityByLocalFile(
                    context = context,
                    localFile = file,
                    fileTypeInfo = fileTypeInfo,
                    viewType = viewType,
                    handle = imageNode.id.longValue,
                    parentId = imageNode.parentId.longValue,
                )
            } ?: run {
                val typedFileNode = addImageTypeUseCase(imageNode)
                if (source == ImagePreviewFetcherSource.CHAT) {
                    getNodeContentUriUseCase(imageNode as ChatImageFile)
                } else {
                    getFolderLinkNodeContentUriUseCase(typedFileNode)
                }.let { contentUri ->
                    megaNavigator.openMediaPlayerActivityByFileNode(
                        context = context,
                        contentUri = contentUri,
                        fileNode = typedFileNode,
                        viewType = viewType
                    )
                }
            }
        }.onFailure { Timber.e(it) }
    }

    /**
     * Detect the node whether is local file
     *
     * @param imageNode image node
     * @param source ImagePreviewFetcherSource
     * @return local file path
     */
    private suspend fun isLocalFile(
        imageNode: ImageNode,
        source: ImagePreviewFetcherSource,
    ): String? {
        return if (source == ImagePreviewFetcherSource.ZIP) {//handle zip file
            imageNode.fullSizePath
        } else if (source == ImagePreviewFetcherSource.CHAT) {
            MegaNode.unserialize(imageNode.serializedData)?.let { node ->
                checkNodePath(node)
            }
        } else {
            getNodeByHandle(imageNode.id.longValue)?.let { node ->
                checkNodePath(node)
            }
            imageNode.fullSizePath
        }
    }

    private suspend fun checkNodePath(
        node: MegaNode,
    ): String? {
        val localPath = FileUtil.getLocalFile(node)
        return File(FileUtil.getDownloadLocation(), node.name).let { file ->
            if (localPath != null && ((FileUtil.isFileAvailable(file) && file.length() == node.size)
                        || (node.fingerprint == getFingerprintUseCase(localPath)))
            ) {
                localPath
            } else {
                null
            }
        }
    }
}