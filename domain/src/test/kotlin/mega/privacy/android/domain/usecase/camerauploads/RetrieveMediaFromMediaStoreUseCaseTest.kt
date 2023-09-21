package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.mapper.CameraUploadsRecordMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [RetrieveMediaFromMediaStoreUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetrieveMediaFromMediaStoreUseCaseTest {

    private lateinit var underTest: RetrieveMediaFromMediaStoreUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val cameraUploadsRecordMapper = mock<CameraUploadsRecordMapper>()

    @BeforeAll
    fun setUp() {
        underTest = RetrieveMediaFromMediaStoreUseCase(
            cameraUploadRepository = cameraUploadRepository,
            cameraUploadsRecordMapper = cameraUploadsRecordMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            cameraUploadsRecordMapper,
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
            val fileType = mock<SyncRecordType>()
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            val cameraUploadsMediaList1 = listOf<CameraUploadsMedia>(mock(), mock())
            val cameraUploadsMediaList2 = listOf<CameraUploadsMedia>(mock())
            whenever(cameraUploadRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadRepository.getMediaList(
                    mediaStoreFileType1,
                    selectionQuery
                )
            ).thenReturn(cameraUploadsMediaList1)
            whenever(
                cameraUploadRepository.getMediaList(
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
            val fileType = mock<SyncRecordType>()
            val tempRoot = "tempRoot"
            val selectionQuery = "selectionQuery"

            val cameraUploadsMediaList1 = listOf<CameraUploadsMedia>(mock(), mock())
            val cameraUploadsMediaList2 = listOf<CameraUploadsMedia>(mock())
            whenever(cameraUploadRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadRepository.getMediaList(
                    mediaStoreFileType1,
                    selectionQuery
                )
            ).thenReturn(cameraUploadsMediaList1)
            whenever(
                cameraUploadRepository.getMediaList(
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

            assertThat(underTest(parentPath, types, folderType, fileType, tempRoot))
                .isEqualTo(cameraUploadsRecordList1)
        }
}
