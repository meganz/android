package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import java.math.BigInteger

/**
 * A representation of an in progress transfer.
 */
sealed interface InProgressTransfer : TypedTransfer, InProgressTransferData {

    /**
     * A representation of an in progress download transfer.
     *
     * @property nodeId The node id of the node being downloaded.
     */
    data class Download(
        override val tag: Int,
        override val totalBytes: Long,
        override val isPaused: Boolean,
        override val fileName: String,
        override val speed: Long,
        override val state: TransferState,
        override val priority: BigInteger,
        override val progress: Progress,
        val nodeId: NodeId,
    ) : InProgressTransfer

    /**
     * A representation of an in progress upload transfer.
     *
     * @property localPath The local path of the file being uploaded.
     */
    data class Upload(
        override val tag: Int,
        override val totalBytes: Long,
        override val isPaused: Boolean,
        override val fileName: String,
        override val speed: Long,
        override val state: TransferState,
        override val priority: BigInteger,
        override val progress: Progress,
        val localPath: String,
    ) : InProgressTransfer
}