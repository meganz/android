package mega.privacy.android.app.presentation.transfers.starttransfer.model

import android.net.Uri
import androidx.core.net.toUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
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
         * true if this transfer is a high priority transfer, false otherwise
         */
        val isHighPriority: Boolean
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
     * @property uris list of files to be uploaded.
     * @property destinationId the id of the folder where the files will be uploaded.
     */
    sealed interface StartUpload : TransferTriggerEvent {
        override val type: TransferType
            get() = TransferType.GENERAL_UPLOAD

        val uris: List<Uri>

        val destinationId: NodeId

        /**
         * Upload files
         */
        data class Files(
            override val uris: List<Uri>,
            override val destinationId: NodeId,
        ) : StartUpload
    }
}