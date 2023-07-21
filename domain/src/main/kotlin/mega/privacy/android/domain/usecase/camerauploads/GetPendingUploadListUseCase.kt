package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import java.io.File
import java.util.Queue
import javax.inject.Inject

/**
 * Use case to prepare sync record lists for camera upload
 */
class GetPendingUploadListUseCase @Inject constructor(
    private val getNodeFromCloudUseCase: GetNodeFromCloudUseCase,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val updateTimeStamp: UpdateCameraUploadTimeStamp,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val mediaLocalPathExists: MediaLocalPathExists,
    private val shouldCompressVideo: ShouldCompressVideo,
    private val getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val getNodeGPSCoordinatesUseCase: GetNodeGPSCoordinatesUseCase,
) {

    suspend operator fun invoke(
        mediaList: Queue<CameraUploadMedia>,
        isSecondary: Boolean,
        isVideo: Boolean,
    ): List<SyncRecord> {
        val pendingList = mutableListOf<SyncRecord>()
        val parentNodeHandle =
            if (isSecondary) getSecondarySyncHandleUseCase() else getPrimarySyncHandleUseCase()
        val type = if (isVideo) SyncRecordType.TYPE_VIDEO else SyncRecordType.TYPE_PHOTO

        while (mediaList.size > 0) {
            yield()
            val media = mediaList.poll() ?: continue
            if (media.filePath?.let { mediaLocalPathExists(it, isSecondary) } == true) {
                continue
            }

            val sourceFile = media.filePath?.let { File(it) }
            val localFingerPrint = media.filePath?.let { getFingerprintUseCase(it) }
            val nodeExists = localFingerPrint?.let { fingerprint ->
                getNodeFromCloudUseCase(fingerprint, NodeId(parentNodeHandle))
            }

            if (nodeExists == null) {
                val gpsData = sourceFile?.let { getGPSCoordinatesUseCase(it.absolutePath, isVideo) }
                val record = SyncRecord(
                    0,
                    sourceFile?.absolutePath,
                    null,
                    localFingerPrint,
                    null,
                    media.timestamp,
                    sourceFile?.name,
                    gpsData?.second,
                    gpsData?.first,
                    if (shouldCompressVideo() && type == SyncRecordType.TYPE_VIDEO)
                        SyncStatus.STATUS_TO_COMPRESS.value
                    else
                        SyncStatus.STATUS_PENDING.value,
                    type,
                    null,
                    false,
                    isSecondary
                )
                pendingList.add(record)
            } else {
                if (!isNodeInRubbish(nodeExists.id.longValue) && getParentNodeUseCase(nodeExists.id)?.id?.longValue != parentNodeHandle) {
                    val (latitude, longitude) = getNodeGPSCoordinatesUseCase(nodeExists.id)
                    val record = SyncRecord(
                        0,
                        media.filePath,
                        null,
                        nodeExists.originalFingerprint,
                        nodeExists.fingerprint,
                        media.timestamp,
                        sourceFile?.name,
                        latitude.toFloat(),
                        longitude.toFloat(),
                        SyncStatus.STATUS_PENDING.value,
                        type,
                        nodeExists.id.longValue,
                        true,
                        isSecondary
                    )
                    pendingList.add(record)
                } else {
                    if (isVideo) {
                        updateTimeStamp(media.timestamp, SyncTimeStamp.PRIMARY_VIDEO)
                        updateTimeStamp(media.timestamp, SyncTimeStamp.SECONDARY_VIDEO)
                    } else {
                        updateTimeStamp(media.timestamp, SyncTimeStamp.PRIMARY_PHOTO)
                        updateTimeStamp(media.timestamp, SyncTimeStamp.SECONDARY_PHOTO)
                    }
                }
            }
        }
        return pendingList
    }
}
