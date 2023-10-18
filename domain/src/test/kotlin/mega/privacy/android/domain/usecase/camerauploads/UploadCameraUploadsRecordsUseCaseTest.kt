package mega.privacy.android.domain.usecase.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsTransferProgress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileNotFoundException
import java.util.stream.Stream


@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadCameraUploadsRecordsUseCaseTest {
    private lateinit var underTest: UploadCameraUploadsRecordsUseCase

    private val findNodeWithFingerprintInParentNodeUseCase =
        mock<FindNodeWithFingerprintInParentNodeUseCase>()
    private val copyNodeUseCase = mock<CopyNodeUseCase>()
    private val setCoordinatesUseCase = mock<SetCoordinatesUseCase>()
    private val getNodeGPSCoordinatesUseCase = mock<GetNodeGPSCoordinatesUseCase>()
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val startUploadUseCase = mock<StartUploadUseCase>()
    private val getGPSCoordinatesUseCase = mock<GetGPSCoordinatesUseCase>()
    private val setOriginalFingerprintUseCase = mock<SetOriginalFingerprintUseCase>()
    private val areLocationTagsEnabledUseCase = mock<AreLocationTagsEnabledUseCase>()
    private val createTempFileAndRemoveCoordinatesUseCase =
        mock<CreateTempFileAndRemoveCoordinatesUseCase>()
    private val setCameraUploadsRecordUploadStatusUseCase =
        mock<SetCameraUploadsRecordUploadStatusUseCase>()
    private val setCameraUploadsRecordGeneratedFingerprintUseCase =
        mock<SetCameraUploadsRecordGeneratedFingerprintUseCase>()
    private val createImageOrVideoThumbnailUseCase = mock<CreateImageOrVideoThumbnailUseCase>()
    private val createImageOrVideoPreviewUseCase = mock<CreateImageOrVideoPreviewUseCase>()
    private val deleteThumbnailUseCase = mock<DeleteThumbnailUseCase>()
    private val deletePreviewUseCase = mock<DeletePreviewUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    val primaryUploadNodeId = NodeId(1111L)
    val secondaryUploadNodeId = NodeId(2222L)
    val tempRoot = "tempRoot"

    val record1 = CameraUploadsRecord(
        mediaId = 1234L,
        fileName = "fileName",
        filePath = "filePath",
        timestamp = 0L,
        folderType = CameraUploadFolderType.Secondary,
        type = SyncRecordType.TYPE_PHOTO,
        uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
        originalFingerprint = "originalFingerprint",
        generatedFingerprint = null,
        tempFilePath = "tempFilePath",
    )

    @BeforeAll
    fun setUp() {
        underTest = UploadCameraUploadsRecordsUseCase(
            findNodeWithFingerprintInParentNodeUseCase = findNodeWithFingerprintInParentNodeUseCase,
            copyNodeUseCase = copyNodeUseCase,
            setCoordinatesUseCase = setCoordinatesUseCase,
            getNodeGPSCoordinatesUseCase = getNodeGPSCoordinatesUseCase,
            getFingerprintUseCase = getFingerprintUseCase,
            startUploadUseCase = startUploadUseCase,
            getGPSCoordinatesUseCase = getGPSCoordinatesUseCase,
            setOriginalFingerprintUseCase = setOriginalFingerprintUseCase,
            areLocationTagsEnabledUseCase = areLocationTagsEnabledUseCase,
            createTempFileAndRemoveCoordinatesUseCase = createTempFileAndRemoveCoordinatesUseCase,
            setCameraUploadsRecordUploadStatusUseCase = setCameraUploadsRecordUploadStatusUseCase,
            setCameraUploadsRecordGeneratedFingerprintUseCase = setCameraUploadsRecordGeneratedFingerprintUseCase,
            createImageOrVideoThumbnailUseCase = createImageOrVideoThumbnailUseCase,
            createImageOrVideoPreviewUseCase = createImageOrVideoPreviewUseCase,
            deleteThumbnailUseCase = deleteThumbnailUseCase,
            deletePreviewUseCase = deletePreviewUseCase,
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            findNodeWithFingerprintInParentNodeUseCase,
            copyNodeUseCase,
            setCoordinatesUseCase,
            getNodeGPSCoordinatesUseCase,
            getFingerprintUseCase,
            startUploadUseCase,
            getGPSCoordinatesUseCase,
            setOriginalFingerprintUseCase,
            areLocationTagsEnabledUseCase,
            createTempFileAndRemoveCoordinatesUseCase,
            setCameraUploadsRecordUploadStatusUseCase,
            setCameraUploadsRecordGeneratedFingerprintUseCase,
            createImageOrVideoThumbnailUseCase,
            createImageOrVideoPreviewUseCase,
            deleteThumbnailUseCase,
            deletePreviewUseCase,
            fileSystemRepository,
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class NoCopyOrUpload {

        @ParameterizedTest(name = "when is in parent folder is {0} and folder type is {1}")
        @MethodSource("provideParameters")
        fun `test that if node exists in rubbish bin or parent folder then the node is not uploaded or copied`(
            exists: Boolean?,
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val existingNodeId = mock<NodeId>()
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }

                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(exists, existingNodeId))

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(startUploadUseCase, never()).invoke(
                    localPath = record.filePath,
                    parentNodeId = uploadNodeId,
                    fileName = record.fileName,
                    modificationTime = record.timestamp / 1000,
                    appData = TransferType.CU_UPLOAD.name,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
                verify(copyNodeUseCase, never()).invoke(
                    nodeToCopy = existingNodeId,
                    newNodeParent = uploadNodeId,
                    newNodeName = record.fileName,
                )
            }

        @ParameterizedTest(name = "when is in parent folder is {0} and folder type is {1}")
        @MethodSource("provideParameters")
        fun `test that if node exists in rubbish bin or parent folder then the status is set to ALREADY_EXISTS`(
            exists: Boolean?,
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }

                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(exists, mock()))

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(setCameraUploadsRecordUploadStatusUseCase).invoke(
                    mediaId = record.mediaId,
                    timestamp = record.timestamp,
                    folderType = record.folderType,
                    uploadStatus = CameraUploadsRecordUploadStatus.ALREADY_EXISTS,
                )
            }

        private fun provideParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(null, CameraUploadFolderType.Primary),
            Arguments.of(null, CameraUploadFolderType.Secondary),
            Arguments.of(true, CameraUploadFolderType.Primary),
            Arguments.of(true, CameraUploadFolderType.Secondary),
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class Copy {

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if node exists in a folder other than parent folder then record is copied`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                val existingNodeId = mock<NodeId>()
                val newNodeId = mock<NodeId>()
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, existingNodeId))
                whenever(getNodeGPSCoordinatesUseCase(existingNodeId)).thenReturn(Pair(0.0, 0.0))
                whenever(copyNodeUseCase(existingNodeId, uploadNodeId, record.fileName))
                    .thenReturn(newNodeId)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(copyNodeUseCase).invoke(
                    nodeToCopy = existingNodeId,
                    newNodeParent = uploadNodeId,
                    newNodeName = record.fileName,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a node is copied then the coordinates are set to the new node after copy`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                val existingNodeId = mock<NodeId>()
                val newNodeId = mock<NodeId>()
                val (latitude, longitude) = Pair(0.0, 0.0)
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, existingNodeId))
                whenever(getNodeGPSCoordinatesUseCase(existingNodeId))
                    .thenReturn(Pair(latitude, longitude))
                whenever(copyNodeUseCase(existingNodeId, uploadNodeId, record.fileName))
                    .thenReturn(newNodeId)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(setCoordinatesUseCase).invoke(
                    nodeId = newNodeId,
                    latitude = latitude,
                    longitude = longitude,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if node is copied then the status is set to COPIED after copy`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            val record = record1.copy(folderType = cameraUploadFolderType)
            val cameraUploadsRecords = listOf(record)
            val uploadNodeId = when (cameraUploadFolderType) {
                CameraUploadFolderType.Primary -> primaryUploadNodeId
                CameraUploadFolderType.Secondary -> secondaryUploadNodeId
            }
            val existingNodeId = mock<NodeId>()
            val newNodeId = mock<NodeId>()
            val (latitude, longitude) = Pair(0.0, 0.0)
            whenever(
                findNodeWithFingerprintInParentNodeUseCase(
                    record.originalFingerprint,
                    record.generatedFingerprint,
                    uploadNodeId,
                )
            ).thenReturn(Pair(false, existingNodeId))
            whenever(getNodeGPSCoordinatesUseCase(existingNodeId))
                .thenReturn(Pair(latitude, longitude))
            whenever(copyNodeUseCase(existingNodeId, uploadNodeId, record.fileName))
                .thenReturn(newNodeId)

            underTest(
                cameraUploadsRecords,
                primaryUploadNodeId,
                secondaryUploadNodeId,
                tempRoot
            ).collect()

            verify(setCameraUploadsRecordUploadStatusUseCase).invoke(
                mediaId = record.mediaId,
                timestamp = record.timestamp,
                folderType = record.folderType,
                uploadStatus = CameraUploadsRecordUploadStatus.COPIED,
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if node is copied then an event ToCopy is emitted before copy and an event Copied is emitted after copy`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                val existingNodeId = mock<NodeId>()
                val newNodeId = mock<NodeId>()
                val (latitude, longitude) = Pair(0.0, 0.0)
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, existingNodeId))
                whenever(getNodeGPSCoordinatesUseCase(existingNodeId))
                    .thenReturn(Pair(latitude, longitude))
                whenever(copyNodeUseCase(existingNodeId, uploadNodeId, record.fileName))
                    .thenReturn(newNodeId)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).test {
                    assertThat(awaitItem()).isEqualTo(
                        CameraUploadsTransferProgress.ToCopy(
                            record,
                            existingNodeId
                        )
                    )
                    assertThat(awaitItem()).isEqualTo(
                        CameraUploadsTransferProgress.Copied(
                            record,
                            existingNodeId
                        )
                    )
                    awaitComplete()
                }
            }

        private fun provideParameters() = Stream.of(
            Arguments.of(CameraUploadFolderType.Primary),
            Arguments.of(CameraUploadFolderType.Secondary),
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class Upload {

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if does node exists in a cloud then record is uploaded`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(startUploadUseCase).invoke(
                    localPath = record.filePath,
                    parentNodeId = uploadNodeId,
                    fileName = record.fileName,
                    modificationTime = record.timestamp / 1000,
                    appData = TransferType.CU_UPLOAD.name,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the generated fingerprint exists then the generated fingerprint is set`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                val generatedFingerprint = "generatedFingerprint"
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
                whenever(
                    createTempFileAndRemoveCoordinatesUseCase(
                        tempRoot,
                        record.filePath,
                        record.tempFilePath,
                        record.timestamp
                    )
                ).thenReturn("tempFilePath")
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(false)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.tempFilePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(getFingerprintUseCase(record.tempFilePath))
                    .thenReturn(generatedFingerprint)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(setCameraUploadsRecordGeneratedFingerprintUseCase).invoke(
                    mediaId = record.mediaId,
                    timestamp = record.timestamp,
                    folderType = record.folderType,
                    generatedFingerprint = generatedFingerprint,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer starts then the status is set to STARTED`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(setCameraUploadsRecordUploadStatusUseCase).invoke(
                    mediaId = record.mediaId,
                    timestamp = record.timestamp,
                    folderType = record.folderType,
                    uploadStatus = CameraUploadsRecordUploadStatus.STARTED,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer starts then the status is set to UPLOADED`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(setCameraUploadsRecordUploadStatusUseCase).invoke(
                    mediaId = record.mediaId,
                    timestamp = record.timestamp,
                    folderType = record.folderType,
                    uploadStatus = CameraUploadsRecordUploadStatus.UPLOADED,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer starts then then an event ToUpload is emitted`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).test {
                    assertThat(awaitItem()).isEqualTo(
                        CameraUploadsTransferProgress.ToUpload(
                            record,
                            transferStartEvent
                        )
                    )
                    awaitItem()
                    awaitComplete()
                }
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer is in progress then then an event UploadInProgress is emitted`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferUpdateEvent = mock<TransferEvent.TransferUpdateEvent>()
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferUpdateEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).test {
                    awaitItem()
                    assertThat(awaitItem()).isEqualTo(
                        CameraUploadsTransferProgress.UploadInProgress(
                            record,
                            transferUpdateEvent,
                        )
                    )
                    awaitItem()
                    awaitComplete()
                }
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then then an event Uploaded is emitted`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).test {
                    awaitItem()
                    assertThat(awaitItem()).isEqualTo(
                        CameraUploadsTransferProgress.Uploaded(
                            record,
                            transferFinishedEvent,
                            NodeId(transferFinished.nodeHandle)
                        )
                    )
                    awaitComplete()
                }
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then the original fingerprint is set`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot,
                ).collect()

                verify(setOriginalFingerprintUseCase).invoke(
                    nodeId = NodeId(transferFinished.nodeHandle),
                    originalFingerprint = record.originalFingerprint,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then the gps coordinates are set`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot,
                ).collect()

                verify(setCoordinatesUseCase).invoke(
                    nodeId = NodeId(transferFinished.nodeHandle),
                    latitude = 0.0F.toDouble(),
                    longitude = 0.0F.toDouble(),
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then temporary file is deleted`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot,
                ).collect()

                verify(fileSystemRepository).deleteFile(File(record.tempFilePath))
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then preview and thumbnail are recreated`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

                val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
                val transferFinished = mock<Transfer> {
                    on { nodeHandle }.thenReturn(9876L)
                }
                val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { transfer }.thenReturn(transferFinished)
                }
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf(transferStartEvent, transferFinishedEvent))
                whenever(getGPSCoordinatesUseCase(record.filePath, false))
                    .thenReturn(Pair(0.0F, 0.0F))
                whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
                whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot,
                ).collect()

                verify(deleteThumbnailUseCase).invoke(transferFinished.nodeHandle)
                verify(createImageOrVideoThumbnailUseCase)
                    .invoke(transferFinished.nodeHandle, File(record.fileName))
                verify(deletePreviewUseCase).invoke(transferFinished.nodeHandle)
                verify(createImageOrVideoPreviewUseCase)
                    .invoke(transferFinished.nodeHandle, File(record.fileName))
            }

        private fun provideParameters() = Stream.of(
            Arguments.of(CameraUploadFolderType.Primary),
            Arguments.of(CameraUploadFolderType.Secondary),
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class TemporaryFileCreation {

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and location tags is disabled then a temporary file is created`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(false)

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(createTempFileAndRemoveCoordinatesUseCase).invoke(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    record.timestamp,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is of type VIIDEO and location tags is disabled then a temporary file is not created`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(
                    folderType = cameraUploadFolderType,
                    type = SyncRecordType.TYPE_VIDEO
                )
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
                whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
                whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
                whenever(
                    startUploadUseCase(
                        localPath = record.filePath,
                        parentNodeId = uploadNodeId,
                        fileName = record.fileName,
                        modificationTime = record.timestamp / 1000,
                        appData = TransferType.CU_UPLOAD.name,
                        isSourceTemporary = false,
                        shouldStartFirst = false,
                    )
                ).thenReturn(flowOf())

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(createTempFileAndRemoveCoordinatesUseCase, never()).invoke(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    record.timestamp,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a temporary file is created and fails with error FileNotFoundException, then the status is set to LOCAL_FILE_NOT_EXIST`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) {
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
                whenever(
                    createTempFileAndRemoveCoordinatesUseCase(
                        tempRoot,
                        record.filePath,
                        record.tempFilePath,
                        record.timestamp
                    )
                ).thenAnswer { throw FileNotFoundException() }

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(setCameraUploadsRecordUploadStatusUseCase).invoke(
                    mediaId = record.mediaId,
                    timestamp = record.timestamp,
                    folderType = record.folderType,
                    uploadStatus = CameraUploadsRecordUploadStatus.LOCAL_FILE_NOT_EXIST,
                )
            }
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a temporary file is created and fails with error other than FileNotFoundException, then the status is set to FAILED`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
                whenever(
                    createTempFileAndRemoveCoordinatesUseCase(
                        tempRoot,
                        record.filePath,
                        record.tempFilePath,
                        record.timestamp
                    )
                ).thenAnswer { throw Exception() }

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(setCameraUploadsRecordUploadStatusUseCase).invoke(
                    mediaId = record.mediaId,
                    timestamp = record.timestamp,
                    folderType = record.folderType,
                    uploadStatus = CameraUploadsRecordUploadStatus.FAILED,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a temporary file is created and fails with error NotEnoughStorageException, then it will retry 60 times`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
                whenever(
                    createTempFileAndRemoveCoordinatesUseCase(
                        tempRoot,
                        record.filePath,
                        record.tempFilePath,
                        record.timestamp
                    )
                ).thenAnswer { throw NotEnoughStorageException() }

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()


                verify(createTempFileAndRemoveCoordinatesUseCase, times(61)).invoke(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    record.timestamp,
                )
            }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a temporary file is created and fails with error, then the record will not be uploaded`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) =
            runTest {
                val record = record1.copy(folderType = cameraUploadFolderType)
                val cameraUploadsRecords = listOf(record)
                val uploadNodeId = when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        record.originalFingerprint,
                        record.generatedFingerprint,
                        uploadNodeId,
                    )
                ).thenReturn(Pair(false, null))
                whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
                whenever(
                    createTempFileAndRemoveCoordinatesUseCase(
                        tempRoot,
                        record.filePath,
                        record.tempFilePath,
                        record.timestamp
                    )
                ).thenAnswer { throw Exception() }

                underTest(
                    cameraUploadsRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                    tempRoot
                ).collect()

                verify(startUploadUseCase, never()).invoke(
                    localPath = record.tempFilePath,
                    parentNodeId = uploadNodeId,
                    fileName = record.fileName,
                    modificationTime = record.timestamp / 1000,
                    appData = TransferType.CU_UPLOAD.name,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            }

        private fun provideParameters() = Stream.of(
            Arguments.of(CameraUploadFolderType.Primary),
            Arguments.of(CameraUploadFolderType.Secondary),
        )
    }
}
