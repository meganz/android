package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [GetMediaUploadBackupIDUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMediaUploadBackupIDUseCaseTest {

    private lateinit var underTest: GetMediaUploadBackupIDUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetMediaUploadBackupIDUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @Test
    internal fun `test that media upload backup id is returned when invoked`() =
        runTest {
            val expected = 1L
            whenever(cameraUploadsRepository.getMuBackUpId()).thenReturn(expected)
            val actual = underTest()
            assertThat(actual).isEqualTo(expected)
        }
}
