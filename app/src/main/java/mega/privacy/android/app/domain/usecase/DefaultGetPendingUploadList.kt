package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.yield
import mega.privacy.android.data.mapper.camerauploads.SyncRecordTypeIntMapper
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetGPSCoordinates
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import mega.privacy.android.domain.usecase.camerauploads.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.util.Queue
import javax.inject.Inject

/**
 * Use case to prepare sync record lists for camera upload
 */
class DefaultGetPendingUploadList @Inject constructor(
    private val getNodeFromCloud: GetNodeFromCloud,
    private val getNodeByHandle: GetNodeByHandle,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val updateTimeStamp: UpdateCameraUploadTimeStamp,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val mediaLocalPathExists: MediaLocalPathExists,
    private val shouldCompressVideo: ShouldCompressVideo,
    private val getGPSCoordinates: GetGPSCoordinates,
    private val syncRecordTypeIntMapper: SyncRecordTypeIntMapper,
    private val isNodeInRubbish: IsNodeInRubbish,
) : GetPendingUploadList {

    override suspend fun invoke(
        mediaList: Queue<CameraUploadMedia>,
        isSecondary: Boolean,
        isVideo: Boolean,
    ): List<SyncRecord> {
        Timber.d("GetPendingUploadList - is secondary upload: $isSecondary, is video: $isVideo")
        val pendingList = mutableListOf<SyncRecord>()
        val parentNodeHandle =
            if (isSecondary) getSecondarySyncHandleUseCase() else getPrimarySyncHandleUseCase()
        Timber.d("Upload to parent node with handle: $parentNodeHandle")
        val type = if (isVideo) SyncRecordType.TYPE_VIDEO else SyncRecordType.TYPE_PHOTO

        while (mediaList.size > 0) {
            yield()
            val media = mediaList.poll() ?: continue
            if (media.filePath?.let { mediaLocalPathExists(it, isSecondary) } == true) {
                Timber.d("Skip media with timestamp: ${media.timestamp}")
                continue
            }

            val sourceFile = media.filePath?.let { File(it) }
            val localFingerPrint = media.filePath?.let { getFingerprintUseCase(it) }
            var nodeExists: MegaNode? = null
            try {
                nodeExists = getNodeByHandle(parentNodeHandle)?.let { node ->
                    localFingerPrint?.let { fingerprint ->
                        getNodeFromCloud(fingerprint, node)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            if (nodeExists == null) {
                Timber.d("Possible node with same fingerprint is null")
                val gpsData = sourceFile?.let {
                    getGPSCoordinates(
                        it.absolutePath,
                        isVideo
                    )
                }
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
                    syncRecordTypeIntMapper(type),
                    null,
                    false,
                    isSecondary
                )
                Timber.d("Add local file with timestamp: ${record.timestamp} to pending list to upload")
                pendingList.add(record)
            } else {
                Timber.d("Possible node with same fingerprint with handle: ${nodeExists.handle}")
                if (!isNodeInRubbish(nodeExists.handle) && getParentNodeUseCase(NodeId(nodeExists.handle))?.id?.longValue != parentNodeHandle) {
                    val record = SyncRecord(
                        0,
                        media.filePath,
                        null,
                        nodeExists.originalFingerprint,
                        nodeExists.fingerprint,
                        media.timestamp,
                        sourceFile?.name,
                        nodeExists.longitude.toFloat(),
                        nodeExists.latitude.toFloat(),
                        SyncStatus.STATUS_PENDING.value,
                        syncRecordTypeIntMapper(type),
                        nodeExists.handle,
                        true,
                        isSecondary
                    )
                    Timber.d("Add local file with handle: ${record.nodeHandle} to pending list to copy")
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
