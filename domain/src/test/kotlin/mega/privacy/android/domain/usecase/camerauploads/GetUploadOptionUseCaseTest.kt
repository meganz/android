package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetUploadOptionUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetUploadOptionUseCaseTest {

    private lateinit var underTest: GetUploadOptionUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetUploadOptionUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "upload option: {0}")
    @EnumSource(UploadOption::class)
    fun `test that the upload option is retrieved`(uploadOption: UploadOption) = runTest {
        whenever(cameraUploadRepository.getUploadOption()).thenReturn(uploadOption)

        assertThat(underTest()).isEqualTo(uploadOption)
    }

    @Test
    fun `test that the upload option defaults to photos and videos if no existing upload option was set`() =
        runTest {
            whenever(cameraUploadRepository.getUploadOption()).thenReturn(null)

            assertThat(underTest()).isEqualTo(UploadOption.PHOTOS_AND_VIDEOS)
        }
}