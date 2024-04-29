package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsRecordModelMapperTest {
    private lateinit var underTest: CameraUploadsRecordModelMapper

    @BeforeAll
    fun setUp() {
        underTest = CameraUploadsRecordModelMapper()
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val entity = CameraUploadsRecordEntity(
            mediaId = 1234L,
            fileName = "fileName.jpg",
            filePath = "filePath",
            timestamp = 5678L,
            folderType = CameraUploadFolderType.Primary,
            fileType = CameraUploadsRecordType.TYPE_PHOTO,
            uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
            originalFingerprint = "originalFingerprint",
            generatedFingerprint = null,
            tempFilePath = "tempFilePath"
        )

        val expected = CameraUploadsRecord(
            mediaId = 1234L,
            fileName = "fileName.jpg",
            filePath = "filePath",
            timestamp = 5678L,
            folderType = CameraUploadFolderType.Primary,
            type = CameraUploadsRecordType.TYPE_PHOTO,
            uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
            originalFingerprint = "originalFingerprint",
            generatedFingerprint = null,
            tempFilePath = "tempFilePath"
        )

        val actual = underTest(entity)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that when the coroutine is cancelled mapper returns cancellation exception`() {
        val testScope = TestScope()
        testScope.launch {
            cancel()
            assertThrows<CancellationException> {
                underTest(mock())
            }
        }
    }
}
