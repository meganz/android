package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [GetCameraUploadFolderHandlesUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCameraUploadFolderHandlesUseCaseTest {

    private lateinit var underTest: GetCameraUploadFolderHandlesUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetCameraUploadFolderHandlesUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
        whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(INVALID_HANDLE)
    }

    @Test
    fun `test that the local sync handles are returned when available without falling back to the server`() =
        runTest {
            whenever(cameraUploadsRepository.getPrimarySyncHandle()).thenReturn(PRIMARY_HANDLE)
            whenever(cameraUploadsRepository.getSecondarySyncHandle()).thenReturn(SECONDARY_HANDLE)

            assertThat(underTest())
                .containsExactly(NodeId(PRIMARY_HANDLE), NodeId(SECONDARY_HANDLE)).inOrder()
            verify(cameraUploadsRepository, never()).getCameraUploadsSyncHandles()
        }

    @Test
    fun `test that null and invalid local handles are dropped`() = runTest {
        whenever(cameraUploadsRepository.getPrimarySyncHandle()).thenReturn(PRIMARY_HANDLE)
        whenever(cameraUploadsRepository.getSecondarySyncHandle()).thenReturn(null)

        assertThat(underTest()).containsExactly(NodeId(PRIMARY_HANDLE))
    }

    @Test
    fun `test that the server sync handles are used when local handles are unavailable`() = runTest {
        whenever(cameraUploadsRepository.getPrimarySyncHandle()).thenReturn(null)
        whenever(cameraUploadsRepository.getSecondarySyncHandle()).thenReturn(INVALID_HANDLE)
        whenever(cameraUploadsRepository.getCameraUploadsSyncHandles())
            .thenReturn(PRIMARY_HANDLE to SECONDARY_HANDLE)

        assertThat(underTest())
            .containsExactly(NodeId(PRIMARY_HANDLE), NodeId(SECONDARY_HANDLE)).inOrder()
    }

    @Test
    fun `test that an empty list is returned when no handles are configured`() = runTest {
        whenever(cameraUploadsRepository.getPrimarySyncHandle()).thenReturn(null)
        whenever(cameraUploadsRepository.getSecondarySyncHandle()).thenReturn(null)
        whenever(cameraUploadsRepository.getCameraUploadsSyncHandles()).thenReturn(null)

        assertThat(underTest()).isEmpty()
    }

    private companion object {
        const val INVALID_HANDLE = -1L
        const val PRIMARY_HANDLE = 100L
        const val SECONDARY_HANDLE = 200L
    }
}
