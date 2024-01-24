package mega.privacy.android.domain.entity.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferEvent

/**
 * Represents the uploading process progress of a [CameraUploadsRecord] in Camera Uploads feature
 *
 * @property record
 */
sealed interface CameraUploadsTransferProgress {
    val record: CameraUploadsRecord

    /**
     * Represents a record which encounter an error during its upload
     *
     * @param error the error encountered
     */
    data class Error(
        override val record: CameraUploadsRecord,
        val error: Throwable
    ) : CameraUploadsTransferProgress

    /**
     * Represents a record to copy in the scope of uploading the camera uploads record through CameraUploads
     *
     * @property nodeId the existing node id to copy
     */
    data class ToCopy(
        override val record: CameraUploadsRecord,
        val nodeId: NodeId,
    ) : CameraUploadsTransferProgress

    /**
     * Represents a record that completes copy in the scope of uploading the camera uploads record through CameraUploads
     *
     * @property nodeId the existing node id copied
     */
    data class Copied(
        override val record: CameraUploadsRecord,
        val nodeId: NodeId,
    ) : CameraUploadsTransferProgress

    /**
     * Represents a record to upload in the scope of uploading the camera uploads record through CameraUploads
     *
     * @property transferEvent the transfer event associated to the record
     */
    data class ToUpload(
        override val record: CameraUploadsRecord,
        val transferEvent: TransferEvent,
    ) : CameraUploadsTransferProgress

    /**
     * Represents a record currently uploading in the scope of uploading the camera uploads record through CameraUploads
     *
     * @property transferEvent the transfer event associated to the record
     */
    sealed interface UploadInProgress : CameraUploadsTransferProgress {
        val transferEvent: TransferEvent

        /**
         * Represents a record that does not have sufficient space to perform video compression
         */
        data class TransferUpdate(
            override val record: CameraUploadsRecord,
            override val transferEvent: TransferEvent.TransferUpdateEvent
        ) : UploadInProgress

        /**
         * Represents a record that does not have sufficient space to perform video compression
         */
        data class TransferTemporaryError(
            override val record: CameraUploadsRecord,
            override val transferEvent: TransferEvent.TransferTemporaryErrorEvent
        ) : UploadInProgress
    }

    /**
     * Represents a record that completes an upload in the scope of uploading the camera uploads record through CameraUploads
     *
     * @property transferEvent the transfer event associated to the record
     * @property nodeId the result node id after the completion of the upload
     */
    data class Uploaded(
        override val record: CameraUploadsRecord,
        val transferEvent: TransferEvent.TransferFinishEvent,
        val nodeId: NodeId,
    ) : CameraUploadsTransferProgress

    /**
     * Represents a record associated to a video that is under compression
     */
    sealed interface Compressing : CameraUploadsTransferProgress {
        /**
         * Represents a record that does not have sufficient space to perform video compression
         */
        data class InsufficientStorage(
            override val record: CameraUploadsRecord,
        ) : Compressing

        /**
         * Represents a record that is under compression
         *
         * @property progress the current progress of the compression represented by a [Float] between 0 and 1
         */
        data class Progress(
            override val record: CameraUploadsRecord,
            val progress: Float,
        ) : Compressing

        /**
         * Represents a record that successfully complete compression
         */
        data class Successful(
            override val record: CameraUploadsRecord,
        ) : Compressing

        /**
         * Represents a record that compression cancelled
         */
        data class Cancel(
            override val record: CameraUploadsRecord,
        ) : Compressing
    }
}
