package mega.privacy.android.domain.entity.camerauploads

import java.util.Hashtable
import kotlin.math.roundToInt

/**
 *  Camera Upload folder state
 *
 * @property lastTimestamp
 * @property lastHandle
 * @property toUploadCount Number of files to upload
 * @property uploadedCount Number of files uploaded
 * @property bytesToUploadCount Number of bytes to upload
 * @property bytesFinishedUploadedCount Number of bytes uploaded that corresponds to finished uploads
 * @property bytesInProgressUploadedTable Number of bytes uploaded per upload that is in progress. Each upload is identified with the local path.
 *                                        Once an upload is finished, the item is removed from this table and the bytes are added to bytesFinishedUploadedCount
 * @property bytesUploadedCount Number of bytes uploaded
 * @property pendingCount Number of files remaining to be uploaded
 * @property progress current progress of Camera Uploads process for the folder. A [Int] between 0 and 100
 */
data class CameraUploadsFolderState(
    var lastTimestamp: Long = -1,
    var lastHandle: Long = -1,
    var toUploadCount: Int = 0,
    var uploadedCount: Int = 0,
    var bytesToUploadCount: Long = 0,
    var bytesFinishedUploadedCount: Long = 0,
    var bytesInProgressUploadedTable: Hashtable<Int, Long> = Hashtable()
) {
    val bytesUploadedCount: Long
        get() = bytesFinishedUploadedCount + bytesInProgressUploadedTable.values.sum()

    val pendingCount: Int
        get() = toUploadCount - uploadedCount

    val progress: Int
        get() =
            when (bytesToUploadCount) {
                0L -> 0
                else -> ((bytesUploadedCount.toDouble() / bytesToUploadCount) * 100).roundToInt()
            }

    /**
     *  Reset the uploads counts
     */
    fun resetUploadsCounts() {
        toUploadCount = 0
        uploadedCount = 0
        bytesToUploadCount = 0
        bytesFinishedUploadedCount = 0
        bytesInProgressUploadedTable.clear()
    }
}
