package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SetCameraUploadsByWifiUseCase]
 */
@ExperimentalCoroutinesApi
class SetCameraUploadsByWifiUseCaseTest {

    private lateinit var underTest: SetCameraUploadsByWifiUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = SetCameraUploadsByWifiUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that camera uploads can only run when connected to wifi`() = runTest {
        underTest(true)

        verify(cameraUploadRepository).setCameraUploadsByWifi(true)
    }

    @Test
    fun `test that camera uploads can run when connected to either wifi or mobile data`() =
        runTest {
            underTest(false)

            verify(cameraUploadRepository).setCameraUploadsByWifi(false)
        }
}