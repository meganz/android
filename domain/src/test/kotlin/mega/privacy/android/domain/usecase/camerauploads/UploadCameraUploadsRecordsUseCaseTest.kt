package mega.privacy.android.domain.usecase.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsTransferProgress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.CreateTempFileAndRemoveCoordinatesUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DeletePreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DeleteThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadUseCase
import mega.privacy.android.domain.usecase.video.CompressVideoUseCase
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileNotFoundException
import java.util.stream.Stream


@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadCameraUploadsRecordsUseCaseTest {
    private lateinit var underTest: UploadCameraUploadsRecordsUseCase

    private val copyNodeUseCase = mock<CopyNodeUseCase>()
    private val setCoordinatesUseCase = mock<SetCoordinatesUseCase>()
    private val getNodeGPSCoordinatesUseCase = mock<GetNodeGPSCoordinatesUseCase>()
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val startUploadUseCase = mock<StartUploadUseCase>()
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
    private val compressVideoUseCase = mock<CompressVideoUseCase>()
    private val getUploadVideoQualityUseCase = mock<GetUploadVideoQualityUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val addCompletedTransferUseCase = mock<AddCompletedTransferUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val monitorChargingStoppedState: MonitorChargingStoppedState = mock()
    private val isChargingUseCase: IsChargingUseCase = mock()
    private val isChargingRequiredForVideoCompressionUseCase: IsChargingRequiredForVideoCompressionUseCase =
        mock()

    private val primaryUploadNodeId = NodeId(1111L)
    private val secondaryUploadNodeId = NodeId(2222L)
    val tempRoot = "tempRoot"

    val record1 = CameraUploadsRecord(
        mediaId = 1234L,
        fileName = "fileName",
        filePath = "filePath",
        timestamp = 0L,
        folderType = CameraUploadFolderType.Secondary,
        type = CameraUploadsRecordType.TYPE_PHOTO,
        uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
        originalFingerprint = "originalFingerprint",
        generatedFingerprint = null,
        tempFilePath = "tempFilePath",
        latitude = 0.0,
        longitude = 0.0,
        generatedFileName = "generatedFileName"
    )

    private val existingNodeId = mock<NodeId>()
    private val transferFinishedNode = mock<TypedFileNode>()
    private var cameraUploadsRecords = emptyList<CameraUploadsRecord>()
    private var uploadNodeId = NodeId(0L)
    private var record = record1

    @BeforeAll
    fun setUp() {
        underTest = UploadCameraUploadsRecordsUseCase(
            copyNodeUseCase = copyNodeUseCase,
            setCoordinatesUseCase = setCoordinatesUseCase,
            getNodeGPSCoordinatesUseCase = getNodeGPSCoordinatesUseCase,
            getFingerprintUseCase = getFingerprintUseCase,
            startUploadUseCase = startUploadUseCase,
            setOriginalFingerprintUseCase = setOriginalFingerprintUseCase,
            areLocationTagsEnabledUseCase = areLocationTagsEnabledUseCase,
            createTempFileAndRemoveCoordinatesUseCase = createTempFileAndRemoveCoordinatesUseCase,
            setCameraUploadsRecordUploadStatusUseCase = setCameraUploadsRecordUploadStatusUseCase,
            setCameraUploadsRecordGeneratedFingerprintUseCase = setCameraUploadsRecordGeneratedFingerprintUseCase,
            createImageOrVideoThumbnailUseCase = createImageOrVideoThumbnailUseCase,
            createImageOrVideoPreviewUseCase = createImageOrVideoPreviewUseCase,
            deleteThumbnailUseCase = deleteThumbnailUseCase,
            deletePreviewUseCase = deletePreviewUseCase,
            compressVideoUseCase = compressVideoUseCase,
            getUploadVideoQualityUseCase = getUploadVideoQualityUseCase,
            fileSystemRepository = fileSystemRepository,
            addCompletedTransferUseCase = addCompletedTransferUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            monitorChargingStoppedState = monitorChargingStoppedState,
            isChargingUseCase = isChargingUseCase,
            isChargingRequiredForVideoCompressionUseCase = isChargingRequiredForVideoCompressionUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            copyNodeUseCase,
            setCoordinatesUseCase,
            getNodeGPSCoordinatesUseCase,
            getFingerprintUseCase,
            startUploadUseCase,
            setOriginalFingerprintUseCase,
            areLocationTagsEnabledUseCase,
            createTempFileAndRemoveCoordinatesUseCase,
            setCameraUploadsRecordUploadStatusUseCase,
            setCameraUploadsRecordGeneratedFingerprintUseCase,
            createImageOrVideoThumbnailUseCase,
            createImageOrVideoPreviewUseCase,
            deleteThumbnailUseCase,
            deletePreviewUseCase,
            compressVideoUseCase,
            getUploadVideoQualityUseCase,
            fileSystemRepository,
            addCompletedTransferUseCase,
            getNodeByIdUseCase,
        )
    }

    @BeforeEach
    fun setupMock(): Unit = runBlocking {
        whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
        whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(false)
    }

    private fun getUploadNodeId(cameraUploadFolderType: CameraUploadFolderType) =
        when (cameraUploadFolderType) {
            CameraUploadFolderType.Primary -> primaryUploadNodeId
            CameraUploadFolderType.Secondary -> secondaryUploadNodeId
        }

    private fun executeUnderTest() =
        underTest(
            cameraUploadsRecords,
            primaryUploadNodeId,
            secondaryUploadNodeId,
            tempRoot
        )

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class NoCopyOrUpload {

        private fun setInput(
            cameraUploadFolderType: CameraUploadFolderType,
            existsInTargetNode: Boolean?,
        ) {
            record = record1.copy(
                folderType = cameraUploadFolderType,
                existsInTargetNode = existsInTargetNode,
                existingNodeId = existingNodeId,
            )
            cameraUploadsRecords = listOf(record)
            uploadNodeId = getUploadNodeId(cameraUploadFolderType)
        }

        @ParameterizedTest(name = "when is in parent folder is {0} and folder type is {1}")
        @MethodSource("provideParameters")
        fun `test that if node exists in rubbish bin or parent folder then the node is not uploaded or copied`(
            exists: Boolean?,
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType, exists)

            executeUnderTest().collect()

            verify(startUploadUseCase, never()).invoke(
                localPath = record.filePath,
                parentNodeId = uploadNodeId,
                fileName = record.generatedFileName,
                modificationTime = record.timestamp / 1000,
                appData = TransferAppData.CameraUpload,
                isSourceTemporary = false,
                shouldStartFirst = false,
            )
            verify(copyNodeUseCase, never()).invoke(
                nodeToCopy = existingNodeId,
                newNodeParent = uploadNodeId,
                newNodeName = record.generatedFileName,
            )
        }

        @ParameterizedTest(name = "when is in parent folder is {0} and folder type is {1}")
        @MethodSource("provideParameters")
        fun `test that if node exists in rubbish bin or parent folder then the status is set to ALREADY_EXISTS`(
            exists: Boolean?,
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType, exists)

            executeUnderTest().collect()

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

        private val newNodeId = mock<NodeId>()

        private fun setInput(cameraUploadFolderType: CameraUploadFolderType) {
            record = record1.copy(
                folderType = cameraUploadFolderType,
                existsInTargetNode = false,
                existingNodeId = existingNodeId,
            )
            cameraUploadsRecords = listOf(record)
            uploadNodeId = getUploadNodeId(cameraUploadFolderType)
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if node exists in a folder other than parent folder then record is copied`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(getNodeGPSCoordinatesUseCase(existingNodeId)).thenReturn(Pair(0.0, 0.0))
            whenever(copyNodeUseCase(existingNodeId, uploadNodeId, record.generatedFileName))
                .thenReturn(newNodeId)

            executeUnderTest().collect()

            verify(copyNodeUseCase).invoke(
                nodeToCopy = existingNodeId,
                newNodeParent = uploadNodeId,
                newNodeName = record.generatedFileName,
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a node is copied then the coordinates are set to the new node after copy`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            val (latitude, longitude) = Pair(0.0, 0.0)
            whenever(getNodeGPSCoordinatesUseCase(existingNodeId))
                .thenReturn(Pair(latitude, longitude))
            whenever(copyNodeUseCase(existingNodeId, uploadNodeId, record.generatedFileName))
                .thenReturn(newNodeId)

            executeUnderTest().collect()

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
            setInput(cameraUploadFolderType)
            whenever(getNodeGPSCoordinatesUseCase(existingNodeId)).thenReturn(Pair(0.0, 0.0))
            whenever(copyNodeUseCase(existingNodeId, uploadNodeId, record.generatedFileName))
                .thenReturn(newNodeId)

            executeUnderTest().collect()

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
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(getNodeGPSCoordinatesUseCase(existingNodeId)).thenReturn(Pair(0.0, 0.0))
            whenever(copyNodeUseCase(existingNodeId, uploadNodeId, record.generatedFileName))
                .thenReturn(newNodeId)

            executeUnderTest().test {
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

        private val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
        private val transferFinished = mock<Transfer> {
            on { nodeHandle }.thenReturn(9876L)
        }
        private val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
            on { transfer }.thenReturn(transferFinished)
        }
        private var uploadEvents = flowOf(transferStartEvent, transferFinishedEvent)

        private fun setInput(cameraUploadFolderType: CameraUploadFolderType) {
            record = record1.copy(
                folderType = cameraUploadFolderType,
                existsInTargetNode = false,
                existingNodeId = null,
            )
            cameraUploadsRecords = listOf(record)
            uploadNodeId = getUploadNodeId(cameraUploadFolderType)
        }

        private fun mockStartUploadUseCase(
            filePath: String = record.filePath,
            events: Flow<TransferEvent> = uploadEvents,
        ) {
            whenever(
                startUploadUseCase(
                    localPath = filePath,
                    parentNodeId = uploadNodeId,
                    fileName = record.generatedFileName,
                    modificationTime = record.timestamp / 1000,
                    appData = TransferAppData.CameraUpload,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            ).thenReturn(events)
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if does node exists in a cloud then record is uploaded`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase(record.filePath)
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

            executeUnderTest().collect()

            verify(startUploadUseCase).invoke(
                localPath = record.filePath,
                parentNodeId = uploadNodeId,
                fileName = record.generatedFileName,
                modificationTime = record.timestamp / 1000,
                appData = TransferAppData.CameraUpload,
                isSourceTemporary = false,
                shouldStartFirst = false,
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the generated fingerprint exists then the generated fingerprint is set`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            val generatedFingerprint = "generatedFingerprint"
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
            mockStartUploadUseCase(record.tempFilePath)
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(getFingerprintUseCase(record.tempFilePath))
                .thenReturn(generatedFingerprint)

            executeUnderTest().collect()

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
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

            executeUnderTest().collect()

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
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(getNodeByIdUseCase(NodeId(transferFinished.nodeHandle)))
                .thenReturn(transferFinishedNode)

            executeUnderTest().collect()

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
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

            executeUnderTest().test {
                assertThat(awaitItem()).isEqualTo(
                    CameraUploadsTransferProgress.ToUpload(
                        record,
                        transferStartEvent
                    )
                )
                cancelAndConsumeRemainingEvents()
            }
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer is in progress then then an event TransferUpdate is emitted`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
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
            mockStartUploadUseCase(
                events = flowOf(transferStartEvent, transferUpdateEvent, transferFinishedEvent)
            )
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

            executeUnderTest().test {
                awaitItem()
                assertThat(awaitItem()).isEqualTo(
                    CameraUploadsTransferProgress.UploadInProgress.TransferUpdate(
                        record,
                        transferUpdateEvent,
                    )
                )
                cancelAndConsumeRemainingEvents()
            }
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer has a temporary error then then an event TransferTemporaryError is emitted`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)

            val transferStartEvent = mock<TransferEvent.TransferStartEvent>()
            val transferFinished = mock<Transfer> {
                on { nodeHandle }.thenReturn(9876L)
            }
            val transferTemporaryErrorEvent = mock<TransferEvent.TransferTemporaryErrorEvent>()
            val transferFinishedEvent = mock<TransferEvent.TransferFinishEvent> {
                on { transfer }.thenReturn(transferFinished)
            }
            mockStartUploadUseCase(
                events = flowOf(
                    transferStartEvent,
                    transferTemporaryErrorEvent,
                    transferFinishedEvent
                )
            )
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

            executeUnderTest().test {
                awaitItem()
                assertThat(awaitItem()).isEqualTo(
                    CameraUploadsTransferProgress.UploadInProgress.TransferTemporaryError(
                        record,
                        transferTemporaryErrorEvent,
                    )
                )
                cancelAndConsumeRemainingEvents()
            }
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then then an event Uploaded is emitted`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(fileSystemRepository.deleteFile(File(record.tempFilePath))).thenReturn(true)
            executeUnderTest().test {
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
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(getNodeByIdUseCase(NodeId(transferFinished.nodeHandle)))
                .thenReturn(transferFinishedNode)

            executeUnderTest().collect()

            verify(setOriginalFingerprintUseCase).invoke(
                nodeId = NodeId(transferFinished.nodeHandle),
                originalFingerprint = record.originalFingerprint,
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then the gps coordinates are set`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(getNodeByIdUseCase(NodeId(transferFinished.nodeHandle)))
                .thenReturn(transferFinishedNode)

            executeUnderTest().collect()

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
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)

            executeUnderTest().collect()

            verify(fileSystemRepository).deleteFile(File(record.tempFilePath))
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then preview and thumbnail are recreated`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(getNodeByIdUseCase(NodeId(transferFinished.nodeHandle)))
                .thenReturn(transferFinishedNode)

            executeUnderTest().collect()

            verify(deleteThumbnailUseCase).invoke(transferFinished.nodeHandle)
            verify(createImageOrVideoThumbnailUseCase)
                .invoke(transferFinished.nodeHandle, File(record.filePath))
            verify(deletePreviewUseCase).invoke(transferFinished.nodeHandle)
            verify(createImageOrVideoPreviewUseCase)
                .invoke(transferFinished.nodeHandle, File(record.filePath))
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and the transfer finished then the completed transfer is added`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(getNodeByIdUseCase(NodeId(transferFinished.nodeHandle)))
                .thenReturn(transferFinishedNode)

            executeUnderTest().collect()

            verify(addCompletedTransferUseCase).invoke(
                transferFinishedEvent.transfer,
                transferFinishedEvent.error
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that no post transfer operations are done if node is not retrieved in the cloud after a transfer finish`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()
            whenever(deleteThumbnailUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(deletePreviewUseCase(transferFinished.nodeHandle)).thenReturn(true)
            whenever(getNodeByIdUseCase(NodeId(transferFinished.nodeHandle)))
                .thenReturn(null)

            executeUnderTest().collect()

            verify(setOriginalFingerprintUseCase, never()).invoke(
                nodeId = NodeId(transferFinished.nodeHandle),
                originalFingerprint = record.originalFingerprint,
            )
            verify(setCoordinatesUseCase, never()).invoke(
                nodeId = NodeId(transferFinished.nodeHandle),
                latitude = 0.0F.toDouble(),
                longitude = 0.0F.toDouble(),
            )
            verify(deleteThumbnailUseCase, never()).invoke(transferFinished.nodeHandle)
            verify(createImageOrVideoThumbnailUseCase, never())
                .invoke(transferFinished.nodeHandle, File(record.filePath))
            verify(deletePreviewUseCase, never()).invoke(transferFinished.nodeHandle)
            verify(createImageOrVideoPreviewUseCase, never())
                .invoke(transferFinished.nodeHandle, File(record.filePath))
            verify(setCameraUploadsRecordUploadStatusUseCase, never()).invoke(
                mediaId = record.mediaId,
                timestamp = record.timestamp,
                folderType = record.folderType,
                uploadStatus = CameraUploadsRecordUploadStatus.UPLOADED,
            )
            verify(addCompletedTransferUseCase, never()).invoke(
                transferFinishedEvent.transfer,
                transferFinishedEvent.error
            )
        }

        private fun provideParameters() = Stream.of(
            Arguments.of(CameraUploadFolderType.Primary),
            Arguments.of(CameraUploadFolderType.Secondary),
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class TemporaryFileCreation {

        private fun setInput(
            cameraUploadFolderType: CameraUploadFolderType,
            type: CameraUploadsRecordType = CameraUploadsRecordType.TYPE_PHOTO,
        ) {
            record = record1.copy(
                folderType = cameraUploadFolderType,
                type = type,
                existsInTargetNode = false,
                existingNodeId = null,
            )
            cameraUploadsRecords = listOf(record)
            uploadNodeId = getUploadNodeId(cameraUploadFolderType)
        }

        private fun mockStartUploadUseCase(
            filePath: String = record.filePath,
        ) {
            whenever(
                startUploadUseCase(
                    localPath = filePath,
                    parentNodeId = uploadNodeId,
                    fileName = record.generatedFileName,
                    modificationTime = record.timestamp / 1000,
                    appData = TransferAppData.CameraUpload,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            ).thenReturn(flowOf())
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is uploaded and location tags is disabled then a temporary file is created`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(false)

            executeUnderTest().collect()

            verify(createTempFileAndRemoveCoordinatesUseCase).invoke(
                tempRoot,
                record.filePath,
                record.tempFilePath,
                record.timestamp,
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if record is of type VIDEO and location tags is disabled then a temporary file is not created`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType, CameraUploadsRecordType.TYPE_VIDEO)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()

            executeUnderTest().collect()

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
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
            whenever(
                createTempFileAndRemoveCoordinatesUseCase(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    record.timestamp
                )
            ).thenAnswer { throw FileNotFoundException() }

            executeUnderTest().collect()

            verify(setCameraUploadsRecordUploadStatusUseCase).invoke(
                mediaId = record.mediaId,
                timestamp = record.timestamp,
                folderType = record.folderType,
                uploadStatus = CameraUploadsRecordUploadStatus.LOCAL_FILE_NOT_EXIST,
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a temporary file is created and fails with error other than FileNotFoundException, then the status is set to FAILED`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
            whenever(
                createTempFileAndRemoveCoordinatesUseCase(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    record.timestamp
                )
            ).thenAnswer { throw Exception() }

            executeUnderTest().collect()

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
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
            whenever(
                createTempFileAndRemoveCoordinatesUseCase(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    record.timestamp
                )
            ).thenAnswer { throw NotEnoughStorageException() }

            executeUnderTest().collect()


            verify(createTempFileAndRemoveCoordinatesUseCase, times(60)).invoke(
                tempRoot,
                record.filePath,
                record.tempFilePath,
                record.timestamp,
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a temporary file is created and fails with error NotEnoughStorageException, it will emit a NotEnoughStorageException error after 60 attempt`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            val expected = NotEnoughStorageException()
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
            whenever(
                createTempFileAndRemoveCoordinatesUseCase(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    record.timestamp
                )
            ).thenAnswer { throw expected }

            val event = executeUnderTest().firstOrNull()
            assertThat(event).isEqualTo(
                CameraUploadsTransferProgress.Error(
                    record = record,
                    error = expected
                )
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that if a temporary file is created and fails with error, then the record will not be uploaded`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(false)
            whenever(
                createTempFileAndRemoveCoordinatesUseCase(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    record.timestamp
                )
            ).thenAnswer { throw Exception() }

            executeUnderTest().collect()

            verify(startUploadUseCase, never()).invoke(
                localPath = record.tempFilePath,
                parentNodeId = uploadNodeId,
                fileName = record.generatedFileName,
                modificationTime = record.timestamp / 1000,
                appData = TransferAppData.CameraUpload,
                isSourceTemporary = false,
                shouldStartFirst = false,
            )
        }

        private fun provideParameters() = Stream.of(
            Arguments.of(CameraUploadFolderType.Primary),
            Arguments.of(CameraUploadFolderType.Secondary),
        )
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class VideoCompression {

        private fun setInput(
            cameraUploadFolderType: CameraUploadFolderType,
            type: CameraUploadsRecordType = CameraUploadsRecordType.TYPE_PHOTO,
        ) {
            record = record1.copy(
                folderType = cameraUploadFolderType,
                type = type,
                existsInTargetNode = false,
                existingNodeId = null,
            )
            cameraUploadsRecords = listOf(record)
            uploadNodeId = getUploadNodeId(cameraUploadFolderType)
        }

        private fun mockStartUploadUseCase(
            filePath: String = record.filePath,
        ) {
            whenever(
                startUploadUseCase(
                    localPath = filePath,
                    parentNodeId = uploadNodeId,
                    fileName = record.generatedFileName,
                    modificationTime = record.timestamp / 1000,
                    appData = TransferAppData.CameraUpload,
                    isSourceTemporary = false,
                    shouldStartFirst = false,
                )
            ).thenReturn(flowOf(TransferEvent.TransferFinishEvent(mock(), null)))
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that when device is not charging and the charging is required for compression,then no compression is executed`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            val quality = VideoQuality.HIGH
            whenever(getUploadVideoQualityUseCase()).thenReturn(quality)
            setInput(cameraUploadFolderType, type = CameraUploadsRecordType.TYPE_VIDEO)
            whenever(isChargingUseCase()).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(true)
            executeUnderTest().collect()
            verifyNoInteractions(compressVideoUseCase, startUploadUseCase)
        }

        @ParameterizedTest(name = "when folder type is {0} and compression events are {1}")
        @MethodSource("provideParameters")
        fun `test that when charging is disconnected, compression cancelled and file is not uploaded`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            val compressionEvents: List<VideoCompressionState> =
                generateSequence(0f) { it + 0.5f }
                    .takeWhile { it <= 1f }.map {
                        VideoCompressionState.Progress(it, 0, 1, "path")
                    }.toList()
            setInput(cameraUploadFolderType, type = CameraUploadsRecordType.TYPE_VIDEO)
            val quality = VideoQuality.HIGH
            whenever(getUploadVideoQualityUseCase()).thenReturn(quality)
            whenever(isChargingUseCase()).thenReturn(true)
            whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(true)
            whenever(monitorChargingStoppedState()).thenReturn(flowOf(true, false))
            whenever(compressVideoUseCase(tempRoot, record.filePath, record.tempFilePath, quality))
                .thenReturn(flow {
                    emit(compressionEvents.first())
                })
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            executeUnderTest().test {
                val cancelItem = awaitItem()
                assertThat(cancelItem).isInstanceOf(CameraUploadsTransferProgress.Compressing.Cancel::class.java)
                verify(compressVideoUseCase).invoke(
                    tempRoot,
                    record.filePath,
                    record.tempFilePath,
                    quality
                )
                verifyNoInteractions(getFingerprintUseCase)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that when the file uploaded is not a video and the compression option is not original, then no compress is executed`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType)
            val quality = VideoQuality.MEDIUM
            whenever(isChargingUseCase()).thenReturn(false)
            whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(false)
            whenever(monitorChargingStoppedState()).thenReturn(emptyFlow())
            whenever(getUploadVideoQualityUseCase()).thenReturn(quality)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase()

            executeUnderTest().collect()

            verify(compressVideoUseCase, never()).invoke(
                tempRoot,
                record.filePath,
                record.tempFilePath,
                quality
            )
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that when the file uploaded is a video and the compression option is not original, then compress the video`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType, CameraUploadsRecordType.TYPE_VIDEO)
            val quality = VideoQuality.MEDIUM
            whenever(getUploadVideoQualityUseCase()).thenReturn(quality)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase(record.tempFilePath)
            whenever(compressVideoUseCase(tempRoot, record.filePath, record.tempFilePath, quality))
                .thenReturn(flowOf(VideoCompressionState.Finished))

            executeUnderTest().collect()

            verify(compressVideoUseCase).invoke(
                tempRoot,
                record.filePath,
                record.tempFilePath,
                quality
            )
        }

        @ParameterizedTest(name = "when folder type is {0} and compression events are {1}")
        @MethodSource("provideCompressionEvents")
        fun `test that when video is under compression, then Compressing events are emitted`(
            cameraUploadFolderType: CameraUploadFolderType,
            compressionEvents: List<VideoCompressionState>,
        ) = runTest {
            setInput(cameraUploadFolderType, CameraUploadsRecordType.TYPE_VIDEO)
            val quality = VideoQuality.MEDIUM
            whenever(isChargingUseCase()).thenReturn(false)
            whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(false)
            whenever(monitorChargingStoppedState()).thenReturn(emptyFlow())
            whenever(getUploadVideoQualityUseCase()).thenReturn(quality)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase(record.tempFilePath)
            whenever(compressVideoUseCase(tempRoot, record.filePath, record.tempFilePath, quality))
                .thenReturn(compressionEvents.asFlow())

            executeUnderTest().test {
                compressionEvents.filterNot {
                    it is VideoCompressionState.Finished
                            || it is VideoCompressionState.Successful
                            || it is VideoCompressionState.Failed
                }
                    .forEach {
                        assertThat(awaitItem())
                            .isEqualTo(
                                when (it) {
                                    is VideoCompressionState.Progress -> {
                                        CameraUploadsTransferProgress.Compressing.Progress(
                                            record = record,
                                            progress = it.progress,
                                        )
                                    }

                                    is VideoCompressionState.Successful -> {
                                        CameraUploadsTransferProgress.Compressing.Successful(
                                            record = record,
                                        )
                                    }

                                    is VideoCompressionState.InsufficientStorage -> {
                                        CameraUploadsTransferProgress.Compressing.InsufficientStorage(
                                            record = record,
                                        )
                                    }

                                    else -> {}
                                }
                            )
                    }
                cancelAndConsumeRemainingEvents()
            }
        }

        @ParameterizedTest(name = "when folder type is {0}")
        @MethodSource("provideParameters")
        fun `test that when video compression failed, then upload the original file`(
            cameraUploadFolderType: CameraUploadFolderType,
        ) = runTest {
            setInput(cameraUploadFolderType, CameraUploadsRecordType.TYPE_VIDEO)
            val quality = VideoQuality.MEDIUM
            whenever(isChargingUseCase()).thenReturn(false)
            whenever(isChargingRequiredForVideoCompressionUseCase()).thenReturn(false)
            whenever(monitorChargingStoppedState()).thenReturn(emptyFlow())
            whenever(getUploadVideoQualityUseCase()).thenReturn(quality)
            whenever(areLocationTagsEnabledUseCase()).thenReturn(true)
            whenever(fileSystemRepository.doesFileExist(record.tempFilePath)).thenReturn(false)
            whenever(fileSystemRepository.doesFileExist(record.filePath)).thenReturn(true)
            mockStartUploadUseCase(record.filePath)
            whenever(compressVideoUseCase(tempRoot, record.filePath, record.tempFilePath, quality))
                .thenReturn(
                    flowOf(VideoCompressionState.Failed(null), VideoCompressionState.Finished)
                )

            executeUnderTest().collect()

            verify(startUploadUseCase).invoke(
                localPath = record.filePath,
                parentNodeId = uploadNodeId,
                fileName = record.generatedFileName,
                modificationTime = record.timestamp / 1000,
                appData = TransferAppData.CameraUpload,
                isSourceTemporary = false,
                shouldStartFirst = false,
            )
        }

        private fun provideParameters() = Stream.of(
            Arguments.of(CameraUploadFolderType.Primary),
            Arguments.of(CameraUploadFolderType.Secondary),
        )

        private fun provideCompressionEvents() = Stream.of(
            Arguments.of(
                CameraUploadFolderType.Primary,
                listOf(
                    VideoCompressionState.Progress(0.5f, 0, 1, "path"),
                    VideoCompressionState.Finished,
                ),
            ),
            Arguments.of(
                CameraUploadFolderType.Secondary,
                listOf(
                    VideoCompressionState.Progress(0.5f, 0, 1, "path"),
                    VideoCompressionState.Finished,
                ),
            ),
            Arguments.of(
                CameraUploadFolderType.Primary,
                listOf(
                    VideoCompressionState.Failed(null),
                    VideoCompressionState.Finished,
                ),
            ),
            Arguments.of(
                CameraUploadFolderType.Secondary,
                listOf(
                    VideoCompressionState.Failed(null),
                    VideoCompressionState.Finished,
                ),
            ),
            Arguments.of(
                CameraUploadFolderType.Primary,
                listOf(
                    VideoCompressionState.InsufficientStorage,
                    VideoCompressionState.Finished,
                ),
            ),
            Arguments.of(
                CameraUploadFolderType.Secondary,
                listOf(
                    VideoCompressionState.InsufficientStorage,
                    VideoCompressionState.Finished,
                ),
            ),
        )
    }
}
