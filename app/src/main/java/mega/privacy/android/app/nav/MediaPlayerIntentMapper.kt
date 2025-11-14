package mega.privacy.android.app.nav

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerComposeActivity
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_FOLDER_LINK
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PLACEHOLDER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_ADD_TO_ALBUM
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import javax.inject.Inject

/**
 * Mapper to create media player intent
 */
class MediaPlayerIntentMapper @Inject constructor(
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
) {
    /**
     * Create media player intent
     *
     * @return Intent for media player activity
     */
    operator fun invoke(
        context: Context,
        contentUri: NodeContentUri,
        fileTypeInfo: FileTypeInfo,
        sortOrder: SortOrder,
        name: String,
        handle: Long,
        parentHandle: Long,
        isFolderLink: Boolean,
        isMediaQueueAvailable: Boolean = true,
        viewType: Int? = null,
        path: String? = null,
        offlineParentId: Int? = null,
        offlineParent: String? = null,
        searchedItems: List<Long>? = null,
        mediaQueueTitle: String? = null,
        nodeHandles: List<Long>? = null,
        collectionTitle: String? = null,
        collectionId: Long? = null,
        enableAddToAlbum: Boolean = false,
    ): Intent {
        val intent = getIntent(context, fileTypeInfo).apply {
            putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, sortOrder)
            putExtra(INTENT_EXTRA_KEY_PLACEHOLDER, 0)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, name)
            putExtra(INTENT_EXTRA_KEY_HANDLE, handle)
            putExtra(INTENT_EXTRA_KEY_IS_FOLDER_LINK, isFolderLink)
            viewType?.let {
                putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, viewType)
            }
            if (isMediaQueueAvailable) {
                putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, parentHandle)
            }
            putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, isMediaQueueAvailable)
            path?.let {
                putExtra(INTENT_EXTRA_KEY_PATH, path)
            }
            offlineParentId?.let {
                putExtra(INTENT_EXTRA_KEY_PARENT_ID, it)
            }
            offlineParent?.let {
                putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, offlineParent)
            }
            searchedItems?.let {
                putExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH, it.toLongArray())
            }
            mediaQueueTitle?.let {
                putExtra(INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE, it)
            }
            nodeHandles?.let {
                putExtra(NODE_HANDLES, it.toLongArray())
            }
            collectionTitle?.let {
                putExtra(INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE, it)
            }
            collectionId?.let {
                putExtra(INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID, it)
            }
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(INTENT_EXTRA_KEY_VIDEO_ADD_TO_ALBUM, enableAddToAlbum)
        }
        val mimeType =
            if (fileTypeInfo.extension == "opus") "audio/*" else fileTypeInfo.mimeType
        nodeContentUriIntentMapper(intent, contentUri, mimeType, fileTypeInfo.isSupported)
        return intent
    }

    private fun getIntent(context: Context, fileTypeInfo: FileTypeInfo) = when {
        fileTypeInfo.isSupported && fileTypeInfo is VideoFileTypeInfo ->
            Intent(context, VideoPlayerComposeActivity::class.java)

        fileTypeInfo.isSupported && fileTypeInfo is AudioFileTypeInfo ->
            Intent(context, AudioPlayerActivity::class.java)

        else -> Intent(Intent.ACTION_VIEW)
    }
}

