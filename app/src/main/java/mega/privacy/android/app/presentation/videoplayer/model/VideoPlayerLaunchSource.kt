package mega.privacy.android.app.presentation.videoplayer.model

import android.net.Uri
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

/**
 * Immutable description of what the video player should play, decoupled from the launch
 * [android.content.Intent].
 * @property contentUri URI of the first item to play (was `intent.data`).
 * @property handle handle of the first playing video.
 * @property fileName file name of the first playing video.
 * @property adapterType launch source (see `NodeSourceTypeInt` / `Constants` adapter types).
 * @property isPlaylist whether a playlist/queue should be built around the first item.
 * @property rebuildPlaylist whether the playlist should be rebuilt.
 * @property needStopHttpServer whether the streaming server should be stopped on close.
 * @property parentId offline parent id (offline source).
 * @property offlinePathDirectory zip directory path (zip source).
 * @property parentHandle parent node handle (general source).
 * @property nodeHandles explicit node handles for the queue (recents source).
 * @property searchedItems node handles for the queue (search source).
 * @property mediaQueueTitle queue title (search source).
 * @property contactEmail contact email (contact-file source).
 * @property sortOrder sort order for building the queue.
 * @property fileLinkUrl public file-link URL (file-link source).
 * @property localFilePath local file path (local-file source).
 * @property collectionTitle title of the video collection, if launched from one.
 * @property collectionId id of the video collection, if launched from one.
 */
data class VideoPlayerLaunchSource(
    val contentUri: Uri?,
    val handle: Long,
    val fileName: String?,
    val adapterType: Int,
    val isPlaylist: Boolean = true,
    val rebuildPlaylist: Boolean = true,
    val needStopHttpServer: Boolean = false,
    val parentId: Int = -1,
    val offlinePathDirectory: String? = null,
    val parentHandle: Long = INVALID_HANDLE,
    val nodeHandles: List<Long>? = null,
    val searchedItems: List<Long>? = null,
    val mediaQueueTitle: String? = null,
    val contactEmail: String? = null,
    val sortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val fileLinkUrl: String? = null,
    val localFilePath: String? = null,
    val collectionTitle: String? = null,
    val collectionId: Long? = null,
)
