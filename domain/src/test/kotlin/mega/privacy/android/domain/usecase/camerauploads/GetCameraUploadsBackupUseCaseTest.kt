package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCameraUploadsBackupUseCaseTest {

    private lateinit var underTest: GetCameraUploadsBackupUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetCameraUploadsBackupUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @Test
    internal fun `test that camera uploads backup is returned when invoked`() =
        runTest {
            val expected = mock<Backup>()
            whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(expected)
            val actual = underTest()
            assertThat(actual).isEqualTo(expected)
        }
}