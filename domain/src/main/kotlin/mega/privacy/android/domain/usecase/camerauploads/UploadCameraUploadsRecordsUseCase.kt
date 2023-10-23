package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsTransferProgress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.CreateTempFileAndRemoveCoordinatesUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DeletePreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DeleteThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadUseCase
import mega.privacy.android.domain.usecase.video.CompressVideoUseCase
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Camera Uploads upload process
 *
 * This use case is responsible to upload the list of [CameraUploadsRecord].
 * For each record, it will check if:
 * - the node does not exists in the cloud : upload
 * - the node exists in another folder except rubbish bin : copy
 * - the node exists in the upload folder : do nothing
 *
 * It will also generate the temporary file to upload in case:
 * - the user set the option to remove gps coordinates
 * - the user set the option to compress the video
 *
 * The use case is also responsible of setting the upload status in the database.
 * It will return a flow of events representing the status progress for an individual record.
 * The caller is responsible to aggregate the information.
 */
class UploadCameraUploadsRecordsUseCase @Inject constructor(
    private val findNodeWithFingerprintInParentNodeUseCase: FindNodeWithFingerprintInParentNodeUseCase,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val setCoordinatesUseCase: SetCoordinatesUseCase,
    private val getNodeGPSCoordinatesUseCase: GetNodeGPSCoordinatesUseCase,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val startUploadUseCase: StartUploadUseCase,
    private val getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase,
    private val setOriginalFingerprintUseCase: SetOriginalFingerprintUseCase,
    private val areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase,
    private val createTempFileAndRemoveCoordinatesUseCase: CreateTempFileAndRemoveCoordinatesUseCase,
    private val setCameraUploadsRecordUploadStatusUseCase: SetCameraUploadsRecordUploadStatusUseCase,
    private val setCameraUploadsRecordGeneratedFingerprintUseCase: SetCameraUploadsRecordGeneratedFingerprintUseCase,
    private val createImageOrVideoThumbnailUseCase: CreateImageOrVideoThumbnailUseCase,
    private val createImageOrVideoPreviewUseCase: CreateImageOrVideoPreviewUseCase,
    private val deleteThumbnailUseCase: DeleteThumbnailUseCase,
    private val deletePreviewUseCase: DeletePreviewUseCase,
    private val compressVideoUseCase: CompressVideoUseCase,
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {

    companion object {
        private const val CONCURRENT_UPLOADS_LIMIT = 16
        private const val CONCURRENT_VIDEO_COMPRESSION_LIMIT = 1
    }

    /**
     * Limit the number of concurrent uploads to [CONCURRENT_UPLOADS_LIMIT]
     * to not overload the memory of the app,
     */
    private val semaphore = Semaphore(CONCURRENT_UPLOADS_LIMIT)

    /**
     * Limit the number of concurrent video compression to [CONCURRENT_VIDEO_COMPRESSION_LIMIT]
     * to not overload the memory and cache size of the app
     */
    private val videoCompressionSemaphore = Semaphore(CONCURRENT_VIDEO_COMPRESSION_LIMIT)

    /**
     * Camera Uploads upload process
     *
     * @param cameraUploadsRecords The list of records to process
     * @param primaryUploadNodeId The primary upload node id
     * @param secondaryUploadNodeId The secondary upload node id
     * @param tempRoot The file path to the temporary folder to generate temp files
     */
    suspend operator fun invoke(
        cameraUploadsRecords: List<CameraUploadsRecord>,
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
        tempRoot: String,
    ) = channelFlow {
        val videoQuality = getUploadVideoQualityUseCase() ?: VideoQuality.ORIGINAL
        val locationTagsDisabled = !areLocationTagsEnabledUseCase()

        cameraUploadsRecords.map { record ->
            val parentNodeId = getParentNodeId(record, primaryUploadNodeId, secondaryUploadNodeId)

            val (existsInParentFolder, existingNodeId) =
                findNodeWithFingerprintInParentNodeUseCase(
                    record.originalFingerprint,
                    record.generatedFingerprint,
                    parentNodeId,
                )
            Triple(record, existsInParentFolder, existingNodeId)
        }.map { (record, existsInParentFolder, existingNodeId) ->
            launch {
                semaphore.acquire()

                val parentNodeId =
                    getParentNodeId(record, primaryUploadNodeId, secondaryUploadNodeId)

                when {
                    // node does not exist => upload
                    existingNodeId == null -> {

                        // create temporary file
                        if (shouldRemoveLocationTags(record.type, locationTagsDisabled)) {
                            flow {
                                emit(
                                    createTempFileAndRemoveCoordinatesUseCase(
                                        tempRoot,
                                        record.filePath,
                                        record.tempFilePath,
                                        record.timestamp,
                                    )
                                )
                            }.retry(60) { cause ->
                                return@retry if (cause is NotEnoughStorageException) {
                                    // total delay (1 second times 60 attempts) = 60 seconds
                                    delay(TimeUnit.SECONDS.toMillis(1))
                                    true
                                } else {
                                    // not storage exception, no need to retry
                                    false
                                }
                            }.catch {
                                setCameraUploadsRecordUploadStatusUseCase(
                                    mediaId = record.mediaId,
                                    timestamp = record.timestamp,
                                    folderType = record.folderType,
                                    uploadStatus =
                                    if (it is FileNotFoundException)
                                        CameraUploadsRecordUploadStatus.LOCAL_FILE_NOT_EXIST
                                    else CameraUploadsRecordUploadStatus.FAILED
                                )
                            }
                                .cancellable()
                                .singleOrNull()
                                ?: run {
                                    semaphore.release()
                                    return@launch
                                }
                        }

                        // Compress Video
                        if (shouldCompressVideo(record.type, videoQuality)) {
                            videoCompressionSemaphore.acquire()
                            compressVideoUseCase(
                                tempRoot,
                                record.filePath,
                                record.tempFilePath,
                                videoQuality,
                            ).catch { emit(VideoCompressionState.Finished) }
                                .onCompletion { videoCompressionSemaphore.release() }
                                .collect {
                                    when (it) {
                                        is VideoCompressionState.Progress,
                                        is VideoCompressionState.Successful
                                        -> {
                                            trySend(
                                                CameraUploadsTransferProgress.Compressing(
                                                    record = record,
                                                    compressionState = it,
                                                )
                                            )
                                        }

                                        is VideoCompressionState.InsufficientStorage,
                                        -> {
                                            trySend(
                                                CameraUploadsTransferProgress.Compressing(
                                                    record = record,
                                                    compressionState = it,
                                                )
                                            )
                                        }

                                        else -> Unit
                                    }
                                }
                        }

                        // generate fingerprint and save it
                        // This step is important to check if a file exist in the cloud drive,
                        // in case the original fingerprint cannot be assigned to the Node after the transfer finishes
                        getFingerprintUseCase(record.tempFilePath)?.let { generatedFingerprint ->
                            setCameraUploadsRecordGeneratedFingerprintUseCase(
                                mediaId = record.mediaId,
                                timestamp = record.timestamp,
                                folderType = record.folderType,
                                generatedFingerprint = generatedFingerprint,
                            )
                        }

                        // retrieve path of file to upload
                        val path = when {
                            shouldRemoveLocationTags(record.type, locationTagsDisabled) -> {
                                record.tempFilePath
                            }

                            shouldCompressVideo(record.type, videoQuality) -> {
                                // Fallback to the original file if for some reason the compression failed
                                record.tempFilePath.takeIf { fileSystemRepository.doesFileExist(it) }
                                    ?: record.filePath
                            }

                            else -> record.filePath
                        }

                        // upload
                        startUploadUseCase(
                            localPath = path,
                            parentNodeId = parentNodeId,
                            fileName = record.fileName,
                            modificationTime = record.timestamp / 1000,
                            appData = TransferType.CU_UPLOAD.name,
                            isSourceTemporary = false,
                            shouldStartFirst = false,
                        ).collect { globalTransfer ->
                            when (globalTransfer) {
                                is TransferEvent.TransferStartEvent -> {
                                    // set status to STARTED
                                    setCameraUploadsRecordUploadStatusUseCase(
                                        mediaId = record.mediaId,
                                        timestamp = record.timestamp,
                                        folderType = record.folderType,
                                        uploadStatus = CameraUploadsRecordUploadStatus.STARTED,
                                    )

                                    trySend(
                                        CameraUploadsTransferProgress.ToUpload(
                                            record = record,
                                            transferEvent = globalTransfer,
                                        )
                                    )
                                }

                                is TransferEvent.TransferFinishEvent -> {
                                    // set the original fingerprint to the Node
                                    setOriginalFingerprintUseCase(
                                        nodeId = NodeId(globalTransfer.transfer.nodeHandle),
                                        originalFingerprint = record.originalFingerprint,
                                    )

                                    // set the GPS coordinates to the Node
                                    val (latitude, longitude) = getGPSCoordinatesUseCase(
                                        filePath = record.filePath,
                                        isVideo = record.type == SyncRecordType.TYPE_VIDEO,
                                    )
                                    setCoordinatesUseCase(
                                        nodeId = NodeId(globalTransfer.transfer.nodeHandle),
                                        latitude = latitude.toDouble(),
                                        longitude = longitude.toDouble(),
                                    )

                                    // set status to UPLOADED
                                    setCameraUploadsRecordUploadStatusUseCase(
                                        mediaId = record.mediaId,
                                        timestamp = record.timestamp,
                                        folderType = record.folderType,
                                        uploadStatus = CameraUploadsRecordUploadStatus.UPLOADED,
                                    )

                                    // delete temp file
                                    fileSystemRepository.deleteFile(File(record.tempFilePath))

                                    // create thumbnail and preview
                                    val nodeHandle = globalTransfer.transfer.nodeHandle
                                    File(record.fileName).let {
                                        if (deleteThumbnailUseCase(nodeHandle)) {
                                            createImageOrVideoThumbnailUseCase(nodeHandle, it)
                                        }
                                        if (deletePreviewUseCase(nodeHandle)) {
                                            createImageOrVideoPreviewUseCase(nodeHandle, it)
                                        }
                                    }

                                    trySend(
                                        CameraUploadsTransferProgress.Uploaded(
                                            record = record,
                                            transferEvent = globalTransfer,
                                            nodeId = NodeId(nodeHandle),
                                        )
                                    )

                                    semaphore.release()
                                }

                                else ->
                                    trySend(
                                        CameraUploadsTransferProgress.UploadInProgress(
                                            record = record,
                                            transferEvent = globalTransfer,
                                        )
                                    )
                            }
                        }
                    }

                    // node exists but not in target folder => copy
                    existsInParentFolder == false -> {
                        trySend(
                            CameraUploadsTransferProgress.ToCopy(
                                record = record,
                                nodeId = existingNodeId,
                            )
                        )
                        copyNodeUseCase(
                            nodeToCopy = existingNodeId,
                            newNodeParent = parentNodeId,
                            newNodeName = record.fileName,
                        ).let { newNodeId ->
                            val (latitude, longitude) = getNodeGPSCoordinatesUseCase(existingNodeId)
                            setCoordinatesUseCase(
                                nodeId = newNodeId,
                                latitude = latitude,
                                longitude = longitude,
                            )
                        }
                        trySend(
                            CameraUploadsTransferProgress.Copied(
                                record = record,
                                nodeId = existingNodeId,
                            )
                        )

                        setCameraUploadsRecordUploadStatusUseCase(
                            mediaId = record.mediaId,
                            timestamp = record.timestamp,
                            folderType = record.folderType,
                            uploadStatus = CameraUploadsRecordUploadStatus.COPIED,
                        )

                        semaphore.release()
                        return@launch
                    }

                    // node exists in target folder or is in rubbish bin => do nothing
                    else -> {
                        setCameraUploadsRecordUploadStatusUseCase(
                            mediaId = record.mediaId,
                            timestamp = record.timestamp,
                            folderType = record.folderType,
                            uploadStatus = CameraUploadsRecordUploadStatus.ALREADY_EXISTS,
                        )
                        semaphore.release()
                        return@launch
                    }
                }
            }
        }.joinAll()
        channel.close()
    }
        .buffer(UNLIMITED)
        .cancellable()

    private fun getParentNodeId(
        record: CameraUploadsRecord,
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId
    ): NodeId = when (record.folderType) {
        CameraUploadFolderType.Primary -> primaryUploadNodeId
        CameraUploadFolderType.Secondary -> secondaryUploadNodeId
    }

    private fun shouldRemoveLocationTags(
        type: SyncRecordType,
        locationTagsDisabled: Boolean,
    ) = type == SyncRecordType.TYPE_PHOTO && locationTagsDisabled

    private fun shouldCompressVideo(
        type: SyncRecordType,
        videoQuality: VideoQuality
    ) = type == SyncRecordType.TYPE_VIDEO && videoQuality != VideoQuality.ORIGINAL
}

