package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsCameraUploadsNodeValidUseCaseTest {
    private lateinit var underTest: IsCameraUploadsNodeValidUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsCameraUploadsNodeValidUseCase(
            cameraUploadRepository = cameraUploadRepository,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            isNodeInRubbishOrDeletedUseCase,
        )
    }

    @Test
    fun `test that true is returned when the node id is invalid and the node is not in rubbish bin or deleted`() =
        runTest {
            val nodeId = NodeId(123456)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(isNodeInRubbishOrDeletedUseCase(nodeId.longValue)).thenReturn(false)

            val result = underTest(nodeId)

            assertThat(result).isTrue()
        }

    @Test
    fun `test that false is returned when the node id is invalid`() =
        runTest {
            val nodeId = NodeId(123456)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(nodeId.longValue)

            val result = underTest(nodeId)

            assertThat(result).isFalse()
        }

    @Test
    fun `est that false is returned when the node is in rubbish bin or deleted`() =
        runTest {
            val nodeId = NodeId(123456)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(-1L)
            whenever(isNodeInRubbishOrDeletedUseCase(nodeId.longValue)).thenReturn(true)

            val result = underTest(nodeId)

            assertThat(result).isFalse()
        }
}
