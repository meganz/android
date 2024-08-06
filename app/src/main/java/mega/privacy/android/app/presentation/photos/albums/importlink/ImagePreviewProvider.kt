package mega.privacy.android.app.presentation.photos.albums.importlink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.MediaDiscoveryImageNodeFetcher.Companion.IS_RECURSIVE
import mega.privacy.android.app.presentation.imagepreview.fetcher.MediaDiscoveryImageNodeFetcher.Companion.PARENT_ID
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.FROM_ALBUM_SHARING
import mega.privacy.android.app.utils.Constants.FROM_MEDIA_DISCOVERY
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetAlbumPhotoFileUrlByNodeIdUseCase
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.navigation.MegaNavigator
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * AlbumImport Preview help class
 */
class ImagePreviewProvider @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getAlbumPhotoFileUrlByNodeIdUseCase: GetAlbumPhotoFileUrlByNodeIdUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val monitorSubFolderMediaDiscoverySettingsUseCase: MonitorSubFolderMediaDiscoverySettingsUseCase,
    private val megaNavigator: MegaNavigator,
    private val getNodeContentUriByHandleUseCase: GetNodeContentUriByHandleUseCase,
) {

    /**
     * onPreviewPhoto
     */
    fun onPreviewPhoto(
        activity: Activity,
        photo: Photo,
    ) {
        if (photo is Photo.Video) {
            (activity as LifecycleOwner).lifecycleScope.launch {
                launchVideoScreenFromAlbumSharing(activity = activity, photo = photo)
            }
        } else {
            startImagePreviewFromAlbumSharing(activity = activity, photo = photo)
        }
    }

    fun onPreviewPhotoFromMD(
        activity: Activity,
        photo: Photo,
        photoIds: List<Long>,
        currentSort: Sort,
        isFolderLink: Boolean = false,
        folderNodeId: Long? = null,
    ) {
        if (photo is Photo.Video) {
            (activity as LifecycleOwner).lifecycleScope.launch {
                launchVideoScreenFromMD(
                    activity = activity,
                    photo = photo,
                    currentSort = currentSort,
                    isFolderLink = isFolderLink,
                    folderNodeId = folderNodeId,
                )
            }
        } else {
            startImagePreviewFromMD(
                activity = activity,
                photo = photo,
                folderNodeId = folderNodeId,
            )
        }
    }

    private fun startImagePreviewFromMD(
        activity: Activity,
        photo: Photo,
        folderNodeId: Long?,
    ) {
        (activity as LifecycleOwner).lifecycleScope.launch {
            folderNodeId?.let { parentID ->
                val recursive =
                    monitorSubFolderMediaDiscoverySettingsUseCase().first()
                ImagePreviewActivity.createIntent(
                    context = activity,
                    imageSource = ImagePreviewFetcherSource.MEDIA_DISCOVERY,
                    menuOptionsSource = ImagePreviewMenuSource.MEDIA_DISCOVERY,
                    anchorImageNodeId = NodeId(photo.id),
                    params = mapOf(PARENT_ID to parentID, IS_RECURSIVE to recursive),
                ).run {
                    activity.startActivity(this)
                }
            }
        }
    }

    private fun startImagePreviewFromAlbumSharing(
        activity: Activity,
        photo: Photo,
    ) = (activity as LifecycleOwner).lifecycleScope.launch {
        val intent = ImagePreviewActivity.createIntent(
            context = activity,
            imageSource = ImagePreviewFetcherSource.ALBUM_SHARING,
            menuOptionsSource = ImagePreviewMenuSource.ALBUM_SHARING,
            anchorImageNodeId = NodeId(photo.id),
        )
        activity.startActivity(intent)
    }

    /**
     * Launch video player
     *
     * @param activity
     * @param photo Photo item
     */
    private suspend fun launchVideoScreenFromAlbumSharing(activity: Activity, photo: Photo) {
        val nodeHandle = photo.id
        val nodeName = photo.name
        startMediaActivity(
            activity = activity,
            nodeHandle = nodeHandle,
            name = nodeName,
            viewType = FROM_ALBUM_SHARING,
            parentId = getNodeParentHandle(nodeHandle)
        )
    }

    private suspend fun launchVideoScreenFromMD(
        activity: Activity,
        photo: Photo,
        currentSort: Sort,
        isFolderLink: Boolean = false,
        folderNodeId: Long? = null,
    ) {
        val nodeHandle = photo.id
        val nodeName = photo.name
        startMediaActivity(
            activity = activity,
            nodeHandle = nodeHandle,
            name = nodeName,
            viewType = FROM_MEDIA_DISCOVERY,
            parentId = folderNodeId ?: getNodeParentHandle(nodeHandle),
            sortOrder = if (currentSort == Sort.NEWEST) {
                SortOrder.ORDER_MODIFICATION_DESC
            } else {
                SortOrder.ORDER_MODIFICATION_ASC
            },
            isFolderLink = isFolderLink
        )
    }

    private suspend fun startMediaActivity(
        activity: Activity,
        nodeHandle: Long,
        name: String,
        viewType: Int,
        parentId: Long? = null,
        sortOrder: SortOrder? = null,
        isFolderLink: Boolean = false,
    ) {
        runCatching {
            isLocalFile(nodeHandle)?.let { localPath ->
                val file = File(localPath)
                megaNavigator.openMediaPlayerActivityByLocalFile(
                    context = activity,
                    localFile = file,
                    handle = nodeHandle,
                    parentId = parentId ?: -1,
                    viewType = viewType,
                    sortOrder = sortOrder ?: SortOrder.ORDER_NONE,
                    isFolderLink = isFolderLink
                )
            } ?: run {
                val contentUri = getNodeContentUriByHandleUseCase(nodeHandle)
                megaNavigator.openMediaPlayerActivity(
                    context = activity,
                    contentUri = contentUri,
                    name = name,
                    handle = nodeHandle,
                    parentId = parentId ?: -1,
                    viewType = viewType,
                    sortOrder = sortOrder ?: SortOrder.ORDER_NONE,
                    isFolderLink = isFolderLink
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Get node parent handle
     *
     * @param handle node handle
     * @return parent handle
     */
    private suspend fun getNodeParentHandle(handle: Long): Long? =
        getNodeByHandle(handle)?.parentHandle

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return true is local file, otherwise is false
     */
    private suspend fun isLocalFile(
        handle: Long,
    ): String? =
        getNodeByHandle(handle)?.let { node ->
            val localPath = FileUtil.getLocalFile(node)
            File(FileUtil.getDownloadLocation(), node.name).let { file ->
                if (localPath != null && ((FileUtil.isFileAvailable(file) && file.length() == node.size)
                            || (node.fingerprint == getFingerprintUseCase(localPath)))
                ) {
                    localPath
                } else {
                    null
                }
            }
        }

    /**
     * Update intent
     *
     * @param handle node handle
     * @param name node name
     * @param intent Intent
     * @return updated intent
     */
    private suspend fun updateIntent(
        handle: Long,
        name: String,
        intent: Intent,
        isFolderLink: Boolean = false,
        isAlbumSharing: Boolean = false,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        when {
            isAlbumSharing -> {
                getAlbumPhotoFileUrlByNodeIdUseCase(NodeId(handle))
            }

            isFolderLink -> {
                getLocalFolderLinkFromMegaApiUseCase(handle)
            }

            else -> {
                getFileUrlByNodeHandleUseCase(handle)
            }
        }?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, MimeTypeList.typeForName(name).type)
            }
        }

        return intent
    }
}