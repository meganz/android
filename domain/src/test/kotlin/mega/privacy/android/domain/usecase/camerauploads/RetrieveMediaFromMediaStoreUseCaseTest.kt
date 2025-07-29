package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.camerauploads.mapper.CameraUploadsRecordMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [RetrieveMediaFromMediaStoreUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetrieveMediaFromMediaStoreUseCaseTest {

    private lateinit var underTest: RetrieveMediaFromMediaStoreUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val cameraUploadsRecordMapper = mock<CameraUploadsRecordMapper>()
    private val getPendingCameraUploadsRecordsUseCase =
        mock<GetPendingCameraUploadsRecordsUseCase>()
    private val setCameraUploadsRecordUploadStatusUseCase =
        mock<SetCameraUploadsRecordUploadStatusUseCase>()

    @BeforeEach
    fun setUp() {
        runBlocking { whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(emptyList()) }
        underTest = RetrieveMediaFromMediaStoreUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            cameraUploadsRecordMapper = cameraUploadsRecordMapper,
            getPendingCameraUploadsRecordsUseCase = getPendingCameraUploadsRecordsUseCase,
            setCameraUploadsRecordUploadStatusUseCase = setCameraUploadsRecordUploadStatusUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadsRepository,
            cameraUploadsRecordMapper,
            getPendingCameraUploadsRecordsUseCase,
            setCameraUploadsRecordUploadStatusUseCase
        )
    }

    @Test
    fun `test that invoking the use case returns a combination of list retrieved from the Media Store for each MediaStoreFileType`() =
        runTest {
            val parentPath = ""
            val mediaStoreFileType1 = mock<MediaStoreFileType>()
            val mediaStoreFileType2 = mock<MediaStoreFileType>()
            val types = listOf(mediaStoreFileType1, mediaStoreFileType2)
            val folderType = mock<CameraUploadFolderType>()
            val fileType = mock<CameraUploadsRecordType>()
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            val cameraUploadsMediaList1 = listOf<CameraUploadsMedia>(mock(), mock())
            val cameraUploadsMediaList2 = listOf<CameraUploadsMedia>(mock())
            whenever(cameraUploadsRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadsRepository.getMediaList(
                    mediaStoreFileType1,
                    selectionQuery
                )
            ).thenReturn(cameraUploadsMediaList1)
            whenever(
                cameraUploadsRepository.getMediaList(
                    mediaStoreFileType2,
                    selectionQuery
                )
            ).thenReturn(cameraUploadsMediaList2)

            val cameraUploadsRecordList1 = listOf<CameraUploadsRecord>(mock(), mock())
            val cameraUploadsRecordList2 = listOf<CameraUploadsRecord>(mock())
            cameraUploadsMediaList1.forEachIndexed { index, media ->
                whenever(
                    cameraUploadsRecordMapper(
                        media,
                        folderType,
                        fileType,
                        tempRoot
                    )
                ).thenReturn(
                    cameraUploadsRecordList1[index]
                )
            }

            cameraUploadsMediaList2.forEachIndexed { index, media ->
                whenever(
                    cameraUploadsRecordMapper(
                        media,
                        folderType,
                        fileType,
                        tempRoot
                    )
                ).thenReturn(
                    cameraUploadsRecordList2[index]
                )
            }

            whenever(cameraUploadsRepository.getAllCameraUploadsRecords()).thenReturn(emptyList())

            val expected = cameraUploadsRecordList1 + cameraUploadsRecordList2

            assertThat(underTest(parentPath, types, folderType, fileType, tempRoot))
                .isEqualTo(expected)
        }

    @Test
    fun `test that if an error is thrown when mapping, the record is omitted in the result returned`() =
        runTest {
            val parentPath = ""
            val mediaStoreFileType1 = mock<MediaStoreFileType>()
            val mediaStoreFileType2 = mock<MediaStoreFileType>()
            val types = listOf(mediaStoreFileType1, mediaStoreFileType2)
            val folderType = mock<CameraUploadFolderType>()
            val fileType = mock<CameraUploadsRecordType>()
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            val cameraUploadsMediaList1 = listOf<CameraUploadsMedia>(mock(), mock())
            val cameraUploadsMediaList2 = listOf<CameraUploadsMedia>(mock())
            whenever(cameraUploadsRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadsRepository.getMediaList(
                    mediaStoreFileType1,
                    selectionQuery
                )
            ).thenReturn(cameraUploadsMediaList1)
            whenever(
                cameraUploadsRepository.getMediaList(
                    mediaStoreFileType2,
                    selectionQuery
                )
            ).thenReturn(cameraUploadsMediaList2)

            val cameraUploadsRecordList1 = listOf<CameraUploadsRecord>(mock(), mock())
            cameraUploadsMediaList1.forEachIndexed { index, media ->
                whenever(
                    cameraUploadsRecordMapper(
                        media,
                        folderType,
                        fileType,
                        tempRoot
                    )
                ).thenReturn(
                    cameraUploadsRecordList1[index]
                )
            }

            cameraUploadsMediaList2.forEachIndexed { _, media ->
                whenever(
                    cameraUploadsRecordMapper(
                        media,
                        folderType,
                        fileType,
                        tempRoot
                    )
                ).thenThrow(
                    RuntimeException("error")
                )
            }

            whenever(cameraUploadsRepository.getAllCameraUploadsRecords()).thenReturn(emptyList())

            assertThat(underTest(parentPath, types, folderType, fileType, tempRoot))
                .isEqualTo(cameraUploadsRecordList1)
        }

    @Test
    fun `test that the record is filtered out if the cameraUploadsMedia already exists in the database`() =
        runTest {
            val parentPath = ""
            val mediaStoreFileType1 = mock<MediaStoreFileType>()
            val types = listOf(mediaStoreFileType1)
            val folderType = mock<CameraUploadFolderType>()
            val fileType = mock<CameraUploadsRecordType>()
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            val media1 = mock<CameraUploadsMedia> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
            }

            val cameraUploadsRecord1 = mock<CameraUploadsRecord> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
                on { this.folderType }.thenReturn(CameraUploadFolderType.Primary)
            }

            val cameraUploadsMediaList1 = listOf(media1, mock())
            whenever(cameraUploadsRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadsRepository.getMediaList(mediaStoreFileType1, selectionQuery)
            ).thenReturn(cameraUploadsMediaList1)

            val cameraUploadsRecordList1 = listOf<CameraUploadsRecord>(mock(), mock())
            cameraUploadsMediaList1.forEachIndexed { index, media ->
                whenever(
                    cameraUploadsRecordMapper(media, folderType, fileType, tempRoot)
                ).thenReturn(
                    cameraUploadsRecordList1[index]
                )
            }

            whenever(cameraUploadsRepository.getAllCameraUploadsRecords())
                .thenReturn(listOf(cameraUploadsRecord1))


            val expected =
                cameraUploadsRecordList1.filterNot { it.mediaId == 1111L && it.timestamp == 1234L }

            assertThat(underTest(parentPath, types, folderType, fileType, tempRoot))
                .isEqualTo(expected)
        }

    @Test
    fun `test that getPendingCameraUploadsRecordsUseCase is not invoked when media list is null`() =
        runTest {
            val parentPath = ""
            val mediaStoreFileType1 = mock<MediaStoreFileType>()
            val types = listOf(mediaStoreFileType1)
            val folderType = CameraUploadFolderType.Primary
            val fileType = CameraUploadsRecordType.TYPE_VIDEO
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            whenever(cameraUploadsRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadsRepository.getMediaList(mediaStoreFileType1, selectionQuery)
            ).thenReturn(emptyList())

            val cameraUploadsRecord1 = mock<CameraUploadsRecord> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
                on { this.folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            whenever(cameraUploadsRepository.getAllCameraUploadsRecords())
                .thenReturn(listOf(cameraUploadsRecord1))

            underTest(parentPath, types, folderType, fileType, tempRoot)
            verifyNoInteractions(getPendingCameraUploadsRecordsUseCase)
        }

    @Test
    fun `test that setCameraUploadsRecordUploadStatusUseCase is invoked as expected when there is no existed records`() =
        runTest {
            val parentPath = ""
            val mediaStoreFileType1 = mock<MediaStoreFileType>()
            val types = listOf(mediaStoreFileType1)
            val folderType = CameraUploadFolderType.Primary
            val fileType = CameraUploadsRecordType.TYPE_VIDEO
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            val media1 = mock<CameraUploadsMedia> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
            }
            val noExistRecord = mock<CameraUploadsRecord> {
                on { mediaId }.thenReturn(55555L)
                on { timestamp }.thenReturn(1234L)
                on { this.folderType }.thenReturn(folderType)
                on { this.type }.thenReturn(fileType)
            }

            val cameraUploadsMediaList1 = listOf(media1, mock())
            whenever(cameraUploadsRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadsRepository.getMediaList(mediaStoreFileType1, selectionQuery)
            ).thenReturn(cameraUploadsMediaList1)

            val cameraUploadsRecord1 = mock<CameraUploadsRecord> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
                on { this.folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            whenever(cameraUploadsRepository.getAllCameraUploadsRecords())
                .thenReturn(listOf(cameraUploadsRecord1))

            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(listOf(noExistRecord))

            underTest(parentPath, types, folderType, fileType, tempRoot)

            verify(setCameraUploadsRecordUploadStatusUseCase).invoke(
                mediaId = noExistRecord.mediaId,
                timestamp = noExistRecord.timestamp,
                folderType = noExistRecord.folderType,
                uploadStatus = CameraUploadsRecordUploadStatus.LOCAL_FILE_NOT_EXIST
            )
        }

    @Test
    fun `test that setCameraUploadsRecordUploadStatusUseCase is not invoked when pending records not includes correct folderType items`() =
        runTest {
            val parentPath = ""
            val mediaStoreFileType1 = mock<MediaStoreFileType>()
            val types = listOf(mediaStoreFileType1)
            val folderType = CameraUploadFolderType.Primary
            val fileType = CameraUploadsRecordType.TYPE_VIDEO
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            val media1 = mock<CameraUploadsMedia> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
            }
            val noExistRecord = mock<CameraUploadsRecord> {
                on { mediaId }.thenReturn(55555L)
                on { timestamp }.thenReturn(1234L)
                on { this.folderType }.thenReturn(CameraUploadFolderType.Secondary)
                on { this.type }.thenReturn(fileType)
            }

            val cameraUploadsMediaList1 = listOf(media1, mock())
            whenever(cameraUploadsRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadsRepository.getMediaList(mediaStoreFileType1, selectionQuery)
            ).thenReturn(cameraUploadsMediaList1)

            val cameraUploadsRecord1 = mock<CameraUploadsRecord> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
                on { this.folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            whenever(cameraUploadsRepository.getAllCameraUploadsRecords())
                .thenReturn(listOf(cameraUploadsRecord1))

            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(listOf(noExistRecord))

            underTest(parentPath, types, folderType, fileType, tempRoot)

            verifyNoInteractions(setCameraUploadsRecordUploadStatusUseCase)
        }

    @Test
    fun `test that setCameraUploadsRecordUploadStatusUseCase is not invoked when pending records not includes correct fileType items`() =
        runTest {
            val parentPath = ""
            val mediaStoreFileType1 = mock<MediaStoreFileType>()
            val types = listOf(mediaStoreFileType1)
            val folderType = CameraUploadFolderType.Primary
            val fileType = CameraUploadsRecordType.TYPE_VIDEO
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            val media1 = mock<CameraUploadsMedia> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
            }
            val noExistRecord = mock<CameraUploadsRecord> {
                on { mediaId }.thenReturn(55555L)
                on { timestamp }.thenReturn(1234L)
                on { this.folderType }.thenReturn(folderType)
                on { this.type }.thenReturn(CameraUploadsRecordType.TYPE_PHOTO)
            }

            val cameraUploadsMediaList1 = listOf(media1, mock())
            whenever(cameraUploadsRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadsRepository.getMediaList(mediaStoreFileType1, selectionQuery)
            ).thenReturn(cameraUploadsMediaList1)

            val cameraUploadsRecord1 = mock<CameraUploadsRecord> {
                on { mediaId }.thenReturn(1111L)
                on { timestamp }.thenReturn(1234L)
                on { this.folderType }.thenReturn(CameraUploadFolderType.Primary)
            }
            whenever(cameraUploadsRepository.getAllCameraUploadsRecords())
                .thenReturn(listOf(cameraUploadsRecord1))

            whenever(getPendingCameraUploadsRecordsUseCase()).thenReturn(listOf(noExistRecord))

            underTest(parentPath, types, folderType, fileType, tempRoot)

            verifyNoInteractions(setCameraUploadsRecordUploadStatusUseCase)
        }
}
