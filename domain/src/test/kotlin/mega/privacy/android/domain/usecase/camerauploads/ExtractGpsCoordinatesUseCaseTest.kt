package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtractGpsCoordinatesUseCaseTest {
    private lateinit var underTest: ExtractGpsCoordinatesUseCase

    private val getGPSCoordinatesUseCase = mock<GetGPSCoordinatesUseCase>()

    private val record = CameraUploadsRecord(
        mediaId = 1L,
        fileName = "fileName",
        filePath = "filePath",
        timestamp = 0L,
        folderType = CameraUploadFolderType.Primary,
        type = CameraUploadsRecordType.TYPE_VIDEO,
        uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
        originalFingerprint = "originalFingerprint",
        generatedFingerprint = "generatedFingerprint",
        tempFilePath = "tempFilePath",
    )

    @BeforeAll
    fun setUp() {
        underTest = ExtractGpsCoordinatesUseCase(
            getGPSCoordinatesUseCase = getGPSCoordinatesUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getGPSCoordinatesUseCase,
        )
    }

    @ParameterizedTest(name = "when existence of a Node is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the gps coordinates are set to the record when the record has an existingNodeId`(
        hasExistingNodeId: Boolean
    ) = runTest {
        val expected = Pair(0.0, 0.0)
        val record1 = record.copy(
            existingNodeId = if (hasExistingNodeId) mock() else null
        )
        val list = listOf(record1)
        whenever(
            getGPSCoordinatesUseCase(
                record.filePath,
                record.type == CameraUploadsRecordType.TYPE_VIDEO
            )
        ).thenReturn(expected)

        if (hasExistingNodeId) {
            verifyNoInteractions(getGPSCoordinatesUseCase)
        } else {
            assertThat(underTest(list)[0].latitude).isEqualTo(expected.first)
            assertThat(underTest(list)[0].longitude).isEqualTo(expected.second)
        }
    }

    @Test
    fun `test that the size of the result returned is equals to the size of the list entered in parameter`() =
        runTest {
            val expected = 5
            val list = List(expected) { index ->
                record.copy(
                    mediaId = index.toLong(),
                    filePath = "filePath/$index",
                    existingNodeId = null,
                )
            }
            for (i in 0..<expected) {
                whenever(
                    getGPSCoordinatesUseCase(
                        list[i].filePath,
                        list[i].type == CameraUploadsRecordType.TYPE_VIDEO,
                    )
                ).thenReturn(Pair(0.0, 0.0))
            }
            assertThat(underTest(list).size).isEqualTo(expected)
        }

    @Test
    fun `test that an error thrown when retrieving the coordinates is caught silently`() =
        runTest {
            val expected = 5
            val list = List(expected) { index ->
                record.copy(
                    mediaId = index.toLong(),
                    filePath = "filePath/$index",
                    existingNodeId = null,
                )
            }
            for (i in 0..<expected) {
                val stub = whenever(
                    getGPSCoordinatesUseCase(
                        list[i].filePath,
                        list[i].type == CameraUploadsRecordType.TYPE_VIDEO,
                    )
                )
                if (i == 0)
                    stub.thenThrow(RuntimeException::class.java)
                else
                    stub.thenReturn(mock())
            }
            assertThat(underTest(list).size).isEqualTo(expected)
        }
}
