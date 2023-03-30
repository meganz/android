package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [IsCameraUploadsByWifiUseCase]
 */
@ExperimentalCoroutinesApi
class IsCameraUploadsByWifiUseCaseTest {

    private lateinit var underTest: IsCameraUploadsByWifiUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = IsCameraUploadsByWifiUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that is camera uploads by wifi is invoked`() = runTest {
        underTest()

        verify(cameraUploadRepository).isCameraUploadsByWifi()
    }
}