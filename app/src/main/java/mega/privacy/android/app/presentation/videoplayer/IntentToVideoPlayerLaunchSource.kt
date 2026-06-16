package mega.privacy.android.app.presentation.videoplayer

import android.content.Intent
import android.os.Build
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerLaunchSource
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.navigation.ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

/**
 * Builds a [VideoPlayerLaunchSource] from a legacy launch [Intent].
 *
 * Transitional bridge that lets the decoupled [ComposeVideoPlayerViewModel] (and its tests) be fed
 * from an existing launch Intent. Once the navigator builds the source directly for the Compose
 * route, this mapper can be removed.
 */
internal fun Intent.toVideoPlayerLaunchSource(): VideoPlayerLaunchSource =
    VideoPlayerLaunchSource(
        contentUri = data,
        handle = getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE),
        fileName = getStringExtra(INTENT_EXTRA_KEY_FILE_NAME),
        adapterType = getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE),
        isPlaylist = getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true),
        rebuildPlaylist = getBooleanExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, true),
        needStopHttpServer = getBooleanExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, false),
        parentId = getIntExtra(INTENT_EXTRA_KEY_PARENT_ID, -1),
        offlinePathDirectory = getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY),
        parentHandle = getLongExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE),
        nodeHandles = getLongArrayExtra(NODE_HANDLES)?.toList(),
        searchedItems = getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)?.toList(),
        mediaQueueTitle = getStringExtra(INTENT_EXTRA_KEY_MEDIA_QUEUE_TITLE),
        contactEmail = getStringExtra(INTENT_EXTRA_KEY_CONTACT_EMAIL),
        sortOrder = sortOrderFromIntent(),
    )

private fun Intent.sortOrderFromIntent(): SortOrder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, SortOrder::class.java)
            ?: SortOrder.ORDER_DEFAULT_ASC
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN) as? SortOrder
            ?: SortOrder.ORDER_DEFAULT_ASC
    }
