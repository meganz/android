package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [GetCameraUploadBackupIDUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCameraUploadBackupIDUseCaseTest {

    private lateinit var underTest: GetCameraUploadBackupIDUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetCameraUploadBackupIDUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    internal fun `test that camera upload backup id is returned when invoked`() =
        runTest {
            val expected = 1L
            whenever(cameraUploadRepository.getCuBackUpId()).thenReturn(expected)
            val actual = underTest()
            assertThat(actual).isEqualTo(expected)
        }
}
