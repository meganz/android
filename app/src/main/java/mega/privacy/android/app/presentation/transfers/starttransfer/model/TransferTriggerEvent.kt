package mega.privacy.android.app.presentation.transfers.starttransfer.model

import android.net.Uri
import androidx.core.net.toUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.transfer.TransferType
import java.io.File

/**
 * Event to trigger the start of a transfer
 */
sealed interface TransferTriggerEvent {

    /**
     * Type of the transfer
     */
    val type: TransferType

    /**
     * Event to start uploading to the chat
     */
    sealed interface StartChatUpload : TransferTriggerEvent {
        override val type: TransferType
            get() = TransferType.CHAT_UPLOAD

        /**
         * The id of the chat where these files will be attached
         */
        val chatId: Long

        /**
         * List of files to be uploaded
         */
        val uris: List<Uri>

        /**
         * Whether this upload is a voice clip
         */
        val isVoiceClip: Boolean

        /**
         * Upload files to chat
         */
        data class Files(
            override val chatId: Long,
            override val uris: List<Uri>,
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
        ) : StartChatUpload {
            override val uris get() = listOf(file.toUri())
            override val isVoiceClip = true
        }
    }

    /**
     * Event to start downloading a list of nodes
     */
    sealed interface DownloadTriggerEvent : TransferTriggerEvent {
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
    ) : DownloadTriggerEvent {
        override val nodes = node?.let { listOf(node) } ?: emptyList()
    }


    /**
     * Event to start downloading a list of nodes to download folder
     * @param nodes list of nodes to be downloaded, they should belong to the same parent folder
     */
    data class StartDownloadNode(
        override val nodes: List<TypedNode>,
        override val isHighPriority: Boolean = false,
    ) : DownloadTriggerEvent

    /**
     * Copy offline node
     *
     * @property nodeIds
     */
    data class CopyOfflineNode(
        val nodeIds: List<NodeId>,
    ) : CopyTriggerEvent

    /**
     * Copy uri
     *
     * @property name
     * @property uri
     */
    data class CopyUri(
        val name: String,
        val uri: Uri,
    ) : CopyTriggerEvent

    /**
     * Event to start downloading node for preview
     *
     * @param node the node to be downloaded for preview
     */
    data class StartDownloadForPreview(
        val node: TypedNode?,
    ) : DownloadTriggerEvent {
        override val nodes = node?.let { listOf(node) } ?: emptyList()
        override val isHighPriority: Boolean = true
    }

    /**
     * Event to start uploading a list of files
     *
     * @property pathsAndNames List of files to be uploaded along with the chosen names for them.
     *                          The name will be null in case the original name.
     * @property destinationId the id of the folder where the files will be uploaded.
     */
    sealed interface StartUpload : TransferTriggerEvent {
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
         */
        data class Files(
            override val pathsAndNames: Map<String, String?>,
            override val destinationId: NodeId,
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
        ) : StartUpload{
            override val isHighPriority = false
        }
    }
}