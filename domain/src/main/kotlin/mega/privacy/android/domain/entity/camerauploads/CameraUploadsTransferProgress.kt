package mega.privacy.android.domain.entity.camerauploads

import mega.privacy.android.domain.entity.VideoCompressionState
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
    data class UploadInProgress(
        override val record: CameraUploadsRecord,
        val transferEvent: TransferEvent,
    ) : CameraUploadsTransferProgress

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
     *
     * @property compressionState the state of the compression
     */
    data class Compressing(
        override val record: CameraUploadsRecord,
        val compressionState: VideoCompressionState,
    ) : CameraUploadsTransferProgress
}
