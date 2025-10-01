package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.domain.entity.transfer.InProgressTransfer

sealed class CameraUploadsTransferType {
    /**
     * In progress transfers
     */
    data class InProgress(val items: List<InProgressTransfer>) : CameraUploadsTransferType()

    /**
     * In queue transfers
     */
    data class InQueue(val items: List<InProgressTransfer>) : CameraUploadsTransferType()
}