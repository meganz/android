package mega.privacy.android.app.mediaplayer.model

import android.content.Intent
import android.net.Uri
import android.os.Build
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_EMAIL
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.navigation.ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

/**
 * Parameters for building an audio play queue, extracted from a launch [Intent].
 *
 * @property adapterType the node-source adapter type
 * @property handle the node handle of the item to play first
 * @property fileName display name of the first item
 * @property uri direct stream URI (used for non-folder-link adapters as the first-emit URI)
 * @property parentHandle parent folder node handle (may be [INVALID_HANDLE])
 * @property isPlayQueue whether the full sibling play queue should be built
 * @property offlineParentId parent folder id for [mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER]
 * @property zipPath path inside the zip for [mega.privacy.android.app.utils.Constants.ZIP_ADAPTER]
 * @property contactEmail contact email for [mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER]
 * @property handles node handles for [mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER]
 * @property searchHandles node handles for [mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER]
 * @property sortOrder sort order for fetching sibling nodes
 * @property needStopHttpServer whether the HTTP streaming server should be stopped on destroy
 */
data class AudioPlayQueueParams(
    val adapterType: Int,
    val handle: Long,
    val fileName: String,
    val uri: Uri,
    val parentHandle: Long,
    val isPlayQueue: Boolean,
    val offlineParentId: Int,
    val zipPath: String?,
    val contactEmail: String?,
    val handles: List<Long>?,
    val searchHandles: List<Long>?,
    val sortOrder: SortOrder,
    val needStopHttpServer: Boolean,
) {
    companion object {
        /**
         * Extract [AudioPlayQueueParams] from a start-command [Intent].
         *
         * Returns null when required extras are missing or invalid.
         */
        fun from(intent: Intent): AudioPlayQueueParams? {
            val adapterType = intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
            if (adapterType == INVALID_VALUE) return null

            val uri = intent.data ?: return null

            val handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
            if (handle == INVALID_HANDLE) return null

            val fileName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: return null

            val sortOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(
                    INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                    SortOrder::class.java,
                ) ?: SortOrder.ORDER_DEFAULT_ASC
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN) as SortOrder?
                    ?: SortOrder.ORDER_DEFAULT_ASC
            }

            return AudioPlayQueueParams(
                adapterType = adapterType,
                handle = handle,
                fileName = fileName,
                uri = uri,
                parentHandle = intent.getLongExtra(
                    INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                    INVALID_HANDLE,
                ),
                isPlayQueue = intent.getBooleanExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true),
                offlineParentId = intent.getIntExtra(INTENT_EXTRA_KEY_PARENT_ID, -1),
                zipPath = intent.getStringExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY),
                contactEmail = intent.getStringExtra(INTENT_EXTRA_KEY_CONTACT_EMAIL),
                handles = intent.getLongArrayExtra(NODE_HANDLES)?.toList(),
                searchHandles = intent.getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)?.toList(),
                sortOrder = sortOrder,
                needStopHttpServer = intent.getBooleanExtra(
                    INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                    false,
                ),
            )
        }
    }
}
