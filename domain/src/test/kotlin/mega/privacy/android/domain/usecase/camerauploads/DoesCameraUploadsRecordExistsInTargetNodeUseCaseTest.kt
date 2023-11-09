package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesCameraUploadsRecordExistsInTargetNodeUseCaseTest {
    private lateinit var underTest: DoesCameraUploadsRecordExistsInTargetNodeUseCase

    private val findNodeWithFingerprintInParentNodeUseCase =
        mock<FindNodeWithFingerprintInParentNodeUseCase>()

    private val primaryUploadNodeId = mock<NodeId>()
    private val secondaryUploadNodeId = mock<NodeId>()

    private val record = CameraUploadsRecord(
        mediaId = 1L,
        fileName = "fileName",
        filePath = "filePath",
        timestamp = 0L,
        folderType = CameraUploadFolderType.Primary,
        type = SyncRecordType.TYPE_VIDEO,
        uploadStatus = CameraUploadsRecordUploadStatus.PENDING,
        originalFingerprint = "originalFingerprint",
        generatedFingerprint = "generatedFingerprint",
        tempFilePath = "tempFilePath",
    )

    @BeforeAll
    fun setUp() {
        underTest = DoesCameraUploadsRecordExistsInTargetNodeUseCase(
            findNodeWithFingerprintInParentNodeUseCase = findNodeWithFingerprintInParentNodeUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            findNodeWithFingerprintInParentNodeUseCase,
        )
    }

    @ParameterizedTest
    @EnumSource(CameraUploadFolderType::class)
    fun `test that findNodeWithFingerprintInParentNodeUseCase is invoked with correct target node `(
        type: CameraUploadFolderType,
    ) =
        runTest {
            val record = record.copy(folderType = type)
            underTest(listOf(record), primaryUploadNodeId, secondaryUploadNodeId)
            verify(findNodeWithFingerprintInParentNodeUseCase).invoke(
                record.originalFingerprint,
                record.generatedFingerprint,
                when (type) {
                    CameraUploadFolderType.Primary -> primaryUploadNodeId
                    CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                }
            )

        }

    @ParameterizedTest(name = "when folder type is {0}")
    @EnumSource(CameraUploadFolderType::class)
    fun `test that the existingParentNodeId and existsInTargetNode is set to the record when retrieved`(
        type: CameraUploadFolderType,
    ) =
        runTest {
            val expected = Pair(false, mock<NodeId>())
            val record = record.copy(folderType = type)
            val list = listOf(record)
            whenever(
                findNodeWithFingerprintInParentNodeUseCase(
                    record.originalFingerprint,
                    record.generatedFingerprint,
                    when (type) {
                        CameraUploadFolderType.Primary -> primaryUploadNodeId
                        CameraUploadFolderType.Secondary -> secondaryUploadNodeId
                    },
                )
            ).thenReturn(expected)

            assertThat(
                underTest(list, primaryUploadNodeId, secondaryUploadNodeId)[0].existsInTargetNode
            ).isEqualTo(expected.first)
            assertThat(
                underTest(list, primaryUploadNodeId, secondaryUploadNodeId)[0].existingNodeId
            ).isEqualTo(expected.second)
        }

    @Test
    fun `test that the size of the result returned is equals to the size of the list if no error is caught`() =
        runTest {
            val expected = 5
            val list = List(expected) { index ->
                record.copy(
                    mediaId = index.toLong(),
                    filePath = "filePath/$index"
                )
            }
            for (i in 0..<expected) {
                whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        list[i].originalFingerprint,
                        list[i].generatedFingerprint,
                        primaryUploadNodeId
                    )
                ).thenReturn(mock())
            }
            assertThat(underTest(list, primaryUploadNodeId, secondaryUploadNodeId).size)
                .isEqualTo(expected)
        }

    @Test
    fun `test that an error thrown when retrieving the target node is caught silently and record is filtered out from the result`() =
        runTest {
            val size = 5
            val list = List(size) { index ->
                record.copy(
                    mediaId = index.toLong(),
                    originalFingerprint = "originalFingerprint$index",
                    generatedFingerprint = "generatedFingerprint$index",
                    filePath = "filePath/$index",
                )
            }
            for (i in 0..<size) {
                val stub = whenever(
                    findNodeWithFingerprintInParentNodeUseCase(
                        list[i].originalFingerprint,
                        list[i].generatedFingerprint,
                        primaryUploadNodeId
                    )
                )
                if (i == 0)
                    stub.thenThrow(RuntimeException::class.java)
                else
                    stub.thenReturn(Pair(false, null))
            }
            val result = underTest(list, primaryUploadNodeId, secondaryUploadNodeId)
            assertThat(result.size).isEqualTo(size - 1)
        }
}
