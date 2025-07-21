package mega.privacy.android.domain.entity.transfer.event

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import java.io.File

/**
 * Event to trigger some transfer action
 */
sealed interface TransferTriggerEvent {
    sealed interface CloudTransfer : TransferTriggerEvent

    /**
     * Type of the transfer
     */
    val type: TransferType

    /**
     * Check if transfers are paused when receiving this trigger event
     */
    val checkPausedTransfers get() = CheckPausedTransfersType.Never

    /**
     * If true and notification permission is not granted, the transfer should not start until user responds to permission request.
     * Useful in case the fragment or activity is closed once the transfer starts, so permission request is not hidden.
     */
    val waitNotificationPermissionResponseToStart: Boolean

    /**
     * Event to start uploading to the chat
     */
    sealed interface StartChatUpload : TransferTriggerEvent {
        override val type: TransferType
            get() = TransferType.CHAT_UPLOAD

        override val checkPausedTransfers get() = CheckPausedTransfersType.OncePerPausedState

        /**
         * The id of the chat where these files will be attached
         */
        val chatId: Long

        /**
         * List of files to be uploaded
         */
        val uris: List<UriPath>

        /**
         * Whether this upload is a voice clip
         */
        val isVoiceClip: Boolean

        /**
         * Upload files to chat
         */
        data class Files(
            override val chatId: Long,
            override val uris: List<UriPath>,
            override val waitNotificationPermissionResponseToStart: Boolean = false,
        ) : StartChatUpload {
            override val isVoiceClip = false
        }

        /**
         * Upload voice clip to chat
         * @param file the voice clip to be uploaded
         *
         */
        data class VoiceClip(
            override val chatId: Long,
            val file: File,
            override val waitNotificationPermissionResponseToStart: Boolean = false,
        ) : StartChatUpload {
            override val uris get() = listOf(UriPath(file.absolutePath))
            override val isVoiceClip = true
        }
    }

    /**
     * Event to start downloading a list of nodes
     */
    sealed interface DownloadTriggerEvent : CloudTransfer {
        override val type: TransferType
            get() = TransferType.DOWNLOAD

        /**
         * nodes to be transferred
         */
        val nodes: List<TypedNode>

        /**
         * true if this download is a high priority transfer, false otherwise
         */
        val isHighPriority: Boolean

        /**
         * App data related to this type of download
         */
        val appData: TransferAppData? get() = null

        /**
         * True if a message should be shown when the transfer starts.
         * It should be true only if the transfers widget is NOT visible.
         */
        val withStartMessage: Boolean
    }

    /**
     * Copy trigger event
     *
     */
    sealed interface CopyTriggerEvent : DownloadTriggerEvent {
        override val type: TransferType
            get() = TransferType.DOWNLOAD

        override val nodes: List<TypedNode>
            get() = emptyList()
        override val isHighPriority: Boolean
            get() = false
    }


    /**
     * Event to start downloading a node for offline use
     * @param node the node to be saved offline
     */
    data class StartDownloadForOffline(
        val node: TypedNode?,
        override val isHighPriority: Boolean = false,
        override val waitNotificationPermissionResponseToStart: Boolean = false,
        override val withStartMessage: Boolean,
    ) : DownloadTriggerEvent {
        override val nodes = node?.let { listOf(node) } ?: emptyList()
        override val appData = TransferAppData.OfflineDownload
    }


    /**
     * Event to start downloading a list of nodes to download folder
     * @param nodes list of nodes to be downloaded, they should belong to the same parent folder
     */
    data class StartDownloadNode(
        override val nodes: List<TypedNode>,
        override val isHighPriority: Boolean = false,
        override val waitNotificationPermissionResponseToStart: Boolean = false,
        override val withStartMessage: Boolean,
    ) : DownloadTriggerEvent

    /**
     * Event to retry downloading a node
     *
     * @param node the node to be retried
     * @param downloadLocation the location where the node should be downloaded
     */
    data class RetryDownloadNode(
        val node: TypedNode?,
        val downloadLocation: String,
        override val isHighPriority: Boolean = false,
        override val waitNotificationPermissionResponseToStart: Boolean = false,
        override val withStartMessage: Boolean = false,
    ) : DownloadTriggerEvent {
        override val nodes = node?.let { listOf(node) } ?: emptyList()
    }

    /**
     * Copy offline node
     *
     * @property nodeIds
     */
    data class CopyOfflineNode(
        val nodeIds: List<NodeId>,
        override val withStartMessage: Boolean = false,
    ) : CopyTriggerEvent {
        override val waitNotificationPermissionResponseToStart = false
    }

    /**
     * Copy uri
     *
     * @property name
     * @property uriPath
     */
    data class CopyUri(
        val name: String,
        val uriPath: UriPath,
        override val waitNotificationPermissionResponseToStart: Boolean = false,
        override val withStartMessage: Boolean = false,
    ) : CopyTriggerEvent

    /**
     * Event to start downloading node for preview
     *
     * @param node the node to be downloaded for preview
     * @param isOpenWith True if is opened with another app action
     */
    data class StartDownloadForPreview(
        val node: TypedNode?,
        val isOpenWith: Boolean,
        override val waitNotificationPermissionResponseToStart: Boolean = false,
        override val withStartMessage: Boolean = false,
    ) : DownloadTriggerEvent {
        override val nodes = node?.let { listOf(node) } ?: emptyList()
        override val isHighPriority: Boolean = true
        override val appData = TransferAppData.PreviewDownload
        override val checkPausedTransfers = CheckPausedTransfersType.Always
    }

    /**
     * Event to start uploading a list of files
     *
     * @property pathsAndNames List of files to be uploaded along with the chosen names for them.
     *                          The name will be null in case the original name.
     * @property destinationId the id of the folder where the files will be uploaded.
     */
    sealed interface StartUpload : CloudTransfer {
        override val type: TransferType
            get() = TransferType.GENERAL_UPLOAD

        val pathsAndNames: Map<String, String?>

        val destinationId: NodeId

        /**
         * true if this upload is a high priority transfer, false otherwise
         */
        val isHighPriority: Boolean

        /**
         * Upload files
         * @param specificStartMessage
         */
        data class Files(
            override val pathsAndNames: Map<String, String?>,
            override val destinationId: NodeId,
            override val waitNotificationPermissionResponseToStart: Boolean = false,
            val specificStartMessage: String? = null
        ) : StartUpload {
            override val isHighPriority = false
        }

        /**
         * Upload text file.
         *
         * @param path the path of the text file to be uploaded.
         * @param destinationId the id of the folder where the file will be uploaded.
         * @param isEditMode true if the file is uploaded in edit mode, false otherwise.
         * @param fromHomePage true if the file is uploaded from home page, false otherwise.
         */
        data class TextFile(
            val path: String,
            override val destinationId: NodeId,
            val isEditMode: Boolean,
            val fromHomePage: Boolean,
            override val waitNotificationPermissionResponseToStart: Boolean = false,
        ) : StartUpload {
            override val pathsAndNames = mapOf(path to null)
            override val isHighPriority = true
        }

        /**
         * Upload collided files.
         *
         * @property collisionChoice the choice made by the user to resolve the name collision.
         */
        data class CollidedFiles(
            val collisionChoice: NameCollisionChoice?,
            override val pathsAndNames: Map<String, String?>,
            override val destinationId: NodeId,
            override val waitNotificationPermissionResponseToStart: Boolean = false,
        ) : StartUpload {
            override val isHighPriority = false
        }
    }

    /**
     * Event to retry a list of transfers that were cancelled or failed.
     *
     * @property idsAndEvents Map of completed transfer ids and the corresponding [CloudTransfer] to be retried.
     */
    data class RetryTransfers(
        val idsAndEvents: Map<Int, CloudTransfer>,
    ) : TransferTriggerEvent {
        override val type = TransferType.NONE
        override val waitNotificationPermissionResponseToStart = false
    }

    /**
     * Specify the need to check if transfers are paused when the [TransferTriggerEvent] is emitted
     */
    enum class CheckPausedTransfersType {
        /**
         * No need to check if transfers are paused
         */
        Never,

        /**
         * Only be checked once, until transfers are paused again
         */
        OncePerPausedState,

        /**
         * Paused transfers should be checked on each [TransferTriggerEvent] emission
         */
        Always,
    }
}