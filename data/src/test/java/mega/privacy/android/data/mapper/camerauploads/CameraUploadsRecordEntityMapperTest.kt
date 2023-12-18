package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsRecordEntityMapperTest {
    private lateinit var underTest: CameraUploadsRecordEntityMapper

    private val encryptData = mock<EncryptData>()

    @BeforeAll
    fun setUp() {
        underTest = CameraUploadsRecordEntityMapper(
            encryptData = encryptData,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            encryptData,
        )
    }

    @Test
    fun `test that error is thrown when encrypt mediaId is null`() = runTest {
        val record = mock<CameraUploadsRecord> {
            on { mediaId }.thenReturn(1234L)
        }
        whenever(encryptData.invoke(record.mediaId.toString())).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            underTest(record)
        }
    }

    @Test
    fun `test that error is thrown when encrypt timestamp is null`() = runTest {
        val record = mock<CameraUploadsRecord> {
            on { timestamp }.thenReturn(1234L)
        }
        whenever(encryptData.invoke(record.timestamp.toString())).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            underTest(record)
        }
    }

    @Test
    fun `test that mapper returns entity correctly when invoke function`() = runTest {
        val record = CameraUploadsRecord(
            mediaId = 1234L,
            fileName = "fileName.jpg",
            filePath = "filePath",
            timestamp = 56789L,
            folderType = CameraUploadFolderType.Primary,
            type = CameraUploadsRecordType.TYPE_PHOTO,
            uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
            originalFingerprint = "originalFingerprint",
            generatedFingerprint = null,
            tempFilePath = "tempFilePath"
        )

        val expected = CameraUploadsRecordEntity(
            encryptedMediaId = "mediaId",
            encryptedFileName = "fileName.jpg",
            encryptedFilePath = "filePath",
            encryptedTimestamp = "timestamp",
            folderType = CameraUploadFolderType.Primary,
            fileType = CameraUploadsRecordType.TYPE_PHOTO,
            uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
            encryptedOriginalFingerprint = "originalFingerprint",
            encryptedGeneratedFingerprint = null,
            encryptedTempFilePath = "tempFilePath"
        )

        with(record) {
            whenever(encryptData.invoke(mediaId.toString())).thenReturn(expected.encryptedMediaId)
            whenever(encryptData.invoke(fileName)).thenReturn(expected.encryptedFileName)
            whenever(encryptData.invoke(filePath)).thenReturn(expected.encryptedFilePath)
            whenever(encryptData.invoke(timestamp.toString())).thenReturn(expected.encryptedTimestamp)
            whenever(encryptData.invoke(originalFingerprint)).thenReturn(expected.encryptedOriginalFingerprint)
            whenever(encryptData.invoke(generatedFingerprint.toString())).thenReturn(expected.encryptedGeneratedFingerprint)
            whenever(encryptData.invoke(tempFilePath)).thenReturn(expected.encryptedTempFilePath)
        }
        val actual = underTest(record)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that when the coroutine is cancelled mapper returns cancellation exception`() =
        runTest {
            val testScope = TestScope()
            testScope.launch {
                cancel()
                assertThrows<CancellationException> {
                    underTest(mock())
                }
            }
        }
}
