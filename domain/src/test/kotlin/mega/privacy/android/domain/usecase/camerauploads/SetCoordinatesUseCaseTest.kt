package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SetCoordinatesUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetCoordinatesUseCaseTest {

    private lateinit var underTest: SetCoordinatesUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetCoordinatesUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that coordinates are set when invoked`() =
        runTest {
            val nodeId = NodeId(1L)
            val latitude = 0.0
            val longitude = 0.0
            underTest(nodeId, latitude, longitude)
            verify(cameraUploadRepository).setCoordinates(nodeId, latitude, longitude)
        }
}
