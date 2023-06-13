package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDisableCameraUploadSettingsTest {
    private lateinit var underTest: DisableCameraUploadSettings

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @Before
    fun setUp() {
        underTest = DefaultDisableCameraUploadSettings(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @Test
    fun `test that camera upload settings are updated when the use case is invoked`() = runTest {
        underTest()

        verify(cameraUploadRepository, times(1)).setCameraUploadsEnabled(false)
        verify(cameraUploadRepository, times(1)).setSecondaryEnabled(false)
    }
}
