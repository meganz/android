package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case to prepare sync record lists for camera upload
 */
class GetPendingUploadListUseCase @Inject constructor(
    private val getNodeFromCloudUseCase: GetNodeFromCloudUseCase,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val mediaLocalPathExists: MediaLocalPathExists,
    private val shouldCompressVideo: ShouldCompressVideo,
    private val getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val getNodeGPSCoordinatesUseCase: GetNodeGPSCoordinatesUseCase,
) {

    /**
     * Limit the number of concurrent coroutines to process the media list to 16
     * to avoid consuming too much memory
     */
    private val semaphore = Semaphore(16)

    /**
     *  Format the [CameraUploadsMedia] queue to a [SyncRecord] list
     *
     * @param mediaList a queue of [CameraUploadsMedia] retrieved from the MediaStore
     * @param isSecondary true if the media comes from the secondary media folder
     * @param isVideo true if the media are videos
     * @return a list of [SyncRecord]
     */
    suspend operator fun invoke(
        mediaList: List<CameraUploadsMedia>,
        isSecondary: Boolean,
        isVideo: Boolean,
    ): List<SyncRecord> = coroutineScope {
        val parentNodeHandle =
            if (isSecondary) getSecondarySyncHandleUseCase() else getPrimarySyncHandleUseCase()
        val type = if (isVideo) SyncRecordType.TYPE_VIDEO else SyncRecordType.TYPE_PHOTO

        mediaList.map { media ->
            async {
                semaphore.acquire()
                yield()
                val syncRecord = runCatching {
                    // Check if the file is already inserted in the database
                    if (mediaLocalPathExists(media.filePath, isSecondary))
                        return@runCatching null

                    val localFingerPrint =
                        getFingerprintUseCase(media.filePath) ?: return@runCatching null

                    // Check if the file already exists somewhere in the cloud drive
                    val nodeExists =
                        getNodeFromCloudUseCase(
                            originalFingerprint = localFingerPrint,
                            parentNodeId = NodeId(parentNodeHandle),
                        )

                    val sourceFile = File(media.filePath)

                    nodeExists?.let { node ->
                        val isNodeInRubbish = isNodeInRubbish(node.id.longValue)
                        val isNodeNotInCameraUploadsFolder =
                            getParentNodeUseCase(node.id)?.id?.longValue != parentNodeHandle

                        // Check if node exists somewhere else than the camera upload folder
                        // If already in camera upload folder, skip, else copy is needed
                        return@runCatching if (!isNodeInRubbish && isNodeNotInCameraUploadsFolder) {
                            val (latitude, longitude) = getNodeGPSCoordinatesUseCase(nodeExists.id)
                            SyncRecord(
                                localPath = media.filePath,
                                newPath = null,
                                originFingerprint = nodeExists.originalFingerprint,
                                newFingerprint = nodeExists.fingerprint,
                                timestamp = media.timestamp,
                                fileName = sourceFile.name,
                                latitude = latitude.toFloat(),
                                longitude = longitude.toFloat(),
                                status = SyncStatus.STATUS_PENDING.value,
                                type = type,
                                nodeHandle = nodeExists.id.longValue,
                                isCopyOnly = true,
                                isSecondary = isSecondary
                            )
                        } else {
                            null
                        }
                    } ?: run {
                        // The node does not exist, upload is needed
                        val gpsData = getGPSCoordinatesUseCase(sourceFile.absolutePath, isVideo)

                        return@runCatching SyncRecord(
                            localPath = sourceFile.absolutePath,
                            newPath = null,
                            originFingerprint = localFingerPrint,
                            newFingerprint = null,
                            timestamp = media.timestamp,
                            fileName = sourceFile.name,
                            latitude = gpsData.first,
                            longitude = gpsData.second,
                            status =
                            if (shouldCompressVideo() && type == SyncRecordType.TYPE_VIDEO)
                                SyncStatus.STATUS_TO_COMPRESS.value
                            else
                                SyncStatus.STATUS_PENDING.value,
                            type = type,
                            nodeHandle = null,
                            isCopyOnly = false,
                            isSecondary = isSecondary
                        )
                    }
                }.getOrNull()
                semaphore.release()
                syncRecord
            }
        }.awaitAll().filterNotNull()
    }
}
