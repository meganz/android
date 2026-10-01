package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use case to handle chat transfer events
 */
class HandleCUTransferEventsUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(vararg events: TransferEvent) {
        val cameraUploadEvents = events
            .filter { it.transfer.transferType == TransferType.CU_UPLOAD }

        if (cameraUploadEvents.isEmpty()) return

        val (finishedEvents, updateEvents) = cameraUploadEvents
            .partition { it is TransferEvent.TransferFinishEvent }

        val sortedMap = linkedMapOf<Long, Transfer>()

        // Update CU in progress transfers
        updateEvents.mapNotNull {
            if (it is TransferEvent.TransferStartEvent
                || it is TransferEvent.TransferUpdateEvent
                || it is TransferEvent.TransferPaused
            ) {
                it.transfer
            } else {
                null
            }
        }.forEach { transfer ->
            sortedMap.remove(transfer.uniqueId) //remove and then add to be sure that in case of duplicated, the new one is the last one, instead of replacing the value for existing key
            sortedMap[transfer.uniqueId] = transfer
        }

        sortedMap.values.toList().takeIf { it.isNotEmpty() }?.let { updatedCUTransfers ->
            cameraUploadsRepository.updateCameraUploadsInProgressTransfers(updatedCUTransfers)
        }

        // Remove CU in progress transfers
        finishedEvents.map { it.transfer.uniqueId }.toSet().let { finishedCUUniqueIds ->
            cameraUploadsRepository.removeCameraUploadsInProgressTransfers(finishedCUUniqueIds)
        }
    }
}