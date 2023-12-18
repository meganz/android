package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
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
class CameraUploadsRecordModelMapperTest {
    private lateinit var underTest: CameraUploadsRecordModelMapper

    private val decryptData = mock<DecryptData>()

    @BeforeAll
    fun setUp() {
        underTest = CameraUploadsRecordModelMapper(
            decryptData = decryptData,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            decryptData,
        )
    }

    @Test
    fun `test that error is thrown when decrypt mediaId is null`() = runTest {
        val entity = mock<CameraUploadsRecordEntity> {
            on { encryptedMediaId }.thenReturn("1234")
        }
        whenever(decryptData.invoke(entity.encryptedMediaId)).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            underTest(entity)
        }
    }

    @Test
    fun `test that error is thrown when decrypt timestamp is null`() = runTest {
        val entity = mock<CameraUploadsRecordEntity> {
            on { encryptedTimestamp }.thenReturn("1234")
        }
        whenever(decryptData.invoke(entity.encryptedTimestamp)).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            underTest(entity)
        }
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val entity = CameraUploadsRecordEntity(
            encryptedMediaId = "mediaId",
            encryptedFileName = "fileName.jpg",
            encryptedFilePath = "filePath",
            encryptedTimestamp = "timestamp",
            folderType = CameraUploadFolderType.Primary,
            fileType = CameraUploadsRecordType.TYPE_PHOTO,
            uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
            encryptedOriginalFingerprint = "originalFingerprint",
            encryptedGeneratedFingerprint = "null",
            encryptedTempFilePath = "tempFilePath"
        )

        val expected = CameraUploadsRecord(
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

        with(entity) {
            whenever(decryptData.invoke(encryptedMediaId)).thenReturn(expected.mediaId.toString())
            whenever(decryptData.invoke(encryptedFileName)).thenReturn(expected.fileName)
            whenever(decryptData.invoke(encryptedFilePath)).thenReturn(expected.filePath)
            whenever(decryptData.invoke(encryptedTimestamp)).thenReturn(expected.timestamp.toString())
            whenever(decryptData.invoke(encryptedOriginalFingerprint)).thenReturn(expected.originalFingerprint)
            whenever(decryptData.invoke(encryptedGeneratedFingerprint)).thenReturn(expected.generatedFingerprint)
            whenever(decryptData.invoke(encryptedTempFilePath)).thenReturn(expected.tempFilePath)
        }
        val actual = underTest(entity)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapper returns default values correctly when invoke function`() = runTest {
        val entity = CameraUploadsRecordEntity(
            encryptedMediaId = "mediaId",
            encryptedFileName = "fileName.jpg",
            encryptedFilePath = "filePath",
            encryptedTimestamp = "timestamp",
            folderType = CameraUploadFolderType.Primary,
            fileType = CameraUploadsRecordType.TYPE_PHOTO,
            uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
            encryptedOriginalFingerprint = "originalFingerprint",
            encryptedGeneratedFingerprint = "null",
            encryptedTempFilePath = "tempFilePath"
        )

        val expected = CameraUploadsRecord(
            mediaId = 1234L,
            fileName = "",
            filePath = "",
            timestamp = 56789L,
            folderType = CameraUploadFolderType.Primary,
            type = CameraUploadsRecordType.TYPE_PHOTO,
            uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
            originalFingerprint = "",
            generatedFingerprint = null,
            tempFilePath = ""
        )

        with(entity) {
            whenever(decryptData.invoke(encryptedMediaId)).thenReturn(expected.mediaId.toString())
            whenever(decryptData.invoke(encryptedFileName)).thenReturn(null)
            whenever(decryptData.invoke(encryptedFilePath)).thenReturn(null)
            whenever(decryptData.invoke(encryptedTimestamp)).thenReturn(expected.timestamp.toString())
            whenever(decryptData.invoke(encryptedOriginalFingerprint)).thenReturn(null)
            whenever(decryptData.invoke(encryptedGeneratedFingerprint)).thenReturn(null)
            whenever(decryptData.invoke(encryptedTempFilePath)).thenReturn(null)
        }
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
